package com.luckyprospector.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luckyprospector.app.capture.CaptureDraft
import com.luckyprospector.app.data.local.FindingPhotoStore
import com.luckyprospector.app.domain.Finding
import com.luckyprospector.app.domain.FindingRepository
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.GeoPoint
import com.luckyprospector.app.domain.SharedSightingRepository
import com.luckyprospector.app.domain.SyncState
import com.luckyprospector.app.inference.Detector
import com.luckyprospector.app.sharing.SharePreparation
import com.luckyprospector.app.sharing.SharedPhotoPreparer
import android.net.Uri
import java.io.File
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Raised when the user's own classification is kept private because the classifier disagrees. */
class SharedTypeNotConfirmedException : IllegalStateException("Classifier does not confirm the new type")

/**
 * Local findings and their publication to the shared map.
 *
 * Every write runs in [viewModelScope] and is merely awaited by the screen. A
 * publication that belonged to the detail screen's scope used to be cancelled
 * when the user left the screen, stranding the finding in PENDING.
 *
 * What reaches the shared map is decided by the on-device classifier, not by the
 * user's label: see [SharePhotoPolicy][com.luckyprospector.app.domain.SharePhotoPolicy].
 */
class FindingsViewModel(
    private val repository: FindingRepository,
    private val photoStore: FindingPhotoStore,
    private val sharedRepository: SharedSightingRepository,
    private val photoPreparer: SharedPhotoPreparer,
    /** The real model, or null while loading or when only the demo detector exists. */
    private val detector: suspend () -> Detector?,
    uid: Flow<String?>,
) : ViewModel() {

    /** Only touched from viewModelScope, which runs on the main thread. */
    private val inFlight = hashSetOf<String>()

    val findings: StateFlow<List<Finding>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // A PENDING finding is one the user asked to share whose publication never
        // finished (process killed, network lost). Publishing is idempotent, so
        // finishing it once signed in again is safe and honours the original choice.
        // The photo choice is not persisted, so a resumed publication goes out
        // without a photo: the conservative reading of an interrupted consent.
        viewModelScope.launch {
            combine(uid, repository.observeAll()) { account, all ->
                if (account == null) emptySet() else all.filter { it.syncState == SyncState.PENDING }.mapTo(hashSetOf()) { it.id }
            }
                .distinctUntilChanged()
                .collect { pendingIds ->
                    pendingIds.forEach { id ->
                        launch {
                            runCatching {
                                publishOnce(id) {
                                    val pending = current(id)?.takeIf { it.syncState == SyncState.PENDING } ?: return@publishOnce null
                                    when (val prepared = prepare(pending)) {
                                        is SharePreparation.Ready -> pending.copy(type = prepared.type) to null
                                        else -> {
                                            repository.save(pending.copy(syncState = SyncState.FAILED))
                                            null
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    suspend fun saveCapture(draft: CaptureDraft): Result<Unit> = inViewModel {
        val finding = Finding(
            type = draft.type,
            privateImageUri = draft.sourcePhoto.toString(),
            galleryImageUri = null,
            location = draft.location,
            wish = draft.wish,
            prediction = draft.prediction,
        )
        try {
            val stored = photoStore.importJpeg(draft.sourcePhoto, finding.id)
            repository.save(finding.copy(privateImageUri = stored.privateImageUri, galleryImageUri = stored.galleryImageUri))
        } finally {
            draft.sourcePhoto.path?.let(::File)?.delete()
        }
    }

    /**
     * A finding added by hand from a photo already in the gallery: no camera, no
     * model prediction, the date and place the user chose. The shared map still
     * applies its own classifier check before anything could leave the phone.
     */
    suspend fun addManual(photo: Uri, type: FindingType, wish: String?, date: Instant, location: GeoPoint?): Result<Unit> = inViewModel {
        val finding = Finding(
            createdAt = date,
            type = type,
            privateImageUri = photo.toString(),
            galleryImageUri = null,
            location = location,
            wish = wish,
            prediction = null,
        )
        val stored = photoStore.importPicked(photo, finding.id)
        try {
            repository.save(finding.copy(privateImageUri = stored.privateImageUri))
        } catch (failure: Exception) {
            photoStore.delete(stored.privateImageUri, null)
            throw failure
        }
    }

    suspend fun photoLocation(photo: Uri): GeoPoint? = photoStore.locationOf(photo)

    /**
     * Saves locally in every case. A shared pin follows a type change only when the
     * classifier agrees; otherwise the change stays private and the caller is told.
     */
    suspend fun save(original: Finding, updated: Finding): Result<Unit> = inViewModel {
        repository.save(updated)
        if (original.syncState == SyncState.SYNCED && updated.type != original.type) {
            val prepared = prepare(updated)
            if (prepared !is SharePreparation.Ready || prepared.type != updated.type) {
                throw SharedTypeNotConfirmedException()
            }
            sharedRepository.updateType(updated.id, updated.type)
        }
    }

    suspend fun delete(finding: Finding): Result<Unit> = inViewModel {
        if (finding.syncState == SyncState.SYNCED) {
            // Do not leave a public pin behind when the local record is deleted.
            // Failure keeps the local record so withdrawal can be retried after
            // connectivity or authentication returns.
            sharedRepository.withdraw(finding.id)
        }
        photoStore.delete(finding.privateImageUri, finding.galleryImageUri)
        repository.delete(finding.id)
    }

    /**
     * Runs the classifier on the finding's photo and builds the crop that would be
     * shared. Nothing leaves the phone here; the screen shows the result first.
     */
    suspend fun prepareShare(finding: Finding): SharePreparation =
        viewModelScope.async { prepare(finding) }.await()

    /**
     * Publishes with the type the classifier allowed in [prepared], with its photo
     * only when [includePhoto] is true.
     */
    suspend fun share(finding: Finding, prepared: SharePreparation.Ready, includePhoto: Boolean): Result<Unit> = inViewModel {
        publishOnce(finding.id) {
            repository.save(finding.copy(syncState = SyncState.PENDING))
            finding.copy(type = prepared.type) to prepared.jpeg.takeIf { includePhoto }
        }
    }

    /**
     * Before the account is deleted: withdraws every pin it published and every vote
     * it gave, and marks all local findings private again. The findings themselves
     * stay on the phone.
     */
    suspend fun leaveSharedMap(): Result<Unit> = inViewModel {
        sharedRepository.withdrawAll()
        sharedRepository.withdrawVotes()
        repository.observeAll().first()
            .filter { it.syncState != SyncState.LOCAL_ONLY }
            .forEach { repository.save(it.copy(syncState = SyncState.LOCAL_ONLY)) }
    }

    suspend fun unshare(finding: Finding): Result<Unit> = inViewModel {
        sharedRepository.withdraw(finding.id)
        repository.save(finding.copy(syncState = SyncState.LOCAL_ONLY))
    }

    private suspend fun prepare(finding: Finding): SharePreparation =
        photoPreparer.prepare(finding.privateImageUri, finding.type, detector())

    /**
     * Runs at most one publication per finding. Two concurrent attempts would both
     * find no claim and both write; the second would be refused by the rules and
     * mark FAILED a finding the first had just published.
     */
    private suspend fun publishOnce(id: String, prepare: suspend () -> Pair<Finding, ByteArray?>?) {
        if (!inFlight.add(id)) return
        try {
            prepare()?.let { (finding, photo) -> publish(finding, photo) }
        } finally {
            inFlight.remove(id)
        }
    }

    /** [finding] carries the shared type; the local record keeps the user's own. */
    private suspend fun publish(finding: Finding, photo: ByteArray?) {
        try {
            sharedRepository.publish(finding, photo)
        } catch (failure: Exception) {
            current(finding.id)?.let { repository.save(it.copy(syncState = SyncState.FAILED)) }
            throw failure
        }
        current(finding.id)?.let { repository.save(it.copy(syncState = SyncState.SYNCED)) }
    }

    /** Re-read before writing the state, so an edit made meanwhile is not overwritten. */
    private suspend fun current(id: String): Finding? = repository.get(id)

    private suspend fun inViewModel(block: suspend () -> Unit): Result<Unit> =
        viewModelScope.async { runCatching { block() } }.await()
}

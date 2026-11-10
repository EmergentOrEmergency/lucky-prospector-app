package com.luckyprospector.app.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.WriteBatch
import com.luckyprospector.app.domain.Finding
import com.luckyprospector.app.domain.FindingHasNoLocationException
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.GeoPoint
import com.luckyprospector.app.domain.PublishLimitReachedException
import com.luckyprospector.app.domain.SharedSighting
import com.luckyprospector.app.domain.SharedSightingRepository
import com.luckyprospector.app.domain.SharingRequiresSignInException
import com.luckyprospector.app.domain.SightingReportReason
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Firestore writer for the anonymous shared map.
 *
 * Ownership and confirmation claims deliberately live below the signed-in user's
 * private subtree. Each claim is committed in the same batch as the corresponding
 * public mutation because the security rules validate the post-batch state with
 * `existsAfter`.
 */
class FirestoreSharedSightingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : SharedSightingRepository {

    override fun observeSharedSightings(areaPrefixes: Set<String>): Flow<List<SharedSighting>> {
        if (areaPrefixes.isEmpty()) return flowOf(emptyList())
        val perCell = areaPrefixes.map(::observeCell)
        return combine(perCell) { cells ->
            // Prefixes never overlap, but deduplicating keeps the result correct even if
            // a caller passes nested ones.
            cells.asSequence().flatten().distinctBy(SharedSighting::id).toList()
        }
    }

    private fun observeCell(prefix: String): Flow<List<SharedSighting>> = callbackFlow {
        requireUid()
        val registration = firestore.collection(SHARED_SIGHTINGS)
            .whereGreaterThanOrEqualTo(GEOHASH, prefix)
            .whereLessThanOrEqualTo(GEOHASH, prefix + PREFIX_END)
            // The rules refuse to serve removed pins, and Firestore rejects a query that
            // could return a document the rules would refuse, so this filter is required.
            .whereEqualTo(HIDDEN, false)
            .limit(MAX_PINS_PER_CELL)
            .addSnapshotListener { snapshot, failure ->
                if (failure != null) {
                    close(failure)
                    return@addSnapshotListener
                }
                val sightings = snapshot?.documents.orEmpty().mapNotNull { document ->
                    document.data?.toSharedSighting(document.id)
                }
                trySend(sightings)
            }
        awaitClose { registration.remove() }
    }

    override fun observeOwnedSightingIds(): Flow<Set<String>> =
        observePrivateDocumentIds(SHARED_CLAIMS)

    override fun observeConfirmedSightingIds(): Flow<Set<String>> =
        observePrivateDocumentIds(CONFIRMATIONS)

    override fun observeReportedSightingIds(): Flow<Set<String>> =
        observePrivateDocumentIds(REPORT_RECEIPTS)

    override fun observeGoneSightingIds(): Flow<Set<String>> =
        observePrivateDocumentIds(GONE_RECEIPTS)

    override suspend fun publish(finding: Finding, photoJpeg: ByteArray?): SharedSighting {
        val uid = requireUid()
        val sighting = SharedSighting.from(id = finding.id, finding = finding, hasPhoto = photoJpeg != null)
            ?: throw FindingHasNoLocationException()
        val photoRef = firestore.collection(SHARED_PHOTOS).document(sighting.id)
        val publicRef = firestore.collection(SHARED_SIGHTINGS).document(sighting.id)
        val claimRef = firestore.collection(USERS).document(uid)
            .collection(SHARED_CLAIMS).document(sighting.id)

        // A batch that committed on the server but whose response was lost leaves the
        // finding marked FAILED or PENDING locally. A second create would be refused,
        // so recognise the earlier success instead. A claim without its pin is left
        // over from a pin withdrawn elsewhere; publishing again replaces it.
        if (claimRef.get().await().exists() && publicPinExists(publicRef)) return sighting

        val quotaRef = firestore.collection(USERS).document(uid)
            .collection(LIMITS).document(PUBLISH_QUOTA)
        val quotaWrite = nextQuotaWrite(quotaRef.get().await().data, Instant.now())
            ?: throw PublishLimitReachedException()

        val publicDocument = sighting.toFirestoreDocument().toMutableMap().apply {
            // The rules require the authoritative server time so a modified client
            // cannot keep a pin alive by publishing it with a future timestamp.
            this[SHARED_AT] = FieldValue.serverTimestamp()
        }

        firestore.batch()
            .set(publicRef, publicDocument)
            .set(claimRef, mapOf(CREATED_AT to FieldValue.serverTimestamp()))
            .apply {
                if (quotaWrite.startsNewWindow) set(quotaRef, quotaWrite.fields) else update(quotaRef, quotaWrite.fields)
                // The photo is written in the same batch so a pin never promises a
                // photo that does not exist, and the rules can tie the two together.
                if (photoJpeg != null) set(photoRef, mapOf(PHOTO_JPEG to Blob.fromBytes(photoJpeg)))
            }
            .commit()
            .await()

        return sighting
    }

    /** A removed pin is unreadable, which still proves that it exists. */
    private suspend fun publicPinExists(publicRef: DocumentReference): Boolean = try {
        publicRef.get().await().exists()
    } catch (denied: FirebaseFirestoreException) {
        if (denied.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) true else throw denied
    }

    override suspend fun loadPhoto(sightingId: String): ByteArray? {
        requireUid()
        return try {
            firestore.collection(SHARED_PHOTOS).document(sightingId).get().await()
                .getBlob(PHOTO_JPEG)?.toBytes()
        } catch (denied: FirebaseFirestoreException) {
            // A hidden pin's photo is refused by the rules; show the pin without it.
            if (denied.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) null else throw denied
        }
    }

    override suspend fun updateType(sightingId: String, type: FindingType) {
        requireUid()
        firestore.collection(SHARED_SIGHTINGS).document(sightingId)
            .update(TYPE, type.name)
            .await()
    }

    override suspend fun confirm(sighting: SharedSighting) {
        val uid = requireUid()
        val publicRef = firestore.collection(SHARED_SIGHTINGS).document(sighting.id)
        val claimRef = firestore.collection(USERS).document(uid)
            .collection(CONFIRMATIONS).document(sighting.id)
        firestore.batch()
            .update(
                publicRef,
                mapOf(
                    CONFIRMATION_COUNT to FieldValue.increment(1),
                    LAST_CONFIRMED_AT to FieldValue.serverTimestamp(),
                ),
            )
            .set(claimRef, mapOf(CONFIRMED_AT to FieldValue.serverTimestamp()))
            .commit()
            .await()
    }

    override suspend fun report(sighting: SharedSighting, reason: SightingReportReason) {
        val uid = requireUid()
        val publicRef = firestore.collection(SHARED_SIGHTINGS).document(sighting.id)
        val receiptRef = firestore.collection(USERS).document(uid)
            .collection(REPORT_RECEIPTS).document(sighting.id)
        // The rules recompute `hidden` from the incremented counters and refuse a
        // mismatch, so it must be derived from the counts the batch will produce.
        val hidden = sighting.copy(reportCount = sighting.reportCount + 1).isHidden

        firestore.batch()
            .update(publicRef, mapOf(REPORT_COUNT to FieldValue.increment(1)) + hiddenFields(sighting, hidden))
            .set(
                receiptRef,
                mapOf(
                    REPORT_REASON to reason.name,
                    REPORTED_AT to FieldValue.serverTimestamp(),
                ),
            )
            .also { schedulePhotoPurge(it, sighting, hidden) }
            .commit()
            .await()
    }

    override suspend fun markGone(sighting: SharedSighting) {
        val uid = requireUid()
        val publicRef = firestore.collection(SHARED_SIGHTINGS).document(sighting.id)
        val receiptRef = firestore.collection(USERS).document(uid)
            .collection(GONE_RECEIPTS).document(sighting.id)
        val hidden = sighting.copy(goneCount = sighting.goneCount + 1).isHidden

        firestore.batch()
            .update(publicRef, mapOf(GONE_COUNT to FieldValue.increment(1)) + hiddenFields(sighting, hidden))
            .set(receiptRef, mapOf(REPORTED_AT to FieldValue.serverTimestamp()))
            .also { schedulePhotoPurge(it, sighting, hidden) }
            .commit()
            .await()
    }

    /**
     * The pin's `hidden` flag, plus the date the pin is erased when this very write
     * hides it: a Firestore TTL policy on `purgeAt` deletes hidden pins and their
     * photos 90 days later, as the privacy policy promises.
     */
    private fun hiddenFields(sighting: SharedSighting, hidden: Boolean): Map<String, Any> =
        if (hidden && !sighting.isHidden) mapOf(HIDDEN to true, PURGE_AT to purgeDate())
        else mapOf(HIDDEN to hidden)

    private fun schedulePhotoPurge(batch: WriteBatch, sighting: SharedSighting, hidden: Boolean) {
        if (hidden && !sighting.isHidden && sighting.hasPhoto) {
            batch.update(firestore.collection(SHARED_PHOTOS).document(sighting.id), PURGE_AT, purgeDate())
        }
    }

    private fun purgeDate() = Timestamp(Instant.now().plus(HIDDEN_RETENTION).epochSecond, 0)

    override suspend fun withdraw(sightingId: String) {
        val uid = requireUid()
        val publicRef = firestore.collection(SHARED_SIGHTINGS).document(sightingId)
        val claimRef = firestore.collection(USERS).document(uid)
            .collection(SHARED_CLAIMS).document(sightingId)
        val photoRef = firestore.collection(SHARED_PHOTOS).document(sightingId)

        firestore.batch()
            .delete(publicRef)
            .delete(photoRef)
            .delete(claimRef)
            .commit()
            .await()
    }

    override suspend fun withdrawAll(): Int {
        val uid = requireUid()
        val claims = firestore.collection(USERS).document(uid).collection(SHARED_CLAIMS).get().await()
        // Three deletes per pin; a batch holds at most 500 operations.
        claims.documents.map { it.id }.chunked(WITHDRAWALS_PER_BATCH).forEach { ids ->
            val batch = firestore.batch()
            ids.forEach { id ->
                batch.delete(firestore.collection(SHARED_SIGHTINGS).document(id))
                batch.delete(firestore.collection(SHARED_PHOTOS).document(id))
                batch.delete(firestore.collection(USERS).document(uid).collection(SHARED_CLAIMS).document(id))
            }
            batch.commit().await()
        }
        return claims.size()
    }

    override suspend fun withdrawVotes() {
        val uid = requireUid()
        val votes = listOf(
            CONFIRMATIONS to CONFIRMATION_COUNT,
            REPORT_RECEIPTS to REPORT_COUNT,
            GONE_RECEIPTS to GONE_COUNT,
        )
        votes.forEach { (collection, counterField) ->
            val receipts = firestore.collection(USERS).document(uid).collection(collection).get().await()
            receipts.documents.forEach { receipt ->
                val pinRef = firestore.collection(SHARED_SIGHTINGS).document(receipt.id)
                val batch = firestore.batch()
                if (publicPinExists(pinRef)) batch.update(pinRef, counterField, FieldValue.increment(-1))
                batch.delete(receipt.reference).commit().await()
            }
        }
    }

    private fun requireUid(): String = auth.currentUser?.uid ?: throw SharingRequiresSignInException()

    private fun observePrivateDocumentIds(collection: String): Flow<Set<String>> = callbackFlow {
        val uid = requireUid()
        val registration = firestore.collection(USERS).document(uid)
            .collection(collection)
            .addSnapshotListener { snapshot, failure ->
                if (failure != null) {
                    close(failure)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapTo(linkedSetOf()) { it.id })
            }
        awaitClose { registration.remove() }
    }

    companion object {
        internal const val USERS = "users"
        internal const val SHARED_SIGHTINGS = "shared_sightings"
        internal const val SHARED_CLAIMS = "shared_claims"
        internal const val SHARED_PHOTOS = "shared_photos"
        internal const val PHOTO_JPEG = "jpeg"
        internal const val HAS_PHOTO = "hasPhoto"
        internal const val CONFIRMATIONS = "confirmations"
        internal const val REPORT_RECEIPTS = "reports"
        internal const val GONE_RECEIPTS = "gone_reports"
        internal const val LIMITS = "limits"
        internal const val PUBLISH_QUOTA = "publish"
        internal const val CREATED_AT = "createdAt"
        internal const val CONFIRMED_AT = "confirmedAt"
        internal const val TYPE = "type"
        internal const val SHARED_AT = "sharedAt"
        internal const val GEOHASH = "geohash"
        internal const val GONE_COUNT = "goneCount"
        internal const val HIDDEN = "hidden"
        internal const val PURGE_AT = "purgeAt"
        internal const val CONFIRMATION_COUNT = "confirmations"
        internal const val LAST_CONFIRMED_AT = "lastConfirmedAt"
        internal const val REPORT_COUNT = "reportCount"
        internal const val REPORT_REASON = "reason"
        internal const val REPORTED_AT = "reportedAt"
        internal const val WINDOW_START = "windowStart"
        internal const val WINDOW_COUNT = "count"
        internal const val LAST_PUBLISHED_AT = "lastPublishedAt"

        /** Mirrored in firestore.rules. */
        internal const val MAX_PUBLICATIONS_PER_DAY = 20
        internal val QUOTA_WINDOW: Duration = Duration.ofDays(1)

        /** Hidden pins and photos are erased after this; mirrored in firestore.rules (89-91 days). */
        internal val HIDDEN_RETENTION: Duration = Duration.ofDays(90)

        /** Safety cap per geohash cell; the map shows a hint, not a census. */
        internal const val MAX_PINS_PER_CELL = 200L
        private const val WITHDRAWALS_PER_BATCH = 150
        private const val PREFIX_END = ""
    }
}

internal data class QuotaWrite(val startsNewWindow: Boolean, val fields: Map<String, Any>)

/**
 * The write to a private daily counter that must accompany a limited write (a new
 * pin, a feedback message), or null when the daily limit is used up.
 *
 * The rules judge the window by server time. A window is only treated as expired
 * here once it is a little past a day old by the device clock, so a slightly fast
 * clock does not start a window the server would still consider open.
 */
internal fun nextQuotaWrite(
    current: Map<String, Any?>?,
    now: Instant,
    maxPerDay: Int = FirestoreSharedSightingRepository.MAX_PUBLICATIONS_PER_DAY,
    /** The timestamp field the matching rule ties to the guarded write. */
    lastField: String = FirestoreSharedSightingRepository.LAST_PUBLISHED_AT,
): QuotaWrite? {
    val windowStart = (current?.get(FirestoreSharedSightingRepository.WINDOW_START) as? Timestamp)
        ?.toDate()?.toInstant()
    val count = (current?.get(FirestoreSharedSightingRepository.WINDOW_COUNT) as? Number)?.toInt() ?: 0
    val windowExpired = windowStart == null ||
        windowStart.plus(FirestoreSharedSightingRepository.QUOTA_WINDOW).plus(CLOCK_MARGIN) < now

    return when {
        windowExpired -> QuotaWrite(
            startsNewWindow = true,
            fields = mapOf(
                FirestoreSharedSightingRepository.WINDOW_START to FieldValue.serverTimestamp(),
                FirestoreSharedSightingRepository.WINDOW_COUNT to 1,
                lastField to FieldValue.serverTimestamp(),
            ),
        )
        count >= maxPerDay -> null
        else -> QuotaWrite(
            startsNewWindow = false,
            fields = mapOf(
                FirestoreSharedSightingRepository.WINDOW_COUNT to FieldValue.increment(1),
                lastField to FieldValue.serverTimestamp(),
            ),
        )
    }
}

private val CLOCK_MARGIN: Duration = Duration.ofMinutes(10)

private fun Instant.toTimestamp() = Timestamp(epochSecond, nano)

internal fun Map<String, Any?>.toSharedSighting(id: String): SharedSighting? = runCatching {
    val latitude = (this["latitude"] as? Number)?.toDouble() ?: return null
    val longitude = (this["longitude"] as? Number)?.toDouble() ?: return null
    val cellSizeMeters = (this["cellSizeMeters"] as? Number)?.toInt() ?: return null
    val type = (this["type"] as? String)?.let(FindingType::valueOf) ?: return null
    val sharedAt = (this["sharedAt"] as? Timestamp)?.toDate()?.toInstant() ?: return null
    val confirmations = (this["confirmations"] as? Number)?.toInt() ?: return null
    val lastConfirmedAt = (this["lastConfirmedAt"] as? Timestamp)?.toDate()?.toInstant()
    val reportCount = (this["reportCount"] as? Number)?.toInt() ?: 0
    val goneCount = (this[FirestoreSharedSightingRepository.GONE_COUNT] as? Number)?.toInt() ?: 0
    val hasPhoto = this[FirestoreSharedSightingRepository.HAS_PHOTO] as? Boolean ?: false

    SharedSighting(
        id = id,
        location = GeoPoint(latitude, longitude, cellSizeMeters.toFloat()),
        cellSizeMeters = cellSizeMeters,
        type = type,
        sharedAt = sharedAt,
        confirmations = confirmations,
        lastConfirmedAt = lastConfirmedAt,
        reportCount = reportCount,
        goneCount = goneCount,
        hasPhoto = hasPhoto,
    )
}.getOrNull()

internal fun SharedSighting.toFirestoreDocument(): Map<String, Any> = buildMap {
    put("latitude", location.latitude)
    put("longitude", location.longitude)
    put("cellSizeMeters", cellSizeMeters)
    put(FirestoreSharedSightingRepository.TYPE, type.name)
    put(FirestoreSharedSightingRepository.GEOHASH, geohash)
    put("sharedAt", sharedAt.toTimestamp())
    put("confirmations", confirmations)
    put(FirestoreSharedSightingRepository.REPORT_COUNT, reportCount)
    lastConfirmedAt?.let { put("lastConfirmedAt", it.toTimestamp()) }
    put(FirestoreSharedSightingRepository.GONE_COUNT, goneCount)
    put(FirestoreSharedSightingRepository.HIDDEN, isHidden)
    put(FirestoreSharedSightingRepository.HAS_PHOTO, hasPhoto)
}

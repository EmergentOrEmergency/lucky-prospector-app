package com.luckyprospector.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luckyprospector.app.domain.Geohash
import com.luckyprospector.app.domain.MapArea
import com.luckyprospector.app.domain.SharedSighting
import com.luckyprospector.app.domain.SharedSightingRepository
import com.luckyprospector.app.domain.SightingReportReason
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SharedMapState(
    val sightings: List<SharedSighting> = emptyList(),
    val ownedIds: Set<String> = emptySet(),
    val confirmedIds: Set<String> = emptySet(),
    val reportedIds: Set<String> = emptySet(),
    val goneIds: Set<String> = emptySet(),
    val loading: Boolean = false,
    /** True while the listeners are down; they are being retried in the background. */
    val unavailable: Boolean = false,
    val confirmingId: String? = null,
    val confirmationFailed: Boolean = false,
    val reportingId: String? = null,
    val reportFailed: Boolean = false,
    val markingGoneId: String? = null,
    val markGoneFailed: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SharedMapViewModel(
    private val repository: SharedSightingRepository,
    uid: Flow<String?>,
) : ViewModel() {

    private val _state = MutableStateFlow(SharedMapState())
    val state: StateFlow<SharedMapState> = _state.asStateFlow()

    private val areaPrefixes = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            uid.collectLatest { account ->
                if (account == null) {
                    _state.value = SharedMapState()
                    return@collectLatest
                }
                // Reset what belonged to the previous account, but keep the outcome
                // of a confirmation or report that finished in the meantime.
                _state.update {
                    it.copy(
                        sightings = emptyList(),
                        ownedIds = emptySet(),
                        confirmedIds = emptySet(),
                        reportedIds = emptySet(),
                        goneIds = emptySet(),
                        loading = true,
                        unavailable = false,
                    )
                }
                // A listener that fails (network, expired token, rules not yet
                // deployed) used to leave the map in error until the next sign-in.
                // It is now retried with exponential backoff for as long as the
                // account stays signed in.
                var attempt = 0
                while (true) {
                    try {
                        observe().collect { snapshot ->
                            attempt = 0
                            _state.update { current ->
                                current.copy(
                                    sightings = snapshot.sightings,
                                    ownedIds = snapshot.ownedIds,
                                    confirmedIds = snapshot.confirmedIds,
                                    reportedIds = snapshot.reportedIds,
                                    goneIds = snapshot.goneIds,
                                    loading = false,
                                    unavailable = false,
                                )
                            }
                        }
                        return@collectLatest
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        _state.update { it.copy(loading = false, unavailable = true) }
                        delay(retryDelay(attempt++))
                    }
                }
            }
        }
    }

    private fun observe(): Flow<SharedMapState> = combine(
        areaPrefixes
            // Panning fires many camera-idle events; only a settled view is queried.
            .debounce(AREA_DEBOUNCE)
            .distinctUntilChanged()
            .flatMapLatest(repository::observeSharedSightings),
        repository.observeOwnedSightingIds(),
        repository.observeConfirmedSightingIds(),
        repository.observeReportedSightingIds(),
        repository.observeGoneSightingIds(),
    ) { sightings, owned, confirmed, reported, gone ->
        SharedMapState(sightings, owned, confirmed, reported, gone)
    }

    /** Photos are immutable once published, so each is fetched at most once per session. */
    private val photos = mutableMapOf<String, Deferred<ByteArray?>>()

    /** The shared photo of a pin; a failed fetch is forgotten so it can be retried. */
    suspend fun photo(sightingId: String): ByteArray? = withContext(Dispatchers.Main.immediate) {
        // The map asks from a background thread; the cache is only touched on main.
        val pending = photos.getOrPut(sightingId) {
            viewModelScope.async { repository.loadPhoto(sightingId) }
        }
        runCatching { pending.await() }
            .onFailure { photos.remove(sightingId) }
            .getOrNull()
    }

    fun setVisibleArea(area: MapArea) {
        areaPrefixes.value = Geohash.coveringPrefixes(area.south, area.west, area.north, area.east)
    }

    fun confirm(sighting: SharedSighting) {
        if (_state.value.confirmingId != null) return
        _state.update { it.copy(confirmingId = sighting.id, confirmationFailed = false) }
        viewModelScope.launch {
            val failed = runCatching { repository.confirm(sighting) }.isFailure
            _state.update { it.copy(confirmingId = null, confirmationFailed = failed) }
        }
    }

    fun report(sighting: SharedSighting, reason: SightingReportReason) {
        if (_state.value.reportingId != null) return
        _state.update { it.copy(reportingId = sighting.id, reportFailed = false) }
        viewModelScope.launch {
            val failed = runCatching { repository.report(sighting, reason) }.isFailure
            _state.update { it.copy(reportingId = null, reportFailed = failed) }
        }
    }

    fun markGone(sighting: SharedSighting) {
        if (_state.value.markingGoneId != null) return
        _state.update { it.copy(markingGoneId = sighting.id, markGoneFailed = false) }
        viewModelScope.launch {
            val failed = runCatching { repository.markGone(sighting) }.isFailure
            _state.update { it.copy(markingGoneId = null, markGoneFailed = failed) }
        }
    }

    companion object {
        internal val AREA_DEBOUNCE = 400.milliseconds
        private val FIRST_RETRY = 2.seconds
        private val MAX_RETRY = 60.seconds

        /** 2 s, 4 s, 8 s … capped at a minute. */
        internal fun retryDelay(attempt: Int): Duration {
            val factor = 1L shl attempt.coerceIn(0, 10)
            return (FIRST_RETRY * factor.toInt()).coerceAtMost(MAX_RETRY)
        }
    }
}

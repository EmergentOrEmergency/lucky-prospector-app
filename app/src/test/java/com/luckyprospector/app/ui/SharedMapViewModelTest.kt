package com.luckyprospector.app.ui

import com.luckyprospector.app.domain.Finding
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.GeoPoint
import com.luckyprospector.app.domain.Geohash
import com.luckyprospector.app.domain.MapArea
import com.luckyprospector.app.domain.SharedSighting
import com.luckyprospector.app.domain.SharedSightingRepository
import com.luckyprospector.app.domain.SightingReportReason
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedMapViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val pin = SharedSighting(
        id = "pin",
        location = GeoPoint(44.6475, 10.9255, 150f),
        cellSizeMeters = 150,
        type = FindingType.CLOVER_PATCH,
        sharedAt = Instant.parse("2026-09-20T10:00:00Z"),
    )

    private class FakeRepository : SharedSightingRepository {
        var failuresBeforeSuccess = 0
        var subscriptions = 0
        val requestedAreas = mutableListOf<Set<String>>()
        var sightings: List<SharedSighting> = emptyList()
        var confirmFails = false

        override fun observeSharedSightings(areaPrefixes: Set<String>): Flow<List<SharedSighting>> = flow {
            subscriptions++
            requestedAreas += areaPrefixes
            if (failuresBeforeSuccess-- > 0) error("network down")
            emit(sightings)
            kotlinx.coroutines.awaitCancellation()
        }
        override fun observeOwnedSightingIds(): Flow<Set<String>> = flowOf(emptySet())
        override fun observeConfirmedSightingIds(): Flow<Set<String>> = flowOf(emptySet())
        override fun observeReportedSightingIds(): Flow<Set<String>> = flowOf(emptySet())
        override fun observeGoneSightingIds(): Flow<Set<String>> = flowOf(emptySet())
        override suspend fun publish(finding: Finding, photoJpeg: ByteArray?): SharedSighting = error("unused")
        override suspend fun loadPhoto(sightingId: String): ByteArray? = null
        override suspend fun updateType(sightingId: String, type: FindingType) = Unit
        override suspend fun confirm(sighting: SharedSighting) { if (confirmFails) error("refused") }
        override suspend fun report(sighting: SharedSighting, reason: SightingReportReason) = Unit
        override suspend fun markGone(sighting: SharedSighting) = Unit
        override suspend fun withdraw(sightingId: String) = Unit
        override suspend fun withdrawAll(): Int = 0
        override suspend fun withdrawVotes() = Unit
    }

    private fun TestScope.settle() {
        advanceTimeBy(SharedMapViewModel.AREA_DEBOUNCE + 1.seconds)
        runCurrent()
    }

    @Test
    fun `a failed listener is retried and the map recovers`() = runTest(dispatcher) {
        val repository = FakeRepository().apply { failuresBeforeSuccess = 2; sightings = listOf(pin) }
        val viewModel = SharedMapViewModel(repository, MutableStateFlow("uid"))
        viewModel.setVisibleArea(MapArea(44.6, 10.9, 44.7, 11.0))
        settle()
        assertTrue(viewModel.state.value.unavailable)

        advanceTimeBy(SharedMapViewModel.retryDelay(0) + SharedMapViewModel.AREA_DEBOUNCE + 1.seconds)
        runCurrent()
        advanceTimeBy(SharedMapViewModel.retryDelay(1) + SharedMapViewModel.AREA_DEBOUNCE + 1.seconds)
        runCurrent()

        assertFalse(viewModel.state.value.unavailable)
        assertEquals(listOf(pin), viewModel.state.value.sightings)
        assertEquals(3, repository.subscriptions)
    }

    @Test
    fun `queries follow the visible area`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = SharedMapViewModel(repository, MutableStateFlow("uid"))
        viewModel.setVisibleArea(MapArea(44.6, 10.9, 44.7, 11.0))
        settle()
        viewModel.setVisibleArea(MapArea(45.4, 9.1, 45.5, 9.2))
        settle()

        assertEquals(2, repository.requestedAreas.size)
        val (first, last) = repository.requestedAreas
        assertTrue(first.any(Geohash.encode(44.65, 10.95)::startsWith))
        assertTrue(last.any(Geohash.encode(45.45, 9.15)::startsWith))
        assertFalse(last.any(Geohash.encode(44.65, 10.95)::startsWith))
    }

    @Test
    fun `signing out clears the shared state`() = runTest(dispatcher) {
        val repository = FakeRepository().apply { sightings = listOf(pin) }
        val uid = MutableStateFlow<String?>("uid")
        val viewModel = SharedMapViewModel(repository, uid)
        viewModel.setVisibleArea(MapArea(44.6, 10.9, 44.7, 11.0))
        settle()
        assertEquals(listOf(pin), viewModel.state.value.sightings)

        uid.value = null
        runCurrent()
        assertEquals(SharedMapState(), viewModel.state.value)
    }

    @Test
    fun `a refused confirmation is reported and can be retried`() = runTest(dispatcher) {
        val repository = FakeRepository().apply { confirmFails = true }
        val viewModel = SharedMapViewModel(repository, MutableStateFlow("uid"))
        viewModel.confirm(pin)
        assertEquals("pin", viewModel.state.value.confirmingId)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.confirmationFailed)
        assertEquals(null, viewModel.state.value.confirmingId)
    }

    @Test
    fun `retry delay grows and is capped`() {
        assertEquals(2.seconds, SharedMapViewModel.retryDelay(0))
        assertEquals(4.seconds, SharedMapViewModel.retryDelay(1))
        assertEquals(60.seconds, SharedMapViewModel.retryDelay(10))
    }
}

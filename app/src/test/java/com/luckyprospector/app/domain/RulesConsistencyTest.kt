package com.luckyprospector.app.domain

import com.luckyprospector.app.data.remote.FeedbackCategory
import com.luckyprospector.app.data.remote.FeedbackRepository
import com.luckyprospector.app.data.remote.FirestoreSharedSightingRepository
import com.luckyprospector.app.sharing.SharedPhotoPreparer
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Removal thresholds and limits are enforced by firestore.rules and mirrored in
 * Kotlin so the app can explain them. This fails when one side changes alone.
 */
class RulesConsistencyTest {

    private val rules = listOf(File("../firestore.rules"), File("firestore.rules")).first(File::exists).readText()

    @Test
    fun `security rules use the same thresholds and limits as the app`() {
        assertTrue(rules.contains("counter(data, 'reportCount') >= ${SharedSighting.REPORT_HIDE_THRESHOLD}"))
        assertTrue(rules.contains("counter(data, 'goneCount') >= ${SharedSighting.GONE_HIDE_THRESHOLD}"))
        assertTrue(rules.contains("request.resource.data.count <= ${FirestoreSharedSightingRepository.MAX_PUBLICATIONS_PER_DAY}"))
        assertTrue(rules.contains("cellSizeMeters >= ${GeoCoarsening.MIN_CELL_METERS}"))
        assertTrue(rules.contains("geohash.size() == ${Geohash.STORED_PRECISION}"))
        assertTrue(rules.contains("jpeg.size() <= ${SharedPhotoPreparer.MAX_BYTES}"))
        assertTrue(rules.contains("request.resource.data.count <= ${FeedbackRepository.MAX_PER_DAY}"))
        assertTrue(rules.contains("message.size() <= ${FeedbackRepository.MAX_MESSAGE}"))
        assertTrue(rules.contains("contact.size() <= ${FeedbackRepository.MAX_CONTACT}"))
        assertTrue(rules.contains("category in ${FeedbackCategory.entries.joinToString(", ", "[", "]") { "'${it.name}'" }}"))
    }

    @Test
    fun `pins never expire on the server`() {
        assertFalse(rules.contains("expiresAt"))
    }

    @Test
    fun `hidden pins are erased after the retention the app writes`() {
        val days = FirestoreSharedSightingRepository.HIDDEN_RETENTION.toDays()
        assertTrue(rules.contains("purgeAt > request.time + duration.value(${days - 1}, 'd')"))
        assertTrue(rules.contains("purgeAt < request.time + duration.value(${days + 1}, 'd')"))
    }

    @Test
    fun `one person saying a pin is gone is not enough to remove it`() {
        val pin = SharedSighting(
            id = "pin",
            location = GeoPoint(44.6475, 10.9255, 150f),
            cellSizeMeters = 150,
            type = FindingType.CLOVER_PATCH,
            sharedAt = java.time.Instant.EPOCH,
        )
        assertFalse(pin.copy(goneCount = 1).isHidden)
        assertTrue(pin.copy(goneCount = 2).isHidden)
        assertTrue(pin.copy(reportCount = 3).isHidden)
    }
}

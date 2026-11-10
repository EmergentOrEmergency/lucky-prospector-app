package com.luckyprospector.app.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SharedSightingTest {

    private val finding = Finding(
        type = FindingType.FOUR_LEAF_CLOVER,
        privateImageUri = "file:///private/photo.jpg",
        galleryImageUri = null,
        location = GeoPoint(44.6471, 10.9252, 6f),
        wish = "un desiderio privato",
        prediction = ModelPrediction("clover_4", 0.82f, "0.1.0"),
    )

    @Test
    fun `the published position is never the recorded one`() {
        val shared = SharedSighting.from("s1", finding)!!

        assertNotEquals(finding.location, shared.location)
        assertEquals(GeoCoarsening.coarsen(finding.location!!), shared.location)
    }

    @Test
    fun `a finding without a position cannot be shared`() {
        assertNull(SharedSighting.from("s1", finding.copy(location = null)))
    }

    @Test
    fun `an under-coarsened sighting cannot be constructed at all`() {
        // The guard lives in the constructor, so no code path -- including a future
        // one that bypasses `from` -- can produce a publishable object with a cell
        // small enough to identify a doorstep.
        assertThrows(IllegalArgumentException::class.java) {
            SharedSighting(
                id = "s1",
                location = GeoPoint(44.6, 10.9, 10f),
                cellSizeMeters = 10,
                type = FindingType.FOUR_LEAF_CLOVER,
                sharedAt = Instant.EPOCH,
            )
        }
    }

    @Test
    fun `the wish and the photo do not travel`() {
        val shared = SharedSighting.from("s1", finding)!!

        // A compile-time guarantee really, but asserting it means the day someone
        // adds a convenient `wish` field the test says why that is not convenient.
        val fields = shared.javaClass.declaredFields.map { it.name }.toSet()
        assertEquals(emptySet<String>(), fields.intersect(setOf("wish", "privateImageUri", "galleryImageUri", "ownerUid")))
    }
}

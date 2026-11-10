package com.luckyprospector.app.data.remote

import com.luckyprospector.app.domain.Finding
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.GeoPoint
import com.luckyprospector.app.domain.Geohash
import com.luckyprospector.app.domain.ModelPrediction
import com.luckyprospector.app.domain.SharedSighting
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedSightingDocumentTest {

    @Test
    fun `public document contains only the server-approved anonymous fields`() {
        val precise = GeoPoint(44.6471, 10.9252, 4f)
        val finding = Finding(
            id = "finding-1",
            type = FindingType.FOUR_LEAF_CLOVER,
            privateImageUri = "file:///private/exact-photo.jpg",
            galleryImageUri = "content://gallery/photo",
            location = precise,
            wish = "questo non deve uscire",
            prediction = ModelPrediction("four_leaf", .91f, "test"),
        )
        val document = SharedSighting.from("finding-1", finding, sharedAt = Instant.EPOCH)!!
            .toFirestoreDocument()

        assertEquals(
            setOf(
                "latitude", "longitude", "cellSizeMeters", "type", "sharedAt",
                "confirmations", "reportCount", "goneCount", "hidden", "geohash", "hasPhoto",
            ),
            document.keys,
        )
        // The geohash is derived from the coarsened point, never the precise one.
        assertEquals(
            Geohash.encode(document["latitude"] as Double, document["longitude"] as Double),
            document["geohash"],
        )
        assertNotEquals(Geohash.encode(precise.latitude, precise.longitude), document["geohash"])
        assertNotEquals(precise.latitude, document["latitude"])
        assertNotEquals(precise.longitude, document["longitude"])
        assertFalse(document.values.any { it == finding.wish })
        assertFalse(document.values.contains(finding.privateImageUri))
        assertFalse(document.values.any { it == finding.galleryImageUri })
    }

    @Test
    fun `public document round trips into a shared sighting`() {
        val original = SharedSighting(
            id = "shared-1",
            location = GeoPoint(44.7, 10.8, 150f),
            cellSizeMeters = 150,
            type = FindingType.CLOVER_PATCH,
            sharedAt = Instant.parse("2026-09-20T10:00:00Z"),
            confirmations = 3,
            lastConfirmedAt = Instant.parse("2026-09-23T12:30:00Z"),
            reportCount = 2,
            goneCount = 1,
            hasPhoto = true,
        )

        assertEquals(original, original.toFirestoreDocument().toSharedSighting(original.id))
    }

    @Test
    fun `malformed public document is ignored instead of crashing the map`() {
        val malformed = mapOf<String, Any?>(
            "latitude" to 44.7,
            "longitude" to 10.8,
            "cellSizeMeters" to 50,
            "type" to "FOUR_LEAF_CLOVER",
            "sharedAt" to com.google.firebase.Timestamp(0, 0),
            "confirmations" to 0,
        )

        assertNull(malformed.toSharedSighting("bad"))
    }

    @Test
    fun `unknown public type is ignored instead of crashing the map`() {
        val malformed = mapOf<String, Any?>(
            "latitude" to 44.7,
            "longitude" to 10.8,
            "cellSizeMeters" to 150,
            "type" to "OWNER_NAME_OR_OTHER_FORBIDDEN_VALUE",
            "sharedAt" to com.google.firebase.Timestamp(0, 0),
            "confirmations" to 0,
        )

        assertNull(malformed.toSharedSighting("bad"))
    }
}

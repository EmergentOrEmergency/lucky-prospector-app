package com.luckyprospector.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindingValidatorTest {
    @Test
    fun acceptsOfflineFindingWithoutLocationOrAccount() {
        val finding = Finding(
            type = FindingType.CLOVER_PATCH,
            privateImageUri = "file:///private/photo.jpg",
            galleryImageUri = null,
            location = null,
            wish = "Che sia una bella giornata",
            prediction = null,
        )

        assertTrue(FindingValidator.validate(finding).isEmpty())
    }

    @Test
    fun rejectsInvalidCoordinatesAndConfidence() {
        val finding = Finding(
            type = FindingType.FOUR_LEAF_CLOVER,
            privateImageUri = "file:///private/photo.jpg",
            galleryImageUri = null,
            location = GeoPoint(91.0, 181.0, -1f),
            wish = null,
            prediction = ModelPrediction("clover_4", 1.2f, "0.1.0"),
        )

        assertEquals(4, FindingValidator.validate(finding).size)
    }
}


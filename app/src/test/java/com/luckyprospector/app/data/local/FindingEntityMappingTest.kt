package com.luckyprospector.app.data.local

import com.luckyprospector.app.domain.Finding
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.GeoPoint
import com.luckyprospector.app.domain.ModelPrediction
import com.luckyprospector.app.domain.SyncState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class FindingEntityMappingTest {
    @Test fun roundTripPreservesOfflineFinding() {
        val finding = Finding(
            id = "finding-1",
            createdAt = Instant.parse("2026-09-03T14:00:00Z"),
            type = FindingType.FOUR_LEAF_CLOVER,
            privateImageUri = "file:///private/clover.jpg",
            galleryImageUri = "content://media/clover",
            location = GeoPoint(45.46, 9.19, 4.5f),
            wish = "  fortuna  ",
            prediction = ModelPrediction("four_leaf", .91f, "v1", "FOUR_LEAF_CLOVER"),
            syncState = SyncState.PENDING,
        )

        val restored = finding.toEntity().toDomain()

        assertEquals(finding.copy(wish = "fortuna"), restored)
    }

    @Test fun roundTripSupportsMissingLocationAndPrediction() {
        val finding = Finding(
            createdAt = Instant.parse("2026-09-03T15:00:00Z"),
            type = FindingType.CLOVER_PATCH,
            privateImageUri = "file:///private/patch.jpg",
            galleryImageUri = null,
            location = null,
            wish = null,
            prediction = null,
        )
        assertEquals(finding, finding.toEntity().toDomain())
    }
}

package com.luckyprospector.app.domain

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedSightingReportTest {
    private fun sighting(reportCount: Int) = SharedSighting(
        id = "reported-pin",
        location = GeoPoint(45.0, 9.0, 150f),
        cellSizeMeters = 150,
        type = FindingType.CLOVER_PATCH,
        sharedAt = Instant.EPOCH,
        reportCount = reportCount,
    )

    @Test
    fun `pin stays visible below independent report threshold`() {
        assertFalse(sighting(SharedSighting.REPORT_HIDE_THRESHOLD - 1).isHiddenByReports)
    }

    @Test
    fun `pin hides at independent report threshold`() {
        assertTrue(sighting(SharedSighting.REPORT_HIDE_THRESHOLD).isHiddenByReports)
    }
}

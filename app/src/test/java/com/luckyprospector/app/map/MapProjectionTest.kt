package com.luckyprospector.app.map

import com.luckyprospector.app.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapProjectionTest {
    @Test
    fun `all projected points stay inside padded viewport`() {
        val positions = MapProjection.project(listOf(
            GeoPoint(45.0, 9.0, null),
            GeoPoint(46.0, 10.0, null),
            GeoPoint(45.5, 9.2, null),
        ))

        assertTrue(positions.all { it.x in 0.12f..0.88f && it.y in 0.12f..0.88f })
    }

    @Test
    fun `single point is centered`() {
        val position = MapProjection.project(listOf(GeoPoint(45.0, 9.0, null))).single()

        assertEquals(0.5f, position.x, 0.001f)
        assertEquals(0.5f, position.y, 0.001f)
    }

    @Test
    fun `north is rendered above south`() {
        val positions = MapProjection.project(listOf(
            GeoPoint(46.0, 9.0, null),
            GeoPoint(45.0, 9.0, null),
        ))

        assertTrue(positions[0].y < positions[1].y)
    }
}

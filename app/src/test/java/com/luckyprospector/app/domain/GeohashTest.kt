package com.luckyprospector.app.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeohashTest {

    @Test
    fun `encodes the reference vectors`() {
        assertEquals("u4pruydqqvj", Geohash.encode(57.64911, 10.40744, 11))
        assertEquals("ezs42", Geohash.encode(42.605, -5.603, 5))
    }

    @Test
    fun `stored precision is nine characters`() {
        assertEquals(9, Geohash.encode(44.6471, 10.9252).length)
    }

    @Test
    fun `covering prefixes stay within the listener cap`() {
        val boxes = listOf(
            MapArea(44.64, 10.91, 44.66, 10.94), // a neighbourhood
            MapArea(44.4, 10.6, 44.9, 11.2), // a province
            MapArea.ITALY,
        )
        boxes.forEach { box ->
            val prefixes = Geohash.coveringPrefixes(box.south, box.west, box.north, box.east)
            assertTrue("${prefixes.size} prefixes for $box", prefixes.size <= Geohash.MAX_QUERY_CELLS)
        }
    }

    @Test
    fun `every point inside the box matches one of its prefixes`() {
        val box = MapArea(44.4, 10.6, 44.9, 11.2)
        val prefixes = Geohash.coveringPrefixes(box.south, box.west, box.north, box.east)
        val random = Random(7)
        repeat(2_000) {
            val latitude = box.south + random.nextDouble() * (box.north - box.south)
            val longitude = box.west + random.nextDouble() * (box.east - box.west)
            val hash = Geohash.encode(latitude, longitude)
            assertTrue("$latitude,$longitude -> $hash", prefixes.any(hash::startsWith))
        }
    }

    @Test
    fun `a small box is queried at a fine precision`() {
        val prefixes = Geohash.coveringPrefixes(44.645, 10.920, 44.650, 10.930)
        assertTrue(prefixes.all { it.length >= 5 })
    }
}

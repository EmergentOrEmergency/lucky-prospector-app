package com.luckyprospector.app.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoCoarseningTest {

    // Modena, roughly. Any mid-latitude point exercises the longitude convergence.
    private val precise = GeoPoint(44.6471, 10.9252, 8f)

    @Test
    fun `coarsening is deterministic`() {
        assertEquals(GeoCoarsening.coarsen(precise), GeoCoarsening.coarsen(precise))
    }

    @Test
    fun `nearby positions collapse onto the same published point`() {
        // Two positions a few metres apart, well inside one 150 m cell.
        val a = GeoPoint(44.64710, 10.92520, 8f)
        val b = GeoPoint(44.64714, 10.92526, 12f)

        val coarseA = GeoCoarsening.coarsen(a)
        val coarseB = GeoCoarsening.coarsen(b)

        // Guard against a vacuous pass if the pair happened to straddle a boundary.
        assertTrue(
            "test positions must share a cell for this assertion to mean anything",
            metresBetween(a, coarseA) < 150.0 && metresBetween(b, coarseB) < 150.0,
        )
        assertEquals(coarseA, coarseB)
    }

    @Test
    fun `the published point stays within the cell it represents`() {
        val cell = 150
        val coarse = GeoCoarsening.coarsen(precise, cell)

        // Snapping to the centre bounds the error at half a cell per axis.
        assertTrue(
            "published point drifted ${metresBetween(precise, coarse)} m from the original",
            metresBetween(precise, coarse) <= cell.toDouble(),
        )
    }

    @Test
    fun `distinct areas remain distinguishable`() {
        val elsewhere = GeoPoint(44.6600, 10.9400, 8f)

        assertNotEquals(GeoCoarsening.coarsen(precise), GeoCoarsening.coarsen(elsewhere))
    }

    @Test
    fun `reported accuracy becomes the cell size rather than the gps accuracy`() {
        val coarse = GeoCoarsening.coarsen(precise, 200)

        assertEquals(200f, coarse.accuracyMeters)
    }

    @Test
    fun `a larger cell hides more`() {
        // A position and its neighbour 300 m away share a 1 km cell but not a 150 m one.
        val neighbour = GeoPoint(precise.latitude + 0.0027, precise.longitude, 8f)

        assertNotEquals(
            GeoCoarsening.coarsen(precise, 150),
            GeoCoarsening.coarsen(neighbour, 150),
        )
        assertEquals(
            GeoCoarsening.coarsen(precise, 1000),
            GeoCoarsening.coarsen(neighbour, 1000),
        )
    }

    @Test
    fun `cells below the minimum are refused`() {
        // Rejecting this in code as well as in the security rules means a caller
        // cannot quietly weaken the guarantee by passing a small number.
        assertThrows(IllegalArgumentException::class.java) {
            GeoCoarsening.coarsen(precise, 10)
        }
    }

    @Test
    fun `longitudes near the antimeridian stay in range`() {
        val coarse = GeoCoarsening.coarsen(GeoPoint(-16.5, 179.99, 5f))

        assertTrue("longitude ${coarse.longitude} left the valid range", coarse.longitude in -180.0..180.0)
        assertTrue("latitude ${coarse.latitude} left the valid range", coarse.latitude in -90.0..90.0)
    }

    @Test
    fun `positions near the pole do not blow up`() {
        val coarse = GeoCoarsening.coarsen(GeoPoint(89.9999, 24.0, 5f))

        assertTrue(coarse.longitude in -180.0..180.0)
        assertTrue(coarse.latitude in -90.0..90.0)
    }

    private fun metresBetween(a: GeoPoint, b: GeoPoint): Double {
        val metresPerDegree = 111_320.0
        val dLat = (a.latitude - b.latitude) * metresPerDegree
        val dLon = (a.longitude - b.longitude) * metresPerDegree *
            abs(cos(Math.toRadians((a.latitude + b.latitude) / 2)))
        return hypot(dLat, dLon)
    }
}

package com.luckyprospector.app.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor

/**
 * Reduces a precise position to the centre of a grid cell, so a shared sighting
 * says "quadrifogli crescono in questa zona" without saying "questa persona era
 * esattamente qui".
 *
 * The coarsening is deliberately lossy and many-to-one: every position inside a
 * cell collapses onto the same point, so the original cannot be recovered from
 * the result. That property is the whole point, and the unit tests assert it
 * directly rather than trusting it.
 *
 * Apply this once, when the user chooses to share, and store only the result in
 * the shared collection. Never store the precise position publicly and coarsen
 * when drawing -- anything written to a shared document is readable by every
 * client regardless of what the UI chooses to display.
 */
object GeoCoarsening {

    /** Minimum the security rules accept; smaller cells stop protecting anything. */
    const val MIN_CELL_METERS = 100

    const val DEFAULT_CELL_METERS = 150

    private const val METERS_PER_DEGREE_LATITUDE = 111_320.0

    fun coarsen(point: GeoPoint, cellMeters: Int = DEFAULT_CELL_METERS): GeoPoint {
        require(cellMeters >= MIN_CELL_METERS) {
            "cellMeters must be at least $MIN_CELL_METERS, was $cellMeters"
        }

        val latitudeStep = cellMeters / METERS_PER_DEGREE_LATITUDE
        val latitude = snapToCellCentre(point.latitude, latitudeStep).coerceIn(-90.0, 90.0)

        // The longitude step is derived from the *snapped* latitude, not the original.
        // Meridians converge towards the poles, so the step depends on latitude; deriving
        // it from the raw value would give two neighbours in the same cell slightly
        // different grids, and they would no longer collapse onto the same point.
        val cosLatitude = abs(cos(Math.toRadians(latitude)))
        val longitudeStep = if (cosLatitude < 1e-6) {
            360.0 // At the poles every longitude is effectively the same place.
        } else {
            cellMeters / (METERS_PER_DEGREE_LATITUDE * cosLatitude)
        }
        val longitude = wrapLongitude(snapToCellCentre(point.longitude, longitudeStep))

        // The reported accuracy becomes the cell size: the point is no longer a
        // measurement, and presenting it with the original GPS accuracy would
        // overstate what it actually tells you.
        return GeoPoint(latitude, longitude, cellMeters.toFloat())
    }

    private fun snapToCellCentre(value: Double, step: Double): Double =
        (floor(value / step) + 0.5) * step

    private fun wrapLongitude(value: Double): Double {
        var wrapped = (value + 180.0) % 360.0
        if (wrapped < 0) wrapped += 360.0
        return wrapped - 180.0
    }
}

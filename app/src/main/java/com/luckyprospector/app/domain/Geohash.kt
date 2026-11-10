package com.luckyprospector.app.domain

import kotlin.math.ceil

/** The part of the map currently on screen, in degrees. */
data class MapArea(val south: Double, val west: Double, val north: Double, val east: Double) {
    companion object {
        /** Used when no geographic map reports a viewport, e.g. the offline canvas. */
        val ITALY = MapArea(south = 35.4, west = 6.6, north = 47.1, east = 18.6)
    }
}

/**
 * Geohash encoding, used only to query the shared map by area.
 *
 * A shared pin carries the geohash of its *coarsened* position, so the field adds
 * nothing to what the public latitude and longitude already say. Its only purpose
 * is to let the map ask Firestore for the pins in view instead of downloading the
 * whole collection on every start.
 */
object Geohash {

    /** Precision stored on each pin: ~5 m, finer than any cell it describes. */
    const val STORED_PRECISION = 9

    /** Upper bound on prefix queries per viewport; each one is a Firestore listener. */
    const val MAX_QUERY_CELLS = 12

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun encode(latitude: Double, longitude: Double, precision: Int = STORED_PRECISION): String {
        require(precision in 1..12) { "precision must be in 1..12, was $precision" }
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0
        val hash = StringBuilder(precision)
        var bit = 0
        var value = 0
        var evenBit = true
        while (hash.length < precision) {
            if (evenBit) {
                val mid = (lonMin + lonMax) / 2
                if (longitude >= mid) { value = value * 2 + 1; lonMin = mid } else { value *= 2; lonMax = mid }
            } else {
                val mid = (latMin + latMax) / 2
                if (latitude >= mid) { value = value * 2 + 1; latMin = mid } else { value *= 2; latMax = mid }
            }
            evenBit = !evenBit
            if (++bit == 5) {
                hash.append(BASE32[value])
                bit = 0
                value = 0
            }
        }
        return hash.toString()
    }

    /** Width and height in degrees of a cell at [precision]. */
    internal fun cellSize(precision: Int): Pair<Double, Double> {
        val bits = precision * 5
        val lonBits = (bits + 1) / 2
        val latBits = bits / 2
        return 360.0 / (1L shl lonBits) to 180.0 / (1L shl latBits)
    }

    /**
     * The geohash prefixes whose cells together cover a bounding box, at the finest
     * precision that needs no more than [maxCells] of them. A prefix query returns
     * every pin whose stored hash starts with it.
     */
    fun coveringPrefixes(
        south: Double,
        west: Double,
        north: Double,
        east: Double,
        maxCells: Int = MAX_QUERY_CELLS,
    ): Set<String> {
        require(south <= north) { "south must not exceed north" }
        // A box crossing the antimeridian is widened to the whole longitude range:
        // rare for a clover map, and correct, just less selective.
        val (lonFrom, lonTo) = if (west <= east) west to east else -180.0 to 180.0
        val latSpan = north - south
        val lonSpan = lonTo - lonFrom

        val precision = (STORED_PRECISION downTo 1).firstOrNull { candidate ->
            val (cellWidth, cellHeight) = cellSize(candidate)
            val columns = ceil(lonSpan / cellWidth).toInt() + 1
            val rows = ceil(latSpan / cellHeight).toInt() + 1
            columns.toLong() * rows <= maxCells
        } ?: 1

        val (cellWidth, cellHeight) = cellSize(precision)
        val prefixes = linkedSetOf<String>()
        var latitude = south
        while (true) {
            var longitude = lonFrom
            while (true) {
                prefixes += encode(latitude.coerceIn(-90.0, 90.0), longitude.coerceIn(-180.0, 179.999999), precision)
                if (longitude >= lonTo) break
                longitude = minOf(longitude + cellWidth, lonTo)
            }
            if (latitude >= north) break
            latitude = minOf(latitude + cellHeight, north)
        }
        // Only a box wider than the cap allows even at precision 1 exceeds maxCells;
        // the whole world is 32 cells there, so the result stays bounded regardless.
        return prefixes
    }
}

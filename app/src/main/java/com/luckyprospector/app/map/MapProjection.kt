package com.luckyprospector.app.map

import com.luckyprospector.app.domain.GeoPoint

data class MapPosition(val x: Float, val y: Float)

/** Projects coordinates into a normalized 0..1 viewport and handles tiny bounds. */
object MapProjection {
    fun project(points: List<GeoPoint>, padding: Float = 0.12f): List<MapPosition> {
        if (points.isEmpty()) return emptyList()
        require(padding in 0f..0.45f)

        val minLatitude = points.minOf { it.latitude }
        val maxLatitude = points.maxOf { it.latitude }
        val minLongitude = points.minOf { it.longitude }
        val maxLongitude = points.maxOf { it.longitude }
        val latitudeSpan = (maxLatitude - minLatitude).takeUnless { it == 0.0 } ?: 1.0
        val longitudeSpan = (maxLongitude - minLongitude).takeUnless { it == 0.0 } ?: 1.0
        val drawable = 1f - padding * 2f

        return points.map { point ->
            val rawX = if (minLongitude == maxLongitude) 0.5 else (point.longitude - minLongitude) / longitudeSpan
            val rawY = if (minLatitude == maxLatitude) 0.5 else (maxLatitude - point.latitude) / latitudeSpan
            MapPosition(
                x = padding + rawX.toFloat() * drawable,
                y = padding + rawY.toFloat() * drawable,
            )
        }
    }
}


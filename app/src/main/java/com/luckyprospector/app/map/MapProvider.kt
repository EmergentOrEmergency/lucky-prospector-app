package com.luckyprospector.app.map

/**
 * Boundary between the product UI and a future tiled map SDK.
 *
 * [LocalCanvasMapProvider] remains the offline fallback. The development build
 * can select [OpenFreeMapDevelopmentProvider] without coupling product code to that
 * temporary tile host.
 */
interface MapProvider {
    val id: String
    val displayName: String
    val worksWithoutBasemap: Boolean
}

data object LocalCanvasMapProvider : MapProvider {
    override val id = "local-canvas"
    override val displayName = "Local map"
    override val worksWithoutBasemap = true
}

data class MapLibreMapProvider(
    override val id: String,
    override val displayName: String,
    val styleUri: String,
    val productionReady: Boolean,
) : MapProvider {
    override val worksWithoutBasemap = false
}

/** Development-only basemap approved for on-device evaluation. */
val OpenFreeMapDevelopmentProvider = MapLibreMapProvider(
    id = "openfreemap-liberty-development",
    displayName = "OpenFreeMap Liberty",
    styleUri = "https://tiles.openfreemap.org/styles/liberty",
    productionReady = false,
)


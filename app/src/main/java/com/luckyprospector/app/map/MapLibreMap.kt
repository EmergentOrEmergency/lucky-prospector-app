package com.luckyprospector.app.map

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.luckyprospector.app.domain.MapArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

internal data class GeographicMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    /** Photo shown inside the pin, when there is one. */
    val photo: MarkerPhoto? = null,
    /** Ring colour of a photo pin, as ARGB. */
    val accentArgb: Int = 0,
)

/**
 * Geographic basemap with client-side annotations.
 *
 * The tile host sees the requested viewport. Marker payloads are not uploaded:
 * private coordinates remain client-side, while shared coordinates have already
 * been coarsened by the domain boundary before they arrive here.
 */
@Composable
internal fun MapLibreMapView(
    markers: List<GeographicMarker>,
    provider: MapLibreMapProvider,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * When false the camera is fitted to the markers only the first time there are
     * any. The shared map needs this: its markers depend on the visible area, and
     * refitting on every update would move the view the user just chose.
     */
    refitOnMarkerChange: Boolean = true,
    onVisibleAreaChange: ((MapArea) -> Unit)? = null,
    /** Fetches a shared pin's photo; only called for [MarkerPhoto.Shared]. */
    loadSharedPhoto: suspend (String) -> ByteArray? = { null },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)
    val currentOnVisibleAreaChange by rememberUpdatedState(onVisibleAreaChange)
    var fittedOnce by remember { mutableStateOf(false) }
    var initialViewSet by remember { mutableStateOf(false) }
    // Thumbnails are decoded once per photo and reused across marker updates.
    val photoIcons = remember { mutableMapOf<String, Icon?>() }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(Bundle()) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleLoaded by remember { mutableStateOf(false) }

    DisposableEffect(mapView, lifecycleOwner) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView, provider.styleUri) {
        styleLoaded = false
        mapView.getMapAsync { readyMap ->
            readyMap.uiSettings.apply {
                isCompassEnabled = true
                isAttributionEnabled = true
                isLogoEnabled = true
                isRotateGesturesEnabled = true
                isScrollGesturesEnabled = true
                isZoomGesturesEnabled = true
            }
            readyMap.addOnCameraIdleListener {
                val bounds = readyMap.projection.visibleRegion.latLngBounds
                currentOnVisibleAreaChange?.invoke(
                    MapArea(
                        south = bounds.latitudeSouth,
                        west = bounds.longitudeWest,
                        north = bounds.latitudeNorth,
                        east = bounds.longitudeEast,
                    ),
                )
            }
            readyMap.setOnMarkerClickListener { marker ->
                marker.snippet?.let(currentOnMarkerClick)
                true
            }
            readyMap.setStyle(Style.Builder().fromUri(provider.styleUri)) {
                map = readyMap
                styleLoaded = true
            }
        }
    }

    val markerSignature = markers.joinToString("|") {
        "${it.id}:${it.latitude}:${it.longitude}:${it.title}:${it.photo?.key}:${it.accentArgb}"
    }
    LaunchedEffect(map, styleLoaded, markerSignature) {
        val readyMap = map ?: return@LaunchedEffect
        if (!styleLoaded) return@LaunchedEffect

        val missing = markers.filter { it.photo != null && it.iconKey !in photoIcons }
        if (missing.isNotEmpty()) {
            val iconFactory = IconFactory.getInstance(context)
            val decoded = withContext(Dispatchers.IO) {
                missing.associate { marker ->
                    val bitmap = when (val photo = requireNotNull(marker.photo)) {
                        is MarkerPhoto.Local -> PhotoMarkerIcons.create(context, photo.uri, marker.accentArgb)
                        is MarkerPhoto.Shared -> runCatching { loadSharedPhoto(photo.sightingId) }.getOrNull()
                            ?.let { PhotoMarkerIcons.create(context, it, marker.accentArgb) }
                    }
                    marker.iconKey to bitmap
                }
            }
            decoded.forEach { (key, bitmap) ->
                // A local photo that cannot be read will not become readable; a shared
                // one may have failed on the network, so it is tried again next time.
                if (bitmap != null || key.startsWith("local:")) photoIcons[key] = bitmap?.let(iconFactory::fromBitmap)
            }
        }

        readyMap.clear()
        markers.forEach { marker ->
            val options = MarkerOptions()
                .position(LatLng(marker.latitude, marker.longitude))
                .title(marker.title)
                .snippet(marker.id)
            // A photo that cannot be read falls back to the standard pin.
            photoIcons[marker.iconKey]?.let(options::icon)
            readyMap.addMarker(options)
        }

        if (!refitOnMarkerChange) {
            // Set the opening view once, then fit to the first markers once; after
            // that the camera belongs to the user, including over empty areas.
            if (fittedOnce) return@LaunchedEffect
            if (markers.isNotEmpty()) {
                fittedOnce = true
            } else if (initialViewSet) {
                return@LaunchedEffect
            }
            initialViewSet = true
        }
        mapView.post {
            when (markers.size) {
                0 -> readyMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(41.9, 12.5), 4.8),
                )
                1 -> readyMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(markers.first().latitude, markers.first().longitude),
                        15.0,
                    ),
                )
                else -> {
                    val bounds = LatLngBounds.Builder().apply {
                        markers.forEach { include(LatLng(it.latitude, it.longitude)) }
                    }.build()
                    readyMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 96))
                }
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

private val GeographicMarker.iconKey get() = "${photo?.key}:$accentArgb"

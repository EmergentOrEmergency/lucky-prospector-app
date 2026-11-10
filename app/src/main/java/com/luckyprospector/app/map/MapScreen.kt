package com.luckyprospector.app.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luckyprospector.app.domain.Finding
import com.luckyprospector.app.R
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.GeoPoint
import com.luckyprospector.app.domain.MapArea
import com.luckyprospector.app.domain.FreshnessLevel
import com.luckyprospector.app.domain.FreshnessPolicy
import com.luckyprospector.app.domain.SharedSighting
import com.luckyprospector.app.domain.SightingReportReason
import com.luckyprospector.app.ui.components.SlidePhoto
import com.luckyprospector.app.ui.theme.LuckyTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class MapView { PRIVATE, SHARED }

private class MapViewportState {
    var zoom by mutableFloatStateOf(1f)
    var pan by mutableStateOf(Offset.Zero)

    fun zoomBy(factor: Float) {
        zoom = (zoom * factor).coerceIn(1f, 5f)
        pan = Offset.Zero
    }

    fun reset() {
        zoom = 1f
        pan = Offset.Zero
    }
}

@Composable
fun MapScreen(
    findings: List<Finding>,
    modifier: Modifier = Modifier,
    provider: MapProvider = OpenFreeMapDevelopmentProvider,
    onFindingClick: (String) -> Unit = {},
    sharedSightings: List<SharedSighting> = emptyList(),
    ownedSharedIds: Set<String> = emptySet(),
    confirmedSharedIds: Set<String> = emptySet(),
    reportedSharedIds: Set<String> = emptySet(),
    goneSharedIds: Set<String> = emptySet(),
    isSignedIn: Boolean = false,
    sharedLoading: Boolean = false,
    sharedError: String? = null,
    confirmingSightingId: String? = null,
    confirmationError: String? = null,
    reportingSightingId: String? = null,
    reportError: String? = null,
    markingGoneId: String? = null,
    markGoneError: String? = null,
    onSignInRequested: () -> Unit = {},
    onConfirmShared: (SharedSighting) -> Unit = {},
    onReportShared: (SharedSighting, SightingReportReason) -> Unit = { _, _ -> },
    onMarkGone: (SharedSighting) -> Unit = {},
    onVisibleAreaChange: (MapArea) -> Unit = {},
    loadSharedPhoto: suspend (String) -> ByteArray? = { null },
) {
    var view by rememberSaveable { mutableStateOf(MapView.PRIVATE) }
    var selectedSharedId by rememberSaveable { mutableStateOf<String?>(null) }
    val located = findings.filter { it.location != null }
    // Age never hides a pin; only reports or two "not there any more" do. The
    // server already withholds those, this only covers a pin removed moments ago.
    val visibleShared = sharedSightings.filterNot(SharedSighting::isHidden)
    val selectedShared = visibleShared.firstOrNull { it.id == selectedSharedId }
    val sharedScrollState = rememberScrollState()
    LaunchedEffect(visibleShared) {
        if (selectedSharedId != null && selectedShared == null) selectedSharedId = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (view == MapView.SHARED && selectedShared != null) {
                    Modifier.verticalScroll(sharedScrollState)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (view == MapView.PRIVATE) stringResource(R.string.map_private_title) else stringResource(R.string.map_shared_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (view == MapView.PRIVATE) stringResource(R.string.map_private_subtitle)
            else stringResource(R.string.map_shared_subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = view == MapView.PRIVATE,
                onClick = { view = MapView.PRIVATE },
                label = { Text(stringResource(R.string.map_tab_mine)) },
            )
            FilterChip(
                selected = view == MapView.SHARED,
                onClick = { view = MapView.SHARED },
                label = { Text(stringResource(R.string.map_tab_shared)) },
            )
        }

        if (view == MapView.PRIVATE) {
            PrivateMapContent(located, provider, onFindingClick)
        } else if (!isSignedIn) {
            SharedMapSignInPrompt(onSignInRequested)
        } else {
            SharedMapContent(
                sightings = visibleShared,
                provider = provider,
                selected = selectedShared,
                ownedIds = ownedSharedIds,
                confirmedIds = confirmedSharedIds,
                reportedIds = reportedSharedIds,
                goneIds = goneSharedIds,
                loading = sharedLoading,
                error = sharedError,
                confirmingSightingId = confirmingSightingId,
                confirmationError = confirmationError,
                reportingSightingId = reportingSightingId,
                reportError = reportError,
                markingGoneId = markingGoneId,
                markGoneError = markGoneError,
                onSelect = { selectedSharedId = it },
                onConfirm = onConfirmShared,
                onReport = onReportShared,
                onMarkGone = onMarkGone,
                onVisibleAreaChange = onVisibleAreaChange,
                loadSharedPhoto = loadSharedPhoto,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.PrivateMapContent(
    located: List<Finding>,
    provider: MapProvider,
    onFindingClick: (String) -> Unit,
) {
        val viewport = remember { MapViewportState() }
        val geographicProvider = provider as? MapLibreMapProvider
        val fourLeafTitle = stringResource(R.string.shared_type_four_leaf)
        val patchTitle = stringResource(R.string.type_clover_patch)
        val palette = LuckyTheme.colors
        val markers = located.map { finding ->
            val location = requireNotNull(finding.location)
            val fourLeaf = finding.type == FindingType.FOUR_LEAF_CLOVER
            GeographicMarker(
                id = finding.id,
                latitude = location.latitude,
                longitude = location.longitude,
                title = if (fourLeaf) fourLeafTitle else patchTitle,
                // The photo never leaves the phone: it is decoded here into a bitmap.
                photo = MarkerPhoto.Local(finding.privateImageUri),
                accentArgb = (if (fourLeaf) palette.navIndicator else palette.mapClover).toArgb(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cloverCount = located.count { it.type == FindingType.FOUR_LEAF_CLOVER }
            val patchCount = located.count { it.type == FindingType.CLOVER_PATCH }
            AssistChip(onClick = {}, label = { Text(pluralStringResource(R.plurals.clover_count, cloverCount, cloverCount)) }, leadingIcon = { Text("♣") })
            AssistChip(onClick = {}, label = { Text(pluralStringResource(R.plurals.patch_count, patchCount, patchCount)) }, leadingIcon = { Icon(Icons.Outlined.Eco, null) })
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .45f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
        ) {
            if (geographicProvider != null) {
                Box(Modifier.fillMaxSize()) {
                    MapLibreMapView(
                        markers = markers,
                        provider = geographicProvider,
                        onMarkerClick = onFindingClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (located.isEmpty()) {
                        MapStatusOverlay(
                            text = stringResource(R.string.map_no_private_pins_overlay),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                        )
                    }
                }
            } else if (located.isEmpty()) {
                EmptyMap()
            } else {
                Box(Modifier.fillMaxSize()) {
                    OfflineMapCanvas(located, onFindingClick, viewport)
                    MapZoomControls(viewport, Modifier.align(Alignment.TopEnd).padding(10.dp))
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .15f)),
            shadowElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (geographicProvider == null) Icons.Outlined.CloudOff else Icons.Outlined.Public,
                    contentDescription = null,
                )
                Column {
                    Text(
                        stringResource(if (geographicProvider == null) R.string.map_local_name else R.string.map_online_name),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(if (geographicProvider == null) R.string.map_offline_note else R.string.map_online_note),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(if (geographicProvider == null) R.string.map_gesture_hint else R.string.map_online_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.FilterAlt, contentDescription = stringResource(R.string.map_filter_content_description))
            }
        }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.SharedMapSignInPrompt(onSignInRequested: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().weight(1f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(painterResource(R.drawable.lucky_prospector_mark), null, Modifier.size(140.dp))
            Text(stringResource(R.string.map_sign_in_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.map_sign_in_description),
                modifier = Modifier.padding(vertical = 12.dp),
            )
            Button(onClick = onSignInRequested) { Text(stringResource(R.string.sign_in_continue)) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.SharedMapContent(
    sightings: List<SharedSighting>,
    provider: MapProvider,
    selected: SharedSighting?,
    ownedIds: Set<String>,
    confirmedIds: Set<String>,
    reportedIds: Set<String>,
    goneIds: Set<String>,
    loading: Boolean,
    error: String?,
    confirmingSightingId: String?,
    confirmationError: String?,
    reportingSightingId: String?,
    reportError: String?,
    markingGoneId: String?,
    markGoneError: String?,
    onSelect: (String) -> Unit,
    onConfirm: (SharedSighting) -> Unit,
    onReport: (SharedSighting, SightingReportReason) -> Unit,
    onMarkGone: (SharedSighting) -> Unit,
    onVisibleAreaChange: (MapArea) -> Unit,
    loadSharedPhoto: suspend (String) -> ByteArray?,
) {
    var reportTarget by remember { mutableStateOf<SharedSighting?>(null) }
    var goneTarget by remember { mutableStateOf<SharedSighting?>(null) }
    val viewport = remember { MapViewportState() }
    val geographicProvider = provider as? MapLibreMapProvider
    val fourLeafTitle = stringResource(R.string.shared_type_four_leaf)
    val patchTitle = stringResource(R.string.type_clover_patch)
    if (geographicProvider == null) {
        // The offline canvas has no viewport to report; query a fixed area instead.
        LaunchedEffect(Unit) { onVisibleAreaChange(MapArea.ITALY) }
    }
    val palette = LuckyTheme.colors
    val geographicMarkers = sightings.map { sighting ->
        val fourLeaf = sighting.type == FindingType.FOUR_LEAF_CLOVER
        GeographicMarker(
            id = sighting.id,
            latitude = sighting.location.latitude,
            longitude = sighting.location.longitude,
            title = if (fourLeaf) fourLeafTitle else patchTitle,
            photo = if (sighting.hasPhoto) MarkerPhoto.Shared(sighting.id) else null,
            accentArgb = (if (fourLeaf) palette.navIndicator else palette.mapClover).toArgb(),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val cloverCount = sightings.count { it.type == FindingType.FOUR_LEAF_CLOVER }
        val patchCount = sightings.count { it.type == FindingType.CLOVER_PATCH }
        AssistChip(onClick = {}, label = { Text(pluralStringResource(R.plurals.clover_count, cloverCount, cloverCount)) })
        AssistChip(onClick = {}, label = { Text(pluralStringResource(R.plurals.patch_count, patchCount, patchCount)) })
    }

    Card(
        modifier = if (selected == null) {
            Modifier.fillMaxWidth().weight(1f)
        } else {
            // Keep the geographic context usable when the detail card appears.
            // The page becomes scrollable instead of compressing the map into a strip.
            Modifier.fillMaxWidth().height(300.dp)
        },
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
    ) {
        when {
            geographicProvider != null -> Box(Modifier.fillMaxSize()) {
                MapLibreMapView(
                    markers = geographicMarkers,
                    provider = geographicProvider,
                    onMarkerClick = onSelect,
                    modifier = Modifier.fillMaxSize(),
                    refitOnMarkerChange = false,
                    onVisibleAreaChange = onVisibleAreaChange,
                    loadSharedPhoto = loadSharedPhoto,
                )
                when {
                    loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    error != null -> MapStatusOverlay(
                        text = stringResource(R.string.error_shared_map),
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                    sightings.isEmpty() -> MapStatusOverlay(
                        text = stringResource(R.string.map_no_recent_pins),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    )
                }
            }
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.error_shared_map))
            }
            sightings.isEmpty() -> Box(Modifier.fillMaxSize().background(LuckyTheme.colors.mapPaper).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.lucky_prospector_mark), null, Modifier.size(130.dp))
                    Text(stringResource(R.string.map_no_recent_pins), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.map_first_shared_pin), style = MaterialTheme.typography.bodySmall)
                }
            }
            else -> Box(Modifier.fillMaxSize()) {
                SharedOfflineMapCanvas(sightings, onSelect, viewport)
                MapZoomControls(viewport, Modifier.align(Alignment.TopEnd).padding(10.dp))
            }
        }
    }

    selected?.let { sighting ->
        val freshness = FreshnessPolicy.evaluate(sighting)
        val locale = LocalConfiguration.current.locales[0]
        val sharedDate = remember(sighting.sharedAt, locale) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(sighting.sharedAt)
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f)),
            shadowElevation = 4.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (sighting.hasPhoto) {
                    val photo by produceState<ByteArray?>(null, sighting.id) {
                        value = runCatching { loadSharedPhoto(sighting.id) }.getOrNull()
                    }
                    photo?.let { SlidePhoto(it, Modifier.fillMaxWidth(.6f).align(Alignment.CenterHorizontally)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(sighting.type.sharedDisplayName(), fontWeight = FontWeight.Bold)
                    Text(freshness.level.displayName(), style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    stringResource(
                        R.string.map_zone_summary,
                        sighting.cellSizeMeters,
                        pluralStringResource(R.plurals.confirmation_count, sighting.confirmations, sighting.confirmations),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(R.string.map_shared_date, sharedDate),
                    style = MaterialTheme.typography.bodySmall,
                )
                when {
                    sighting.id in ownedIds -> Text(stringResource(R.string.map_published_by_you), color = MaterialTheme.colorScheme.primary)
                    sighting.id in confirmedIds -> Text(stringResource(R.string.map_already_confirmed), color = MaterialTheme.colorScheme.primary)
                    else -> Button(
                        enabled = confirmingSightingId != sighting.id,
                        onClick = { onConfirm(sighting) },
                    ) { Text(if (confirmingSightingId == sighting.id) stringResource(R.string.map_confirming) else stringResource(R.string.map_confirm_still_there)) }
                }
                confirmationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (sighting.goneCount > 0) {
                    Text(stringResource(R.string.map_gone_one), style = MaterialTheme.typography.bodySmall)
                }
                if (sighting.id !in ownedIds) {
                    if (sighting.id in goneIds) {
                        Text(stringResource(R.string.map_gone_sent), color = MaterialTheme.colorScheme.primary)
                    } else {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = markingGoneId != sighting.id,
                            onClick = { goneTarget = sighting },
                        ) {
                            Text(
                                if (markingGoneId == sighting.id) stringResource(R.string.map_marking_gone)
                                else stringResource(R.string.map_mark_gone),
                            )
                        }
                    }
                    markGoneError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (sighting.id in reportedIds) {
                        Text(stringResource(R.string.map_report_sent), color = MaterialTheme.colorScheme.primary)
                    } else {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = reportingSightingId != sighting.id,
                            onClick = { reportTarget = sighting },
                        ) {
                            Text(
                                if (reportingSightingId == sighting.id) stringResource(R.string.map_reporting)
                                else stringResource(R.string.map_report_pin),
                            )
                        }
                    }
                }
                reportError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    reportTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text(stringResource(R.string.map_report_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.map_report_explanation))
                    SightingReportReason.entries.forEach { reason ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onReport(target, reason)
                                reportTarget = null
                            },
                        ) { Text(reason.displayName()) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { reportTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    goneTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { goneTarget = null },
            title = { Text(stringResource(R.string.map_gone_title)) },
            text = { Text(stringResource(R.string.map_gone_explanation)) },
            confirmButton = {
                Button(onClick = {
                    onMarkGone(target)
                    goneTarget = null
                }) { Text(stringResource(R.string.map_gone_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { goneTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .28f)),
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.map_privacy_note), style = MaterialTheme.typography.bodySmall)
            if (geographicProvider != null) {
                Text(stringResource(R.string.map_online_privacy_note), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MapStatusOverlay(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .45f)),
        shadowElevation = 4.dp,
    ) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MapZoomControls(viewport: MapViewportState, modifier: Modifier = Modifier) {
    val zoomInDescription = stringResource(R.string.map_zoom_in)
    val zoomOutDescription = stringResource(R.string.map_zoom_out)
    val zoomResetDescription = stringResource(R.string.map_zoom_reset)
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .45f)),
        shadowElevation = 5.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { viewport.zoomBy(1.5f) }) {
                Text("＋", modifier = Modifier.semantics {
                    contentDescription = zoomInDescription
                })
            }
            Text("${"%.1f".format(viewport.zoom)}×", style = MaterialTheme.typography.labelMedium)
            IconButton(onClick = { viewport.zoomBy(1f / 1.5f) }) {
                Text("−", modifier = Modifier.semantics {
                    contentDescription = zoomOutDescription
                })
            }
            IconButton(onClick = viewport::reset) {
                Text("1×", modifier = Modifier.semantics {
                    contentDescription = zoomResetDescription
                })
            }
        }
    }
}

@Composable
private fun OfflineMapCanvas(findings: List<Finding>, onFindingClick: (String) -> Unit, viewport: MapViewportState) {
    val positions = MapProjection.project(findings.map { requireNotNull(it.location) })
    val tapRadius = with(LocalDensity.current) { 32.dp.toPx() }
    val clovers = findings.count { it.type == FindingType.FOUR_LEAF_CLOVER }
    val patches = findings.size - clovers
    val mapDescription = stringResource(R.string.map_private_accessibility, clovers, patches)
    val palette = LuckyTheme.colors
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyTheme.colors.mapPaper)
            .pointerInput(findings, positions) {
                detectTapGestures(
                    onDoubleTap = {
                        viewport.reset()
                    },
                    onTap = { tap ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val worldTap = Offset(
                        center.x + (tap.x - center.x - viewport.pan.x) / viewport.zoom,
                        center.y + (tap.y - center.y - viewport.pan.y) / viewport.zoom,
                    )
                    val index = positions.indices.minByOrNull { candidate ->
                        val marker = positions[candidate]
                        val dx = worldTap.x - size.width * marker.x
                        val dy = worldTap.y - size.height * marker.y
                        dx * dx + dy * dy
                    }
                    if (index != null) {
                        val marker = positions[index]
                        val dx = worldTap.x - size.width * marker.x
                        val dy = worldTap.y - size.height * marker.y
                        if (dx * dx + dy * dy <= tapRadius * tapRadius / (viewport.zoom * viewport.zoom)) {
                            onFindingClick(findings[index].id)
                        }
                    }
                },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    val nextZoom = (viewport.zoom * zoomChange).coerceIn(1f, 5f)
                    val maxX = size.width * (nextZoom - 1f) / 2f
                    val maxY = size.height * (nextZoom - 1f) / 2f
                    viewport.pan = Offset(
                        (viewport.pan.x + panChange.x).coerceIn(-maxX, maxX),
                        (viewport.pan.y + panChange.y).coerceIn(-maxY, maxY),
                    )
                    viewport.zoom = nextZoom
                }
            }
            .semantics {
                contentDescription = mapDescription
            },
    ) {
        val currentZoom = viewport.zoom
        val currentPan = viewport.pan
        withTransform({
            translate(currentPan.x, currentPan.y)
            scale(currentZoom, currentZoom, pivot = Offset(size.width / 2f, size.height / 2f))
        }) {
        val gridColor = palette.mapGrid
        for (step in 1..5) {
            val fraction = step / 6f
            drawLine(gridColor, Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height), 1.dp.toPx())
            drawLine(gridColor, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), 1.dp.toPx())
        }
        val trail = Path()
        trail.moveTo(0f, size.height * .72f)
        trail.cubicTo(size.width * .3f, size.height * .56f, size.width * .57f, size.height * .8f, size.width, size.height * .3f)
        drawPath(trail, palette.mapTrail, style = Stroke(5.dp.toPx()))

        findings.zip(positions).forEach { (finding, position) ->
            val center = Offset(size.width * position.x, size.height * position.y)
            val color = if (finding.type == FindingType.FOUR_LEAF_CLOVER) palette.mapClover else palette.mapPatch
            drawCircle(Color.White, 16.dp.toPx(), center)
            drawCircle(color, 12.dp.toPx(), center)
            if (finding.type == FindingType.FOUR_LEAF_CLOVER) {
                drawCircle(Color.White, 3.dp.toPx(), center)
            } else {
                drawCircle(Color.White, 8.dp.toPx(), center, style = Stroke(2.dp.toPx()))
            }
        }
        }
    }
}

@Composable
private fun SharedOfflineMapCanvas(sightings: List<SharedSighting>, onSightingClick: (String) -> Unit, viewport: MapViewportState) {
    val positions = MapProjection.project(sightings.map(SharedSighting::location))
    val tapRadius = with(LocalDensity.current) { 32.dp.toPx() }
    val fresh = sightings.count { FreshnessPolicy.evaluate(it).level == FreshnessLevel.FRESH }
    val mapDescription = stringResource(R.string.map_shared_accessibility, sightings.size, fresh)
    val palette = LuckyTheme.colors
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyTheme.colors.mapPaper)
            .pointerInput(sightings, positions) {
                detectTapGestures(
                    onDoubleTap = {
                        viewport.reset()
                    },
                    onTap = { tap ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val worldTap = Offset(
                        center.x + (tap.x - center.x - viewport.pan.x) / viewport.zoom,
                        center.y + (tap.y - center.y - viewport.pan.y) / viewport.zoom,
                    )
                    val index = positions.indices.minByOrNull { candidate ->
                        val marker = positions[candidate]
                        val dx = worldTap.x - size.width * marker.x
                        val dy = worldTap.y - size.height * marker.y
                        dx * dx + dy * dy
                    }
                    if (index != null) {
                        val marker = positions[index]
                        val dx = worldTap.x - size.width * marker.x
                        val dy = worldTap.y - size.height * marker.y
                        if (dx * dx + dy * dy <= tapRadius * tapRadius / (viewport.zoom * viewport.zoom)) {
                            onSightingClick(sightings[index].id)
                        }
                    }
                },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    val nextZoom = (viewport.zoom * zoomChange).coerceIn(1f, 5f)
                    val maxX = size.width * (nextZoom - 1f) / 2f
                    val maxY = size.height * (nextZoom - 1f) / 2f
                    viewport.pan = Offset(
                        (viewport.pan.x + panChange.x).coerceIn(-maxX, maxX),
                        (viewport.pan.y + panChange.y).coerceIn(-maxY, maxY),
                    )
                    viewport.zoom = nextZoom
                }
            }
            .semantics {
                contentDescription = mapDescription
            },
    ) {
        val currentZoom = viewport.zoom
        val currentPan = viewport.pan
        withTransform({
            translate(currentPan.x, currentPan.y)
            scale(currentZoom, currentZoom, pivot = Offset(size.width / 2f, size.height / 2f))
        }) {
        val gridColor = palette.mapGrid
        for (step in 1..5) {
            val fraction = step / 6f
            drawLine(gridColor, Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height), 1.dp.toPx())
            drawLine(gridColor, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), 1.dp.toPx())
        }

        sightings.zip(positions).forEach { (sighting, position) ->
            val center = Offset(size.width * position.x, size.height * position.y)
            val freshness = FreshnessPolicy.evaluate(sighting)
            val base = if (sighting.type == FindingType.FOUR_LEAF_CLOVER) palette.mapClover else palette.mapPatch
            val color = when (freshness.level) {
                FreshnessLevel.FRESH -> base
                FreshnessLevel.FADING -> palette.mapFading
                FreshnessLevel.STALE, FreshnessLevel.EXPIRED -> base.copy(alpha = .35f)
            }
            drawCircle(Color.White, 18.dp.toPx(), center)
            drawCircle(color, 13.dp.toPx(), center)
            drawCircle(Color.White, 7.dp.toPx(), center, style = Stroke(2.dp.toPx()))
        }
        }
    }
}

@Composable
private fun FindingType.sharedDisplayName() = when (this) {
    FindingType.FOUR_LEAF_CLOVER -> stringResource(R.string.shared_type_four_leaf)
    FindingType.CLOVER_PATCH -> stringResource(R.string.type_clover_patch)
}

@Composable
private fun FreshnessLevel.displayName() = when (this) {
    FreshnessLevel.FRESH -> stringResource(R.string.freshness_fresh)
    FreshnessLevel.FADING -> stringResource(R.string.freshness_fading)
    FreshnessLevel.STALE, FreshnessLevel.EXPIRED -> stringResource(R.string.freshness_stale)
}

@Composable
private fun SightingReportReason.displayName() = when (this) {
    SightingReportReason.NOT_RELEVANT -> stringResource(R.string.map_report_not_relevant)
    SightingReportReason.FALSE_INFORMATION -> stringResource(R.string.map_report_false_information)
    SightingReportReason.OFFENSIVE -> stringResource(R.string.map_report_offensive)
}

@Composable
private fun EmptyMap() {
    Box(Modifier.fillMaxSize().background(LuckyTheme.colors.mapPaper).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.lucky_prospector_mark),
                contentDescription = null,
                modifier = Modifier.size(150.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.map_empty_title), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.map_empty_description),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Data kept here only to make previews and the first navigable demo deterministic. */
fun demoMapFindings(): List<Finding> = listOf(
    Finding(type = FindingType.FOUR_LEAF_CLOVER, privateImageUri = "demo://clover-1", galleryImageUri = null, location = GeoPoint(45.4643, 9.1895, 5f), wish = "Una nuova avventura", prediction = null, createdAt = Instant.parse("2026-09-01T10:15:30Z")),
    Finding(type = FindingType.CLOVER_PATCH, privateImageUri = "demo://patch-1", galleryImageUri = null, location = GeoPoint(45.4680, 9.1810, 8f), wish = null, prediction = null, createdAt = Instant.parse("2026-09-02T14:00:00Z")),
    Finding(type = FindingType.FOUR_LEAF_CLOVER, privateImageUri = "demo://clover-2", galleryImageUri = null, location = GeoPoint(45.4590, 9.1960, 4f), wish = null, prediction = null, createdAt = Instant.parse("2026-09-03T09:00:00Z")),
)

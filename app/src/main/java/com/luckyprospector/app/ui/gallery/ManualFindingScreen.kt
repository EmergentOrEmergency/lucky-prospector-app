package com.luckyprospector.app.ui.gallery

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.luckyprospector.app.R
import com.luckyprospector.app.camera.FixedPhotoPreview
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.GeoPoint
import com.luckyprospector.app.location.AddressResult
import com.luckyprospector.app.location.AddressSearch
import com.luckyprospector.app.location.AndroidLocationProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

private enum class PlaceSource { PHOTO, CURRENT, ADDRESS, NONE }

/**
 * Adds a finding by hand: a photo from the gallery, and the place typed as an
 * address or taken from the photo itself, for clovers found before the app or
 * spotted in a park to come back to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualFindingScreen(
    padding: PaddingValues,
    photoLocation: suspend (Uri) -> GeoPoint?,
    onSave: suspend (photo: Uri, type: FindingType, wish: String?, date: Instant, location: GeoPoint?) -> Result<Unit>,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val addressSearch = remember { AddressSearch(context) }
    val locationProvider = remember { AndroidLocationProvider(context) }

    var photo by rememberSaveable { mutableStateOf<Uri?>(null) }
    var type by rememberSaveable { mutableStateOf(FindingType.FOUR_LEAF_CLOVER) }
    var wish by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var photoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var source by rememberSaveable { mutableStateOf(PlaceSource.NONE) }
    var currentPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var locating by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<AddressResult>>(emptyList()) }
    var chosenAddress by remember { mutableStateOf<AddressResult?>(null) }
    var searchedOnce by remember { mutableStateOf(false) }

    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val locationError = stringResource(R.string.error_location)
    val saveError = stringResource(R.string.error_save)

    fun searchAddress() {
        if (query.isBlank() || searching) return
        searching = true
        scope.launch {
            results = runCatching { addressSearch.search(query) }.getOrDefault(emptyList())
            chosenAddress = results.firstOrNull()
            searchedOnce = true
            searching = false
        }
    }

    fun locate() {
        locating = true
        scope.launch {
            currentPoint = runCatching { locationProvider.currentLocation() }.getOrNull()
            if (currentPoint == null) error = locationError
            locating = false
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (locationProvider.hasPermission()) locate() else error = locationError
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photo = uri
            scope.launch {
                photoPoint = photoLocation(uri)
                // A position recorded by the camera is the most faithful place.
                if (photoPoint != null) source = PlaceSource.PHOTO
            }
        }
    }

    val chosenPoint = when (source) {
        PlaceSource.PHOTO -> photoPoint
        PlaceSource.CURRENT -> currentPoint
        PlaceSource.ADDRESS -> chosenAddress?.point
        PlaceSource.NONE -> null
    }
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = remember(date, locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date) }

    Column(
        Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.manual_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        FixedPhotoPreview(photo, height = 220.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !saving,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text(stringResource(if (photo == null) R.string.manual_pick_photo else R.string.manual_change_photo)) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(type == FindingType.FOUR_LEAF_CLOVER, { type = FindingType.FOUR_LEAF_CLOVER }, { Text(stringResource(R.string.type_four_leaf_clover)) })
            FilterChip(type == FindingType.CLOVER_PATCH, { type = FindingType.CLOVER_PATCH }, { Text(stringResource(R.string.type_patch_short)) })
        }
        OutlinedTextField(
            value = wish,
            onValueChange = { wish = it.take(500) },
            label = { Text(stringResource(R.string.wish_short_optional)) },
            minLines = 2,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = { showDatePicker = true }) { Text(stringResource(R.string.manual_date, dateLabel)) }

        Text(stringResource(R.string.manual_location_title), fontWeight = FontWeight.SemiBold)
        if (photoPoint != null) PlaceOption(stringResource(R.string.location_from_photo), source == PlaceSource.PHOTO) { source = PlaceSource.PHOTO }
        PlaceOption(stringResource(R.string.location_current), source == PlaceSource.CURRENT) {
            source = PlaceSource.CURRENT
            error = null
            if (currentPoint == null && !locating) {
                if (locationProvider.hasPermission()) locate()
                else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
        if (source == PlaceSource.CURRENT && locating) Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.locating), Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodySmall)
        }
        if (addressSearch.available) {
            PlaceOption(stringResource(R.string.location_address), source == PlaceSource.ADDRESS) { source = PlaceSource.ADDRESS }
            if (source == PlaceSource.ADDRESS) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(200) },
                        label = { Text(stringResource(R.string.address_hint)) },
                        singleLine = true,
                        // The keyboard's own key searches, so the keyboard need not be closed first.
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { searchAddress() }),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        enabled = query.isNotBlank() && !searching,
                        onClick = ::searchAddress,
                    ) { Text(stringResource(if (searching) R.string.address_searching else R.string.address_search)) }
                }
                if (searchedOnce && results.isEmpty()) Text(stringResource(R.string.address_no_results), style = MaterialTheme.typography.bodySmall)
                results.forEach { result ->
                    PlaceOption(result.label, chosenAddress == result) { chosenAddress = result }
                }
                Text(stringResource(R.string.address_privacy), style = MaterialTheme.typography.bodySmall)
            }
        }
        PlaceOption(stringResource(R.string.location_none), source == PlaceSource.NONE) { source = PlaceSource.NONE }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
            OutlinedButton(onClick = onCancel, enabled = !saving) { Text(stringResource(R.string.cancel)) }
            Button(
                enabled = photo != null && !saving,
                onClick = {
                    val picked = photo ?: return@Button
                    if (saving) return@Button
                    saving = true
                    error = null
                    scope.launch {
                        val instant = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()
                        onSave(picked, type, wish.trim().ifEmpty { null }, instant, chosenPoint)
                            .onSuccess { onDone() }
                            .onFailure { error = saveError }
                        saving = false
                    }
                },
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.saving), Modifier.padding(start = 10.dp))
                } else {
                    Text(stringResource(R.string.search_save_finding))
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun PlaceOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

package com.luckyprospector.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luckyprospector.app.R
import com.luckyprospector.app.domain.SearchResolution
import com.luckyprospector.app.domain.SearchSensitivity

@Composable
fun SettingsScreen(padding: PaddingValues, onBack: () -> Unit) {
    val store = AppSettings.get(LocalContext.current)
    val settings by store.settings.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        SettingsCard(stringResource(R.string.settings_find_title)) {
            ToggleRow(stringResource(R.string.settings_sound), stringResource(R.string.settings_sound_hint), settings.sound) { on ->
                store.update { it.copy(sound = on) }
            }
            ToggleRow(stringResource(R.string.settings_vibration), stringResource(R.string.settings_vibration_hint), settings.vibration) { on ->
                store.update { it.copy(vibration = on) }
            }
        }

        SettingsCard(stringResource(R.string.settings_sensitivity_title)) {
            Text(stringResource(R.string.settings_sensitivity_intro), style = MaterialTheme.typography.bodySmall)
            SearchSensitivity.entries.forEach { level ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { store.update { it.copy(sensitivity = level) } }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.sensitivity == level, onClick = null)
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(level.label(), fontWeight = FontWeight.SemiBold)
                        Text(level.description(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        SettingsCard(stringResource(R.string.settings_resolution_title)) {
            Text(stringResource(R.string.settings_resolution_intro), style = MaterialTheme.typography.bodySmall)
            SearchResolution.entries.forEach { level ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { store.update { it.copy(resolution = level) } }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.resolution == level, onClick = null)
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(
                            if (level == SearchResolution.CAMERA_MAX) level.label()
                            else "${level.label()} · ${level.width}×${level.height}",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(level.description(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .35f)),
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun ToggleRow(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(hint, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SearchSensitivity.label() = stringResource(
    when (this) {
        SearchSensitivity.CAREFUL -> R.string.sensitivity_careful
        SearchSensitivity.NORMAL -> R.string.sensitivity_normal
        SearchSensitivity.SENSITIVE -> R.string.sensitivity_sensitive
    },
)

@Composable
private fun SearchSensitivity.description() = stringResource(
    when (this) {
        SearchSensitivity.CAREFUL -> R.string.sensitivity_careful_hint
        SearchSensitivity.NORMAL -> R.string.sensitivity_normal_hint
        SearchSensitivity.SENSITIVE -> R.string.sensitivity_sensitive_hint
    },
)

@Composable
private fun SearchResolution.label() = stringResource(
    when (this) {
        SearchResolution.STANDARD -> R.string.resolution_standard
        SearchResolution.HIGH -> R.string.resolution_high
        SearchResolution.FULL_HD -> R.string.resolution_full_hd
        SearchResolution.MAX -> R.string.resolution_max
        SearchResolution.CAMERA_MAX -> R.string.resolution_camera_max
    },
)

@Composable
private fun SearchResolution.description() = stringResource(
    when (this) {
        SearchResolution.STANDARD -> R.string.resolution_standard_hint
        SearchResolution.HIGH -> R.string.resolution_high_hint
        SearchResolution.FULL_HD -> R.string.resolution_full_hd_hint
        SearchResolution.MAX -> R.string.resolution_max_hint
        SearchResolution.CAMERA_MAX -> R.string.resolution_camera_max_hint
    },
)

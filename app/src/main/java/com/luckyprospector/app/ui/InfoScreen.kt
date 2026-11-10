package com.luckyprospector.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luckyprospector.app.R
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Attributions shown verbatim: names of works, authors and licences are not
 * translated. Model sources and individual CC0/CC BY photo credits retain their licences.
 */
private val ATTRIBUTIONS = listOf(
    "“Hunting for four-leaf clovers Dataset”, Adam Fonagy, Roboflow Universe, 2023 — CC BY 4.0",
    "“4 leaf clover detect Dataset”, test, Roboflow Universe, 2024 — CC BY 4.0",
    "“clover Object Detection Dataset”, Clover Detection, Roboflow Universe, 2022 — CC BY 4.0",
    "LiteRT (TensorFlow Lite) — Apache License 2.0",
    "MapLibre Native — BSD 2-Clause License",
    "Map data © OpenStreetMap contributors (ODbL); tiles by OpenFreeMap, style based on OpenMapTiles",
    "Firebase — Google LLC",
)

@Composable
fun InfoScreen(
    padding: PaddingValues,
    signedIn: Boolean,
    onFeedback: () -> Unit,
    /** Withdraws the account's pins, then deletes the account. */
    onDeleteAccount: suspend () -> Result<Unit>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val policyUrl = stringResource(R.string.privacy_policy_url)
    val supportEmail = stringResource(R.string.support_email)
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var deleted by remember { mutableStateOf(false) }
    var showModelCredits by remember { mutableStateOf(false) }
    val modelInfo = remember {
        runCatching {
            val metadata = context.assets.open("models/lucky-prospector/model-metadata.json")
                .bufferedReader().use { JSONObject(it.readText()) }
            val provenance = context.assets.open("models/lucky-prospector/dataset-provenance.json")
                .bufferedReader().use { JSONObject(it.readText()) }
            val credits = provenance.optJSONArray("image_attributions")
            val text = (0 until (credits?.length() ?: 0)).map { i ->
                val item = credits!!.getJSONObject(i)
                listOf(
                    item.optString("candidate_id"),
                    item.optString("attribution").ifBlank { item.optString("creator") },
                    item.optString("license"),
                    item.optString("license_url"),
                    item.optString("photo_page_url").ifBlank { item.optString("source_url") },
                ).filter { it.isNotBlank() }.joinToString("\n")
            }
            metadata.getString("model_version") to text
        }.getOrDefault("?" to emptyList<String>())
    }
    val scope = rememberCoroutineScope()
    val deleteErrorMessage = stringResource(R.string.error_delete_account)

    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.info_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Section(stringResource(R.string.info_privacy_title)) {
            Text(stringResource(R.string.info_privacy_summary), style = MaterialTheme.typography.bodyMedium)
            if (policyUrl.isNotBlank()) {
                OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(policyUrl))) }) {
                    Text(stringResource(R.string.info_privacy_full))
                }
            }
        }

        Section(stringResource(R.string.info_contacts_title)) {
            Text(stringResource(R.string.info_contacts_text), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onFeedback) { Text(stringResource(R.string.feedback_button)) }
            if (supportEmail.isNotBlank()) {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$supportEmail")))
                }) { Text(stringResource(R.string.info_contact_email, supportEmail)) }
            }
        }

        Section(stringResource(R.string.info_licenses_title)) {
            Text(
                "Clover detector flc-yolox-tiny-v${modelInfo.first} — Lucky Prospector / YOLOX (Apache License 2.0)",
                style = MaterialTheme.typography.bodySmall,
            )
            ATTRIBUTIONS.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            if (modelInfo.second.isNotEmpty()) {
                TextButton(onClick = { showModelCredits = true }) {
                    Text(stringResource(R.string.info_model_photo_credits))
                }
            }
        }

        if (signedIn || deleted) {
            Section(stringResource(R.string.delete_account_title)) {
                if (deleted) {
                    Text(stringResource(R.string.account_deleted), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(stringResource(R.string.delete_account_text), style = MaterialTheme.typography.bodyMedium)
                    Button(
                        enabled = !deleting,
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) { Text(stringResource(if (deleting) R.string.deleting_account else R.string.delete_account_button)) }
                    deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        Text(
            "${stringResource(R.string.app_name)} · ${stringResource(R.string.info_version, version)}",
            style = MaterialTheme.typography.labelSmall,
        )
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }

    if (showModelCredits) {
        AlertDialog(
            onDismissRequest = { showModelCredits = false },
            title = { Text(stringResource(R.string.info_model_photo_credits)) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(modelInfo.second) { credit ->
                        Text(credit, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelCredits = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = { Text(stringResource(R.string.delete_account_confirm_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        deleting = true
                        deleteError = null
                        scope.launch {
                            onDeleteAccount()
                                .onSuccess { deleted = true }
                                .onFailure { deleteError = deleteErrorMessage }
                            deleting = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text(stringResource(R.string.delete_account_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .35f)),
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

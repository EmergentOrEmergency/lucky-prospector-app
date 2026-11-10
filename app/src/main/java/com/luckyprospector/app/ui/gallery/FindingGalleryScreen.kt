package com.luckyprospector.app.ui.gallery

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalConfiguration
import com.luckyprospector.app.R
import com.luckyprospector.app.domain.Finding
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.SyncState
import com.luckyprospector.app.ui.components.DidYouKnowCard
import com.luckyprospector.app.ui.components.Polaroid
import com.luckyprospector.app.ui.components.polaroidTilt
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun FindingGalleryScreen(
    findings: List<Finding>,
    onFindingClick: (String) -> Unit,
    onStartSearch: () -> Unit = {},
    onAddManually: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (findings.isEmpty()) {
        Column(
            modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.lucky_prospector_mark),
                contentDescription = null,
                modifier = Modifier.size(190.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                stringResource(R.string.gallery_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.gallery_empty_description),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Surface(
                modifier = Modifier.padding(vertical = 18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.private_by_default), style = MaterialTheme.typography.labelLarge)
                }
            }
            Button(onClick = onStartSearch) { Text(stringResource(R.string.start_search)) }
            OutlinedButton(onClick = onAddManually, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.add_manually)) }
            DidYouKnowCard(Modifier.padding(top = 24.dp))
        }
        return
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(stringResource(R.string.gallery_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    pluralStringResource(R.plurals.finding_count, findings.size, findings.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onAddManually, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.add_manually)) }
            }
        }
        item { DidYouKnowCard(Modifier.padding(horizontal = 16.dp)) }
        itemsIndexed(findings, key = { _, finding -> finding.id }) { index, finding ->
            FindingCard(finding, index, { onFindingClick(finding.id) }, Modifier.padding(horizontal = 30.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun FindingCard(finding: Finding, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val date = formatter.format(finding.createdAt.atZone(ZoneId.systemDefault()))
    val wish = finding.wish?.takeIf(String::isNotBlank)
    // The wish is the handwritten line when there is one, as on a real print;
    // otherwise the caption says what was found.
    Polaroid(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        caption = wish ?: finding.type.displayName(),
        subtitle = if (wish != null) "${finding.type.displayName()} · $date" else date,
        tilt = polaroidTilt(index),
        badge = {
            Surface(
                color = if (finding.syncState == SyncState.SYNCED) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (finding.syncState == SyncState.SYNCED) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(50),
                shadowElevation = 4.dp,
            ) {
                Text(
                    finding.syncState.displayName(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
    ) { photoModifier -> FindingPhoto(finding.privateImageUri, photoModifier) }
}

@Composable
fun FindingPhoto(uri: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { it.setImageURI(Uri.parse(uri)) },
        modifier = modifier,
    )
}

@Composable
internal fun FindingType.displayName() = when (this) {
    FindingType.FOUR_LEAF_CLOVER -> stringResource(R.string.type_four_leaf_clover)
    FindingType.CLOVER_PATCH -> stringResource(R.string.type_clover_patch)
}

@Composable
private fun SyncState.displayName() = when (this) {
    SyncState.LOCAL_ONLY -> stringResource(R.string.sync_local_only)
    SyncState.PENDING -> stringResource(R.string.sync_pending)
    SyncState.SYNCED -> stringResource(R.string.sync_synced)
    SyncState.FAILED -> stringResource(R.string.sync_failed)
}

package com.luckyprospector.app.ui.gallery

import com.luckyprospector.app.ui.AppSettings
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.luckyprospector.app.R
import com.luckyprospector.app.domain.Finding
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.PublishLimitReachedException
import com.luckyprospector.app.domain.SyncState
import com.luckyprospector.app.sharing.ShareCard
import com.luckyprospector.app.sharing.SharePreparation
import com.luckyprospector.app.ui.SharedTypeNotConfirmedException
import com.luckyprospector.app.ui.components.Polaroid
import com.luckyprospector.app.ui.components.PreservationDialog
import com.luckyprospector.app.ui.components.SlidePhoto
import com.luckyprospector.app.ui.theme.LuckyTheme
import com.luckyprospector.app.ui.theme.luckyScreenBrush
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindingDetailScreen(
    finding: Finding,
    onBack: () -> Unit,
    onSave: suspend (Finding) -> Result<Unit>,
    onDelete: suspend (String) -> Result<Unit>,
    isSignedIn: Boolean,
    onSignInRequested: () -> Unit,
    /** Runs the classifier and builds the crop; nothing is uploaded. */
    onPrepareShare: suspend () -> SharePreparation,
    onShare: suspend (SharePreparation.Ready, includePhoto: Boolean) -> Result<Unit>,
    onUnshare: suspend () -> Result<Unit>,
    modifier: Modifier = Modifier,
) {
    var wish by remember(finding.id) { mutableStateOf(finding.wish.orEmpty()) }
    var type by remember(finding.id) { mutableStateOf(finding.type) }
    var sharing by remember(finding.id) { mutableStateOf(false) }
    var sharingError by remember(finding.id) { mutableStateOf<String?>(null) }
    var saving by remember(finding.id) { mutableStateOf(false) }
    var saveError by remember(finding.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val operationErrorMessage = stringResource(R.string.error_operation)
    val saveErrorMessage = stringResource(R.string.error_save)
    val publishLimitMessage = stringResource(R.string.error_publish_limit)
    val noCloverMessage = stringResource(R.string.error_share_no_clover)
    val modelUnavailableMessage = stringResource(R.string.error_share_model_unavailable)
    val photoUnreadableMessage = stringResource(R.string.error_share_photo_unreadable)
    val typeNotConfirmedMessage = stringResource(R.string.error_type_not_confirmed)
    var review by remember(finding.id) { mutableStateOf<SharePreparation.Ready?>(null) }
    var showPreservation by remember { mutableStateOf(false) }
    var preparingCard by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val wishes = stringArrayResource(R.array.lucky_wishes)
    val cardFooter = stringResource(R.string.share_card_footer)
    val chooserTitle = stringResource(R.string.share_chooser_title)
    val cardError = stringResource(R.string.error_share_card)
    // An invitation to join the search, with the store link; nothing about the place.
    val shareText = stringResource(
        if (type == FindingType.FOUR_LEAF_CLOVER) R.string.share_text_four_leaf else R.string.share_text_patch,
        stringResource(R.string.app_name),
        stringResource(R.string.store_url),
    )
    fun shareFailure(failure: Throwable) =
        if (failure is PublishLimitReachedException) publishLimitMessage else operationErrorMessage
    Box(modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            // A transparent container gives no content colour of its own, so text
            // would fall back to black, invisible on the dark-mode background.
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = { Text(type.displayName(), fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { padding ->
            Box(
                Modifier.fillMaxSize().background(luckyScreenBrush()),
            ) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
            Polaroid(
                modifier = Modifier.fillMaxWidth(.86f).align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
                caption = wish.trim().ifEmpty { null } ?: type.displayName(),
                tilt = -1.4f,
            ) { photoModifier -> FindingPhoto(finding.privateImageUri, photoModifier) }
            OutlinedTextField(
                value = wish,
                onValueChange = { wish = it },
                label = { Text(stringResource(R.string.wish_optional)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.classification), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FindingType.entries.forEach { candidate ->
                    val selected = type == candidate
                    FilterChip(
                        selected = selected,
                        onClick = { type = candidate },
                        label = { Text(candidate.displayName()) },
                        leadingIcon = if (selected) ({ Text("✓") }) else null,
                        shape = RoundedCornerShape(18.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .78f),
                            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .78f),
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedLabelColor = LuckyTheme.colors.onGold,
                            selectedLeadingIconColor = LuckyTheme.colors.onGold,
                        ),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !preparingCard,
                    onClick = {
                        preparingCard = true
                        scope.launch {
                            runCatching {
                                val card = ShareCard.render(context, finding.privateImageUri, wishes.random(), cardFooter)
                                context.startActivity(ShareCard.shareIntent(context, card, shareText, chooserTitle))
                            }.onFailure { saveError = cardError }
                            preparingCard = false
                        }
                    },
                ) { Text(stringResource(R.string.share_with_friends)) }
                if (type == FindingType.FOUR_LEAF_CLOVER) {
                    OutlinedButton(onClick = { showPreservation = true }) { Text(stringResource(R.string.preserve_button)) }
                }
            }
            Text(stringResource(R.string.shared_map), fontWeight = FontWeight.SemiBold)
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .9f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .3f)),
                shadowElevation = 4.dp,
            ) {
                Text(
                    stringResource(R.string.sharing_privacy_detail),
                    modifier = Modifier.padding(14.dp),
                )
            }
            if (finding.location == null) {
                Text(stringResource(R.string.finding_without_location))
            } else if (!isSignedIn) {
                OutlinedButton(onClick = onSignInRequested) { Text(stringResource(R.string.sign_in_to_share)) }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        when (finding.syncState) {
                            SyncState.LOCAL_ONLY -> stringResource(R.string.sharing_private)
                            SyncState.PENDING -> stringResource(R.string.sharing_pending)
                            SyncState.SYNCED -> stringResource(R.string.sharing_anonymous)
                            SyncState.FAILED -> stringResource(R.string.sharing_failed)
                        },
                    )
                    Switch(
                        checked = finding.syncState == SyncState.SYNCED,
                        enabled = !sharing && finding.syncState != SyncState.PENDING,
                        onCheckedChange = { shouldShare ->
                            sharing = true
                            sharingError = null
                            scope.launch {
                                if (!shouldShare) {
                                    onUnshare().onFailure { sharingError = shareFailure(it) }
                                } else {
                                    // Nothing is published yet: the user first sees what
                                    // the classifier allows and exactly which image would go.
                                    when (val prepared = onPrepareShare()) {
                                        is SharePreparation.Ready -> review = prepared
                                        SharePreparation.NoClover -> sharingError = noCloverMessage
                                        SharePreparation.ModelUnavailable -> sharingError = modelUnavailableMessage
                                        SharePreparation.PhotoUnreadable -> sharingError = photoUnreadableMessage
                                    }
                                }
                                sharing = false
                            }
                        },
                    )
                }
                if (sharing) Text(stringResource(R.string.share_checking), style = MaterialTheme.typography.bodySmall)
            }
            sharingError?.let { Text(it) }
            saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !saving,
                    onClick = {
                        val correction = if (type != finding.type) type.name else finding.prediction?.correctedClass
                        val updated = finding.copy(
                            type = type,
                            wish = wish.trim().ifEmpty { null },
                            prediction = finding.prediction?.copy(correctedClass = correction),
                        )
                        saving = true
                        saveError = null
                        scope.launch {
                            onSave(updated)
                                .onSuccess { onBack() }
                                .onFailure {
                                    saveError = if (it is SharedTypeNotConfirmedException) typeNotConfirmedMessage else saveErrorMessage
                                }
                            saving = false
                        }
                    },
                ) { Text(if (saving) stringResource(R.string.saving) else stringResource(R.string.save)) }
                OutlinedButton(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        saveError = null
                        scope.launch {
                            // A failed withdrawal keeps the finding; say so instead of doing nothing.
                            onDelete(finding.id).onFailure { saveError = operationErrorMessage }
                            saving = false
                        }
                    },
                ) { Text(stringResource(R.string.delete)) }
                OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                }
            }
        }
        }

        if (showPreservation) PreservationDialog(onDismiss = { showPreservation = false })

        review?.let { prepared ->
            ShareReviewDialog(
                prepared = prepared,
                onDismiss = { review = null },
                onConfirm = { includePhoto ->
                    review = null
                    sharing = true
                    scope.launch {
                        onShare(prepared, includePhoto).onFailure { sharingError = shareFailure(it) }
                        sharing = false
                    }
                },
            )
        }

        val systemBarGradient = LuckyTheme.colors.barBrush
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(systemBarGradient),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(systemBarGradient),
        )
    }
}

/**
 * Shows exactly what would leave the phone: the classifier's type and the cropped,
 * EXIF-free image. The user decides with or without the photo, or not at all.
 *
 * The first time, the shared map's rules must be accepted before anything can be
 * published: Play requires apps with user content to state what is not allowed.
 */
@Composable
private fun ShareReviewDialog(
    prepared: SharePreparation.Ready,
    onDismiss: () -> Unit,
    onConfirm: (includePhoto: Boolean) -> Unit,
) {
    val store = AppSettings.get(LocalContext.current)
    val settings by store.settings.collectAsState()
    val rulesPending = !settings.communityRulesAccepted
    var rulesChecked by remember { mutableStateOf(false) }
    val canPublish = !rulesPending || rulesChecked
    fun confirm(includePhoto: Boolean) {
        if (rulesPending) store.update { it.copy(communityRulesAccepted = true) }
        onConfirm(includePhoto)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_review_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SlidePhoto(prepared.jpeg, Modifier.fillMaxWidth(.7f).align(Alignment.CenterHorizontally))
                Text(
                    stringResource(
                        R.string.share_review_type,
                        if (prepared.type == FindingType.FOUR_LEAF_CLOVER) stringResource(R.string.type_four_leaf_clover)
                        else stringResource(R.string.type_clover_patch),
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
                if (prepared.downgraded) Text(stringResource(R.string.share_downgraded), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.share_review_text), style = MaterialTheme.typography.bodySmall)
                if (rulesPending) {
                    Text(stringResource(R.string.community_rules_title), fontWeight = FontWeight.SemiBold)
                    stringArrayResource(R.array.community_rules).forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { rulesChecked = !rulesChecked },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = rulesChecked, onCheckedChange = { rulesChecked = it })
                        Text(stringResource(R.string.community_rules_accept), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = { confirm(true) }, enabled = canPublish) { Text(stringResource(R.string.share_with_photo)) }
                TextButton(onClick = { confirm(false) }, enabled = canPublish) { Text(stringResource(R.string.share_without_photo)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

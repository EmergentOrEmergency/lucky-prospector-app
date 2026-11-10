package com.luckyprospector.app.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.luckyprospector.app.R
import com.luckyprospector.app.auth.AuthProvider
import com.luckyprospector.app.auth.AuthResult
import com.luckyprospector.app.auth.FirebaseAuthProvider
import com.luckyprospector.app.auth.uidChanges
import com.luckyprospector.app.camera.SearchScreen
import com.luckyprospector.app.data.local.FindingPhotoStore
import com.luckyprospector.app.data.local.LuckyProspectorDatabase
import com.luckyprospector.app.data.local.RoomFindingRepository
import com.luckyprospector.app.data.remote.FeedbackRepository
import com.luckyprospector.app.data.remote.FirestoreSharedSightingRepository
import com.luckyprospector.app.map.MapScreen
import com.luckyprospector.app.sharing.SharedPhotoPreparer
import com.luckyprospector.app.ui.gallery.FindingDetailScreen
import com.luckyprospector.app.ui.gallery.FindingGalleryScreen
import com.luckyprospector.app.ui.gallery.ManualFindingScreen
import com.luckyprospector.app.ui.components.HandwritingFont
import com.luckyprospector.app.ui.theme.LuckyTheme
import com.luckyprospector.app.ui.theme.luckyScreenBrush
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class Destination(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    SEARCH(R.string.nav_search, R.drawable.ic_lucky_search),
    GALLERY(R.string.nav_findings, R.drawable.ic_lucky_journal),
    MAP(R.string.nav_map, R.drawable.ic_lucky_map),
    PROFILE(R.string.nav_profile, R.drawable.ic_lucky_profile),
}

@Composable
fun LuckyProspectorApp() {
    val application = LocalContext.current.applicationContext as Application
    val activity = checkNotNull(LocalActivity.current) {
        "LuckyProspectorApp requires an Activity context for Google sign-in"
    }
    // The account picker needs the Activity, so the provider lives with it; the
    // ViewModels follow the account through FirebaseAuth directly.
    val authProvider = remember(activity) { FirebaseAuthProvider(activity) }
    val authUser by authProvider.currentUser.collectAsState()

    val detectorViewModel: DetectorViewModel = viewModel()
    val findingsViewModel: FindingsViewModel = viewModel {
        FindingsViewModel(
            repository = RoomFindingRepository(LuckyProspectorDatabase.getInstance(application).findingDao()),
            photoStore = FindingPhotoStore(application),
            sharedRepository = FirestoreSharedSightingRepository(),
            photoPreparer = SharedPhotoPreparer(application),
            // Only the real model may vouch for what is shared: never the demo detector.
            // Both ViewModels live in the same store, so neither outlives the other.
            detector = {
                detectorViewModel.state.filterNotNull().first().takeUnless { it.modelUnavailable }?.detector
            },
            uid = FirebaseAuth.getInstance().uidChanges(),
        )
    }
    val sharedMapViewModel: SharedMapViewModel = viewModel {
        SharedMapViewModel(FirestoreSharedSightingRepository(), FirebaseAuth.getInstance().uidChanges())
    }

    val detectorState by detectorViewModel.state.collectAsStateWithLifecycle()
    val findings by findingsViewModel.findings.collectAsStateWithLifecycle()
    val sharedMap by sharedMapViewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Saveable: switching language recreates the Activity, as do rotation and process death.
    var destination by rememberSaveable { mutableStateOf(Destination.SEARCH) }
    var selectedFindingId by rememberSaveable { mutableStateOf<String?>(null) }
    var addingManually by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val saveErrorMessage = stringResource(R.string.error_save)

    val selectedFinding = findings.firstOrNull { it.id == selectedFindingId }
    // Without these the system back gesture closes the app from any screen.
    BackHandler(enabled = selectedFinding != null) { selectedFindingId = null }
    BackHandler(enabled = addingManually) { addingManually = false }
    BackHandler(enabled = selectedFinding == null && !addingManually && destination != Destination.SEARCH) {
        destination = Destination.SEARCH
    }
    if (selectedFinding != null) {
        FindingDetailScreen(
            finding = selectedFinding,
            onBack = { selectedFindingId = null },
            onSave = { updated -> findingsViewModel.save(selectedFinding, updated) },
            onDelete = {
                findingsViewModel.delete(selectedFinding).onSuccess { selectedFindingId = null }
            },
            isSignedIn = authUser != null,
            onSignInRequested = {
                selectedFindingId = null
                destination = Destination.PROFILE
            },
            onPrepareShare = { findingsViewModel.prepareShare(selectedFinding) },
            onShare = { prepared, includePhoto -> findingsViewModel.share(selectedFinding, prepared, includePhoto) },
            onUnshare = { findingsViewModel.unshare(selectedFinding) },
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            // A transparent container gives no content colour of its own, so text
            // would fall back to black, invisible on the dark-mode background.
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                LuckyNavigationBar(destination) { destination = it }
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(luckyScreenBrush()),
            ) {
                when (destination) {
                    Destination.SEARCH -> {
                        val loaded = detectorState
                        if (loaded == null) {
                            DetectorLoading(Modifier.padding(padding))
                        } else {
                            SearchScreen(
                                modifier = Modifier.padding(padding),
                                detector = loaded.detector,
                                detectorLoadError = if (loaded.modelUnavailable) stringResource(R.string.search_model_unavailable) else null,
                                onCaptureConfirmed = { draft ->
                                    findingsViewModel.saveCapture(draft)
                                        .onSuccess { destination = Destination.GALLERY }
                                        // An uncaught failure here used to crash the whole app.
                                        .onFailure { scope.launch { snackbarHostState.showSnackbar(saveErrorMessage) } }
                                },
                            )
                        }
                    }
                    Destination.GALLERY -> if (addingManually) ManualFindingScreen(
                        padding = padding,
                        photoLocation = findingsViewModel::photoLocation,
                        onSave = findingsViewModel::addManual,
                        onDone = { addingManually = false },
                        onCancel = { addingManually = false },
                    ) else FindingGalleryScreen(
                        findings = findings,
                        onFindingClick = { selectedFindingId = it },
                        onStartSearch = { destination = Destination.SEARCH },
                        onAddManually = { addingManually = true },
                        modifier = Modifier.padding(padding),
                    )
                    Destination.MAP -> MapScreen(
                        findings = findings,
                        modifier = Modifier.padding(padding),
                        onFindingClick = { selectedFindingId = it },
                        sharedSightings = sharedMap.sightings,
                        ownedSharedIds = sharedMap.ownedIds,
                        confirmedSharedIds = sharedMap.confirmedIds,
                        reportedSharedIds = sharedMap.reportedIds,
                        goneSharedIds = sharedMap.goneIds,
                        isSignedIn = authUser != null,
                        sharedLoading = sharedMap.loading,
                        sharedError = if (sharedMap.unavailable) stringResource(R.string.error_shared_map_update) else null,
                        confirmingSightingId = sharedMap.confirmingId,
                        confirmationError = if (sharedMap.confirmationFailed) stringResource(R.string.error_confirmation) else null,
                        reportingSightingId = sharedMap.reportingId,
                        reportError = if (sharedMap.reportFailed) stringResource(R.string.error_report) else null,
                        markingGoneId = sharedMap.markingGoneId,
                        markGoneError = if (sharedMap.markGoneFailed) stringResource(R.string.error_mark_gone) else null,
                        onSignInRequested = { destination = Destination.PROFILE },
                        onConfirmShared = sharedMapViewModel::confirm,
                        onReportShared = sharedMapViewModel::report,
                        onMarkGone = sharedMapViewModel::markGone,
                        onVisibleAreaChange = sharedMapViewModel::setVisibleArea,
                        loadSharedPhoto = sharedMapViewModel::photo,
                    )
                    Destination.PROFILE -> ProfileScreen(authProvider, padding, findingsViewModel::leaveSharedMap)
                }
            }
        }

        // Android 15+ makes the status bar transparent. Draw our own scrim in
        // the exact inset so the clock and system icons sit on forest green.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(LuckyTheme.colors.barBrush),
        )
    }
}

@Composable
private fun DetectorLoading(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.camera_preparing), modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun LuckyNavigationBar(selected: Destination, onSelected: (Destination) -> Unit) {
    val colors = LuckyTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.barSolid,
        shadowElevation = 12.dp,
    ) {
        Box(Modifier.background(colors.barBrush)) {
            NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                Destination.entries.forEach { item ->
                    val label = stringResource(item.labelRes)
                    NavigationBarItem(
                        selected = selected == item,
                        onClick = { onSelected(item) },
                        icon = {
                            Icon(
                                painter = painterResource(item.iconRes),
                                contentDescription = label,
                                modifier = Modifier.size(28.dp),
                            )
                        },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.navSelectedIcon,
                            selectedTextColor = colors.navSelectedLabel,
                            indicatorColor = colors.navIndicator,
                            unselectedIconColor = colors.navUnselectedIcon,
                            unselectedTextColor = colors.navUnselectedLabel,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    authProvider: AuthProvider,
    padding: PaddingValues,
    /** Withdraws everything the account shared; must succeed before the account goes. */
    onLeaveSharedMap: suspend () -> Result<Unit>,
) {
    val user by authProvider.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showFeedback by rememberSaveable { mutableStateOf(false) }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showInfo) { showInfo = false }
    BackHandler(enabled = showSettings) { showSettings = false }
    val feedbackRepository = remember { FeedbackRepository() }
    val activity = checkNotNull(LocalActivity.current)
    val selectedLanguage = AppLanguageManager.selectedLanguage(activity)
    val signInError = stringResource(R.string.error_sign_in)
    val signOutError = stringResource(R.string.error_sign_out)

    if (showSettings) SettingsScreen(padding, onBack = { showSettings = false })
    else if (showInfo) InfoScreen(
        padding = padding,
        signedIn = user != null,
        onFeedback = { showFeedback = true },
        onDeleteAccount = {
            runCatching {
                onLeaveSharedMap().getOrThrow()
                authProvider.deleteAccount()
            }
        },
        onBack = { showInfo = false },
    ) else Box(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 9.dp, pressedElevation = 5.dp),
        ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.lucky_prospector_mark),
                contentDescription = null,
                modifier = Modifier.size(150.dp),
            )
            // The brand, the same in every language: the translated store title is
            // only for being found, the app itself keeps one name.
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = HandwritingFont,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.profile_description),
                style = MaterialTheme.typography.bodyLarge,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .16f)),
                shadowElevation = 3.dp,
            ) {
                Text(
                    stringResource(R.string.profile_privacy),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            OutlinedButton(onClick = { showLanguageDialog = true }) {
                Text(
                    stringResource(
                        R.string.language_button,
                        stringResource(selectedLanguage.displayNameRes),
                    ),
                )
            }
            OutlinedButton(onClick = { showFeedback = true }) { Text(stringResource(R.string.feedback_button)) }
            OutlinedButton(onClick = { showSettings = true }) { Text(stringResource(R.string.settings_title)) }
            OutlinedButton(onClick = { showInfo = true }) { Text(stringResource(R.string.info_button)) }
            if (user == null) {
                Button(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            when (authProvider.signIn()) {
                                is AuthResult.Success, AuthResult.Cancelled -> Unit
                                is AuthResult.Failure -> error = signInError
                            }
                            busy = false
                        }
                    },
                ) { Text(if (busy) stringResource(R.string.signing_in) else stringResource(R.string.sign_in_google)) }
            } else {
                Text(user?.displayName ?: stringResource(R.string.account_connected))
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            runCatching { authProvider.signOut() }
                                .onFailure { error = signOutError }
                            busy = false
                        }
                    },
                ) { Text(stringResource(R.string.sign_out)) }
            }
            error?.let { Text(it) }
        }
        }
    }
    if (showFeedback) {
        FeedbackDialog(
            signedIn = user != null,
            onSignInRequested = {
                scope.launch {
                    if (authProvider.signIn() is AuthResult.Failure) error = signInError
                }
            },
            onSend = { category, message, contact ->
                runCatching {
                    feedbackRepository.send(
                        category = category,
                        message = message,
                        contact = contact,
                        appVersion = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName.orEmpty(),
                        language = activity.resources.configuration.locales[0].toLanguageTag(),
                    )
                }
            },
            onDismiss = { showFeedback = false },
        )
    }
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language_dialog_title)) },
            text = {
                Column {
                    AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    AppLanguageManager.setLanguage(activity, language)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selectedLanguage == language, onClick = null)
                            Text(stringResource(language.displayNameRes), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }
}

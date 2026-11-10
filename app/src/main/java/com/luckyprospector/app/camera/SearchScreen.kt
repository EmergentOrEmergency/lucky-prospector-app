package com.luckyprospector.app.camera

import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.PathEffect
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.ScaleGestureDetector
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.luckyprospector.app.R
import com.luckyprospector.app.capture.CaptureDraft
import com.luckyprospector.app.capture.CaptureFormState
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.ModelPrediction
import com.luckyprospector.app.inference.*
import com.luckyprospector.app.ui.AppSettings
import com.luckyprospector.app.ui.components.HandwritingFont
import com.luckyprospector.app.location.AndroidLocationProvider
import com.luckyprospector.app.ui.theme.LuckyColors
import com.luckyprospector.app.ui.theme.LuckyTheme
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import kotlin.math.roundToInt

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    detector: Detector = remember { FakeDetector() },
    detectorLoadError: String? = null,
    onConfirmed: (AnalysisState) -> Unit = {},
    /** Returns when the finding is saved (or failed), so the button can wait for it. */
    onCaptureConfirmed: suspend (CaptureDraft) -> Unit = {},
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    if (granted) CameraSearchContent(modifier, detector, detectorLoadError, onConfirmed, onCaptureConfirmed)
    else CameraPermissionPrompt(modifier) { permission.launch(Manifest.permission.CAMERA) }
}

@Composable
private fun CameraSearchContent(
    modifier: Modifier,
    detector: Detector,
    detectorLoadError: String?,
    onConfirmed: (AnalysisState) -> Unit,
    onCaptureConfirmed: suspend (CaptureDraft) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    } }
    val settings by AppSettings.get(context).settings.collectAsState()
    val scene = remember { screenshotScene(context) }
    val imageCapture = remember(settings.resolution) {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(ResolutionSelector.Builder()
                .setAspectRatioStrategy(settings.resolution.aspectRatioStrategy()).build())
            .build()
    }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var requestedZoom by remember { mutableFloatStateOf(1f) }
    var minZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var lastResultAt by remember { mutableLongStateOf(0L) }
    var detectionFresh by remember { mutableStateOf(false) }
    var zoomRequest by remember { mutableLongStateOf(0L) }
    var acceptFramesAfter by remember { mutableLongStateOf(0L) }
    val frameGeneration = rememberUpdatedState(zoomRequest)
    val uiScope = rememberCoroutineScope()
    val locationProvider = remember { AndroidLocationProvider(context) }
    var analysisState by remember { mutableStateOf(AnalysisState()) }
    var capturedPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    var form by remember { mutableStateOf(CaptureFormState()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var savingCapture by remember { mutableStateOf(false) }
    var locationGranted by remember { mutableStateOf(locationProvider.hasPermission()) }
    // The frame on screen when "save" was pressed. While a capture is being taken or
    // confirmed the screen stays on it, so what the user sees is what gets saved.
    var frozenFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    val view = LocalView.current
    var celebration by remember { mutableIntStateOf(0) }
    var celebrationOrigin by remember { mutableStateOf(Offset(.5f, .5f)) }
    var lastCelebration by remember { mutableLongStateOf(0L) }
    val wishes = stringArrayResource(R.array.lucky_wishes)
    var celebrationWish by remember { mutableStateOf<String?>(null) }
    val paused = rememberUpdatedState(frozenFrame != null || capturedPhoto != null || busy)
    val noCloverToSaveMessage by rememberUpdatedState(stringResource(R.string.search_no_clover_to_save))
    val zoomErrorMessage by rememberUpdatedState(stringResource(R.string.camera_zoom_error))
    val cameraStartErrorMessage by rememberUpdatedState(stringResource(R.string.camera_start_error))
    fun capture() {
        val detection = analysisState.detections.captureCandidate()
        if (camera == null || paused.value || !detectionFresh || detection == null ||
            SystemClock.elapsedRealtime() - lastResultAt > DETECTION_FRESH_MS) {
            error = noCloverToSaveMessage
            return
        }
        form = CaptureFormState(type = if (detection.label == DetectionLabel.FOUR_LEAF_CLOVER)
            FindingType.FOUR_LEAF_CLOVER else FindingType.CLOVER_PATCH)
        frozenFrame = previewView.bitmap?.asImageBitmap()
        val prediction = ModelPrediction(detection.label.name, detection.confidence, if (detector is FakeDetector) "demo" else "mobile")
        takePhoto(context, imageCapture, prediction, { busy = it }, { photo ->
            scene?.overwrite(File(photo.uri.path.orEmpty()), previewView.width, previewView.height)
            capturedPhoto = photo
        }) {
            error = it
            frozenFrame = null
        }
    }
    fun resumeLive() {
        zoomRequest++
        acceptFramesAfter = SystemClock.elapsedRealtime()
        frozenFrame = null
        capturedPhoto = null
        analysisState = AnalysisState()
        detectionFresh = false
        celebrationWish = null
    }
    LaunchedEffect(lastResultAt) {
        if (lastResultAt == 0L) return@LaunchedEffect
        delay((DETECTION_FRESH_MS - (SystemClock.elapsedRealtime() - lastResultAt)).coerceAtLeast(0L))
        detectionFresh = false
        if (!paused.value) {
            analysisState = AnalysisState()
            celebrationWish = null
        }
    }
    fun setZoom(ratio: Float) {
        val active = camera ?: return
        if (paused.value) return
        val value = ratio.coerceIn(minZoom, maxZoom)
        requestedZoom = value
        val request = ++zoomRequest
        acceptFramesAfter = Long.MAX_VALUE
        // Boxes from before the zoom change must not enable capture at the new framing.
        analysisState = AnalysisState()
        detectionFresh = false
        val result = active.cameraControl.setZoomRatio(value)
        result.addListener({
            if (camera === active && request == zoomRequest) {
                runCatching { result.get() }.onFailure {
                    if (it.cause !is CameraControl.OperationCanceledException) {
                        error = zoomErrorMessage
                    }
                }
                acceptFramesAfter = SystemClock.elapsedRealtime()
                zoomRequest++
            }
        }, ContextCompat.getMainExecutor(context))
    }
    val zoomAction = rememberUpdatedState<(Float) -> Unit> { setZoom(it) }
    DisposableEffect(previewView) {
        val gesture = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoomAction.value(requestedZoom * detector.scaleFactor)
                return true
            }
        })
        previewView.setOnTouchListener { _, event -> gesture.onTouchEvent(event); true }
        onDispose { previewView.setOnTouchListener(null) }
    }
    var showLocationExplanation by remember { mutableStateOf(false) }
    var encouragementIndex by remember { mutableIntStateOf(0) }
    val encouragements = stringArrayResource(R.array.search_encouragements)
    LaunchedEffect(encouragements.size) {
        while (true) {
            delay(5_500)
            encouragementIndex = (encouragementIndex + 1) % encouragements.size
        }
    }
    val pulse by rememberInfiniteTransition(label = "lucky-box").animateFloat(
        initialValue = .45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lucky-box-pulse",
    )
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        locationGranted = locationProvider.hasPermission()
        capture()
    }

    // Celebrate the moment a clover is confirmed, not every frame it stays confirmed.
    // Clovers confirmed together are one celebration; the cooldown keeps a clover that
    // flickers out and back in from being celebrated twice within a few seconds.
    val celebrate = rememberUpdatedState { found: List<Detection> ->
        val now = SystemClock.elapsedRealtime()
        if (found.isNotEmpty() && !paused.value && now - lastCelebration >= CELEBRATION_COOLDOWN_MS) {
            lastCelebration = now
            val box = found.first().box
            celebrationOrigin = Offset((box.left + box.right) / 2, (box.top + box.bottom) / 2)
            celebration++
            celebrationWish = wishes.random()
            if (settings.vibration) {
                view.performHapticFeedback(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                    else HapticFeedbackConstants.LONG_PRESS,
                )
            }
            if (settings.sound) uiScope.launch { LuckyChime.play() }
        }
    }
    LaunchedEffect(celebration) {
        if (celebration == 0) return@LaunchedEffect
        delay(WISH_VISIBLE_MS)
        celebrationWish = null
    }

    DisposableEffect(lifecycleOwner, detector, settings.sensitivity, settings.resolution, previewSize) {
        if (previewSize == IntSize.Zero) return@DisposableEffect onDispose { }
        // One scope per binding: onDispose cancels it, and a cancelled scope cannot be
        // reused by the next binding when the detector or lifecycle owner changes.
        val analysisScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val future = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        // Frame conversion stays off the main thread while the model runs.
        val analysisExecutor = Executors.newSingleThreadExecutor()
        var disposed = false
        var boundProvider: ProcessCameraProvider? = null
        var boundCamera: Camera? = null
        var boundUseCases: List<UseCase> = emptyList()
        val zoomObserver = androidx.lifecycle.Observer<ZoomState> { state ->
            zoomRatio = state.zoomRatio
            minZoom = state.minZoomRatio
            maxZoom = state.maxZoomRatio
        }
        analysisState = AnalysisState()
        detectionFresh = false
        celebrationWish = null
        acceptFramesAfter = 0L
        future.addListener({
            if (disposed) return@addListener
            try {
                val provider = future.get()
                boundProvider = provider
                val rotation = previewView.display.rotation
                val preview = Preview.Builder()
                    .setTargetRotation(rotation)
                    .setResolutionSelector(ResolutionSelector.Builder()
                        .setAspectRatioStrategy(settings.resolution.aspectRatioStrategy()).build())
                    .build().also { it.surfaceProvider = previewView.surfaceProvider }
                imageCapture.targetRotation = rotation
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setResolutionSelector(settings.resolution.analysisSelector())
                    .setTargetRotation(rotation)
                    .build().also {
                    it.setAnalyzer(analysisExecutor, CloverImageAnalyzer(
                        detector,
                        analysisScope,
                        qualityEvaluator = FrameQualityEvaluator(sizeGuide = detector.sizeGuide),
                        temporalConfirmation = settings.sensitivity.newConfirmation(),
                        isPaused = { paused.value },
                        frameGeneration = { frameGeneration.value },
                        scene = scene,
                    ) { result ->
                        // A result computed just before the freeze must not move the frozen boxes.
                        previewView.post {
                            val now = SystemClock.elapsedRealtime()
                            if (!disposed && !paused.value && result.generation == zoomRequest &&
                                result.receivedAtMillis >= acceptFramesAfter && now - result.receivedAtMillis <= DETECTION_FRESH_MS) {
                                val mapped = result.forPreview(previewView) ?: return@post
                                analysisState = mapped
                                lastResultAt = result.receivedAtMillis
                                detectionFresh = true
                                celebrate.value(mapped.newlyConfirmed)
                                if (mapped.confirmed.isNotEmpty()) onConfirmed(mapped)
                            }
                        }
                    })
                }
                boundUseCases = listOf(preview, analysis, imageCapture)
                val group = UseCaseGroup.Builder().addUseCase(preview).addUseCase(analysis).addUseCase(imageCapture)
                    .setViewPort(requireNotNull(previewView.viewPort)).build()
                val bound = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, group)
                boundCamera = bound
                camera = bound
                bound.cameraInfo.zoomState.observe(lifecycleOwner, zoomObserver)
                val initial = bound.cameraInfo.zoomState.value
                initial?.let { requestedZoom = requestedZoom.coerceIn(it.minZoomRatio, it.maxZoomRatio) }
                bound.cameraControl.setZoomRatio(requestedZoom)
            } catch (_: Exception) {
                camera = null
                error = cameraStartErrorMessage
            }
        }, executor)
        onDispose {
            disposed = true
            boundCamera?.cameraInfo?.zoomState?.removeObserver(zoomObserver)
            boundProvider?.unbind(*boundUseCases.toTypedArray())
            if (camera === boundCamera) camera = null
            analysisScope.cancel()
            analysisExecutor.shutdown()
        }
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize().onSizeChanged { previewSize = it })
        frozenFrame?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        }
        scene?.let {
            val photo = remember(it) { it.photo.asImageBitmap() }
            Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        val palette = LuckyTheme.colors
        val guideLabel = stringResource(R.string.search_size_guide)
        val textMeasurer = rememberTextMeasurer()
        Canvas(Modifier.fillMaxSize()) {
            val confirmed = analysisState.confirmed.map { it.detection }
            val candidates = analysisState.candidates
            // How big a clover should look for the model: shown while searching, hidden
            // as soon as a four-leaf clover is being tracked or the screen is frozen.
            val guide = analysisState.quality?.guideWidthFraction
            if (guide != null && frozenFrame == null && confirmed.isEmpty() && candidates.isEmpty()) {
                drawSizeGuide(guide * size.width, guideLabel, textMeasurer, palette)
            }
            // Every clover gets a faint square, so the user sees the search working;
            // only tracked four-leaf clovers are drawn on top with emphasis.
            analysisState.detections.forEach { detection ->
                if (detection !in confirmed && detection !in candidates) drawCloverHint(detection, palette)
            }
            candidates.forEach { drawCandidateBox(it, palette) }
            confirmed.forEach { drawLuckyBox(it, pulse, palette) }
        }
        CloverCelebration(celebration, celebrationOrigin)
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            color = Color.Black.copy(alpha = .68f),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // A single-frame four-leaf detection is not mentioned at all: it is wrong
                // too often. The text follows the tracker, like the boxes do.
                val confirmedCount = analysisState.confirmed.size
                val fourLeaf = confirmedCount > 0 || analysisState.candidates.isNotEmpty()
                val headline = when {
                    confirmedCount > 1 -> stringResource(R.string.search_four_leaf_confirmed_many, confirmedCount)
                    confirmedCount == 1 -> stringResource(R.string.search_four_leaf_confirmed)
                    fourLeaf -> stringResource(R.string.search_four_leaf_possible)
                    analysisState.quality?.hint == CameraHint.MOVE_CLOSER -> stringResource(R.string.camera_hint_move_closer)
                    analysisState.quality?.hint == CameraHint.MOVE_BACK -> stringResource(R.string.camera_hint_move_back)
                    analysisState.detections.isNotEmpty() -> stringResource(R.string.search_clover_patch)
                    analysisState.quality == null -> stringResource(R.string.camera_preparing)
                    analysisState.quality?.hint == CameraHint.MORE_LIGHT -> stringResource(R.string.camera_hint_more_light)
                    analysisState.quality?.hint == CameraHint.HOLD_STILL -> stringResource(R.string.camera_hint_hold_still)
                    else -> encouragements[encouragementIndex]
                }
                Text(
                    headline,
                    color = if (fourLeaf) LuckyTheme.colors.cameraLuckyText else LuckyTheme.colors.cameraText,
                    style = if (fourLeaf) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                )
                if (fourLeaf) {
                    val confidence = (analysisState.confirmed.map { it.detection } + analysisState.candidates)
                        .maxOfOrNull { it.confidence }?.let { (it * 100).roundToInt() }
                    Text(
                        listOfNotNull(confidence?.let { stringResource(R.string.search_model_confidence, it) },
                            stringResource(R.string.search_check_leaves)).joinToString(" · "),
                        color = LuckyTheme.colors.cameraText,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                    celebrationWish?.let { wish ->
                        Text(wish, color = LuckyTheme.colors.cameraLuckyText,
                            fontFamily = HandwritingFont, fontSize = 18.sp, textAlign = TextAlign.Center)
                    }
                }
                Text(
                    if (detector is FakeDetector) {
                        if (detectorLoadError == null) stringResource(R.string.search_demo_mode) else stringResource(R.string.search_model_unavailable)
                    } else listOfNotNull(
                        stringResource(R.string.search_ai_on_device),
                        analysisState.frameSize?.let { size ->
                            val tiles = analysisState.tilesPerSide
                            if (tiles > 1) "$size · $tiles×$tiles" else size
                        },
                        analysisState.framesPerSecond?.let { stringResource(R.string.search_fps, it) },
                    ).joinToString(" · "),
                    color = if (detector is FakeDetector) LuckyTheme.colors.cameraDemoBadge else LuckyTheme.colors.cameraAiBadge,
                    style = MaterialTheme.typography.labelSmall,
                )
                error?.let { Text(it, color = LuckyTheme.colors.cameraError) }
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (maxZoom > minZoom) Surface(
                color = Color.Black.copy(alpha = .72f),
                contentColor = LuckyTheme.colors.cameraText,
                shape = RoundedCornerShape(28.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { setZoom(requestedZoom / 1.25f) },
                        enabled = camera != null && !paused.value && zoomRatio > minZoom + .01f) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.camera_zoom_out))
                    }
                    TextButton(onClick = { setZoom(1f) }, enabled = camera != null && !paused.value) {
                        Text(stringResource(R.string.camera_zoom_ratio, zoomRatio), color = LuckyTheme.colors.cameraLuckyText)
                    }
                    IconButton(onClick = { setZoom(requestedZoom * 1.25f) },
                        enabled = camera != null && !paused.value && zoomRatio < maxZoom - .01f) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.camera_zoom_in))
                    }
                }
            }
            Button(
                enabled = camera != null && !busy && capturedPhoto == null && detectionFresh && analysisState.detections.captureCandidate() != null,
                onClick = {
                    error = null
                    if (!locationGranted) {
                        showLocationExplanation = true
                    } else capture()
                },
                border = BorderStroke(1.dp, LuckyTheme.colors.cameraDemoBadge.copy(alpha = .55f)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 3.dp),
            ) { Text(if (busy) stringResource(R.string.search_taking_photo) else stringResource(R.string.search_save_finding)) }
            if (!busy && analysisState.detections.captureCandidate() == null) {
                Text(stringResource(R.string.search_save_requires_clover),
                    color = LuckyTheme.colors.cameraText, style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center)
            }
        }

        capturedPhoto?.let { captured ->
            fun cancel() {
                if (savingCapture) return
                File(captured.uri.path.orEmpty()).delete()
                resumeLive()
            }
            BackHandler { cancel() }
            CaptureConfirmation(
                photo = captured.uri,
                state = form,
                saving = savingCapture,
                onStateChange = { form = it },
                onCancel = ::cancel,
                onConfirm = {
                    // Reading the location and copying the photo take a moment; a second
                    // tap meanwhile used to save the same finding twice or three times.
                    if (!savingCapture) {
                        savingCapture = true
                        uiScope.launch {
                            val location = scene?.location ?: if (locationGranted) runCatching { locationProvider.currentLocation() }.getOrNull() else null
                            onCaptureConfirmed(CaptureDraft(captured.uri, form.type, form.normalizedWish(), location, captured.prediction))
                            savingCapture = false
                            resumeLive()
                            form = CaptureFormState()
                        }
                    }
                },
            )
        }
    }

    if (showLocationExplanation) AlertDialog(
        onDismissRequest = { showLocationExplanation = false },
        title = { Text(stringResource(R.string.location_dialog_title)) },
        text = { Text(stringResource(R.string.location_dialog_text)) },
        confirmButton = { Button(onClick = {
            showLocationExplanation = false
            locationPermission.launch(buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION); add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }.toTypedArray())
        }) { Text(stringResource(R.string.allow)) } },
        dismissButton = { TextButton(onClick = {
            showLocationExplanation = false
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                locationPermission.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            } else {
                capture()
            }
        }) { Text(stringResource(R.string.continue_without)) } },
    )

}

/** A dashed circle as big as a clover the model sees well, with a short caption. */
private fun DrawScope.drawSizeGuide(diameter: Float, label: String, textMeasurer: TextMeasurer, palette: LuckyColors) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = diameter / 2
    drawCircle(Color.Black.copy(alpha = .25f), radius, center, style = Stroke(6f))
    drawCircle(
        palette.cameraHint.copy(alpha = .7f),
        radius,
        center,
        style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))),
    )
    val text = textMeasurer.measure(
        label,
        TextStyle(color = palette.cameraHint.copy(alpha = .85f), fontSize = 13.sp, shadow = Shadow(Color.Black, blurRadius = 6f)),
    )
    drawText(text, topLeft = Offset(center.x - text.size.width / 2, center.y + radius + 12f))
}

private fun DrawScope.boxGeometry(detection: Detection): Pair<Offset, Size> {
    val box = detection.box
    return Offset(box.left * size.width, box.top * size.height) to
        Size((box.right - box.left) * size.width, (box.bottom - box.top) * size.height)
}

/** Any clover the model sees, including unconfirmed four-leaf ones: present but quiet. */
private fun DrawScope.drawCloverHint(detection: Detection, palette: LuckyColors) {
    val (topLeft, boxSize) = boxGeometry(detection)
    val radius = CornerRadius(10f)
    drawRoundRect(palette.cameraHint.copy(alpha = .07f), topLeft, boxSize, radius)
    drawRoundRect(palette.cameraHint.copy(alpha = .38f), topLeft, boxSize, radius, style = Stroke(3f))
}

/** A four-leaf clover seen in a few frames at the same place: worth a closer look. */
private fun DrawScope.drawCandidateBox(detection: Detection, palette: LuckyColors) {
    val (topLeft, boxSize) = boxGeometry(detection)
    val radius = CornerRadius(14f)
    drawRoundRect(palette.cameraBoxFourLeaf.copy(alpha = .10f), topLeft, boxSize, radius)
    drawRoundRect(palette.cameraBoxFourLeaf.copy(alpha = .75f), topLeft, boxSize, radius, style = Stroke(5f))
}

/** A confirmed four-leaf clover: the one moment the screen is allowed to celebrate. */
private fun DrawScope.drawLuckyBox(detection: Detection, pulse: Float, palette: LuckyColors) {
    val (topLeft, boxSize) = boxGeometry(detection)
    val color = palette.cameraBoxFourLeaf
    val radius = CornerRadius(18f)

    drawRoundRect(
        color = Color.Black.copy(alpha = .72f),
        topLeft = topLeft,
        size = boxSize,
        cornerRadius = radius,
        style = Stroke(14f),
    )
    drawRoundRect(
        color = color.copy(alpha = .18f + pulse * .24f),
        topLeft = Offset(topLeft.x - 5f, topLeft.y - 5f),
        size = Size(boxSize.width + 10f, boxSize.height + 10f),
        cornerRadius = CornerRadius(22f),
        style = Stroke(22f),
    )
    drawRoundRect(color, topLeft, boxSize, radius, style = Stroke(8f))
    drawLuckyCorners(topLeft, boxSize, color, pulse)
}

private fun DrawScope.drawLuckyCorners(topLeft: Offset, boxSize: Size, color: Color, pulse: Float) {
    val right = topLeft.x + boxSize.width
    val bottom = topLeft.y + boxSize.height
    val length = (minOf(boxSize.width, boxSize.height) * .28f).coerceIn(28f, 76f)
    val stroke = 10f + pulse * 3f
    fun segment(start: Offset, end: Offset) = drawLine(color, start, end, stroke, StrokeCap.Round)
    segment(topLeft, Offset(topLeft.x + length, topLeft.y))
    segment(topLeft, Offset(topLeft.x, topLeft.y + length))
    segment(Offset(right, topLeft.y), Offset(right - length, topLeft.y))
    segment(Offset(right, topLeft.y), Offset(right, topLeft.y + length))
    segment(Offset(topLeft.x, bottom), Offset(topLeft.x + length, bottom))
    segment(Offset(topLeft.x, bottom), Offset(topLeft.x, bottom - length))
    segment(Offset(right, bottom), Offset(right - length, bottom))
    segment(Offset(right, bottom), Offset(right, bottom - length))
}

private data class CapturedPhoto(val uri: Uri, val prediction: ModelPrediction?)

private fun takePhoto(context: Context, capture: ImageCapture, prediction: ModelPrediction?, onBusy: (Boolean) -> Unit, onSuccess: (CapturedPhoto) -> Unit, onError: (String) -> Unit) {
    val file = File.createTempFile("capture-", ".jpg", File(context.cacheDir, "captures").apply { mkdirs() })
    onBusy(true)
    capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) { onBusy(false); onSuccess(CapturedPhoto(Uri.fromFile(file), prediction)) }
        override fun onError(exception: ImageCaptureException) { file.delete(); onBusy(false); onError(context.getString(R.string.error_photo)) }
    })
}

@Composable
private fun CameraPermissionPrompt(modifier: Modifier, onRequest: () -> Unit) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .4f)),
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.camera_permission_description))
                Button(onClick = onRequest, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.camera_permission_button)) }
            }
        }
    }
}

private const val CELEBRATION_COOLDOWN_MS = 4_000L
private const val WISH_VISIBLE_MS = 3_500L
private const val DETECTION_FRESH_MS = 2_000L

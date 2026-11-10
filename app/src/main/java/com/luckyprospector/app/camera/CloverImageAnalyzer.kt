package com.luckyprospector.app.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.os.SystemClock
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.ImageProxyTransformFactory
import com.luckyprospector.app.inference.ConfirmedDetection
import com.luckyprospector.app.inference.Detection
import com.luckyprospector.app.inference.DetectionFrame
import com.luckyprospector.app.inference.Detector
import com.luckyprospector.app.inference.FrameTiling
import com.luckyprospector.app.inference.TemporalConfirmation
import com.luckyprospector.app.inference.Yuv420Converter
import com.luckyprospector.app.inference.YuvPlane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class AnalysisState(
    val detections: List<Detection> = emptyList(),
    val quality: FrameQuality? = null,
    val candidates: List<Detection> = emptyList(),
    val confirmed: List<ConfirmedDetection> = emptyList(),
    /** Clovers confirmed in this very frame, each announced once. */
    val newlyConfirmed: List<Detection> = emptyList(),
    /** Frames the model finished per second, over the last couple of seconds. */
    val framesPerSecond: Float? = null,
    /** Camera frame analysed, sensor orientation (e.g. 1280x960), and its tiles per side. */
    val frameSize: String? = null,
    val tilesPerSide: Int = 1,
    val frameTransform: CameraFrameTransform? = null,
    /** Monotonic time when the analyzer received this frame, before any conversion or inference. */
    val receivedAtMillis: Long = 0L,
    val generation: Long = 0L,
)

/** CameraX analyzer with a conflated one-frame queue: slow inference never builds a backlog. */
@androidx.annotation.OptIn(TransformExperimental::class)
class CloverImageAnalyzer(
    private val detector: Detector,
    private val scope: CoroutineScope,
    private val qualityEvaluator: FrameQualityEvaluator = FrameQualityEvaluator(),
    private val temporalConfirmation: TemporalConfirmation = TemporalConfirmation(),
    /** While true, frames are dropped unread: the screen is frozen on a capture. */
    private val isPaused: () -> Boolean = { false },
    private val frameGeneration: () -> Long = { 0L },
    /** Debug builds only: a photo analysed in place of the camera image. */
    private val scene: ScreenshotScene? = null,
    private val onResult: (AnalysisState) -> Unit,
) : ImageAnalysis.Analyzer {
    private val lock = Any()
    private data class PendingFrame(val frame: DetectionFrame, val transform: CameraFrameTransform,
                                    val receivedAtMillis: Long, val generation: Long)
    private var pendingFrame: PendingFrame? = null
    private var workerRunning = false
    private var trackedGeneration = -1L
    private val finishedAt = ArrayDeque<Long>()

    private fun framesPerSecond(now: Long): Float? {
        finishedAt.addLast(now)
        while (finishedAt.size > 2 && now - finishedAt.first() > FPS_WINDOW_MS) finishedAt.removeFirst()
        val span = now - finishedAt.first()
        return if (finishedAt.size < 2 || span <= 0) null else (finishedAt.size - 1) * 1000f / span
    }

    override fun analyze(image: ImageProxy) {
        val receivedAt = SystemClock.elapsedRealtime()
        val generation = frameGeneration()
        if (isPaused()) {
            image.close()
            return
        }
        val frame = try {
            val converted = image.toDetectionFrame(scene)
            val (width, height) = FrameTiling.uprightSize(image.width, image.height, image.imageInfo.rotationDegrees)
            PendingFrame(converted, CameraFrameTransform(
                ImageProxyTransformFactory().apply { isUsingRotationDegrees = true }.getOutputTransform(image), width, height,
            ), receivedAt, generation)
        } finally {
            image.close()
        }
        val startWorker = synchronized(lock) {
            pendingFrame = frame
            if (workerRunning) false else {
                workerRunning = true
                true
            }
        }
        if (startWorker) scope.launch { drainLatestFrames() }
    }

    private suspend fun drainLatestFrames() {
        while (true) {
            val (frame, transform, receivedAt, generation) = synchronized(lock) {
                pendingFrame.also {
                    pendingFrame = null
                    if (it == null) workerRunning = false
                }
            } ?: return
            if (generation != trackedGeneration) {
                temporalConfirmation.reset()
                trackedGeneration = generation
            }
            runCatching { detector.detect(frame) }.onSuccess { detections ->
                val tracking = temporalConfirmation.update(detections)
                onResult(
                    AnalysisState(
                        detections = detections,
                        quality = qualityEvaluator.evaluate(frame, detections),
                        candidates = tracking.candidates,
                        confirmed = tracking.confirmed,
                        newlyConfirmed = tracking.newlyConfirmed,
                        framesPerSecond = framesPerSecond(SystemClock.elapsedRealtime()),
                        frameSize = "${frame.width}×${frame.height}",
                        tilesPerSide = frame.tilesPerSide,
                        frameTransform = transform,
                        receivedAtMillis = receivedAt,
                        generation = generation,
                    ),
                )
            }
        }
    }
}

private const val FPS_WINDOW_MS = 2_000L

/** Light statistics need no more than ~640 px across: bigger frames are sampled. */
private const val LUMINANCE_WIDTH = 640

private fun ImageProxy.toDetectionFrame(scene: ScreenshotScene? = null): DetectionFrame {
    val tiles = FrameTiling.perSideFor(width, height)
    if (planes.size == 1 || scene != null) return toRgbaFrame(tiles, scene)
    val planeCopies = planes.map { plane ->
        val buffer = plane.buffer.duplicate()
        val copy = ByteArray(buffer.remaining())
        buffer.get(copy)
        YuvPlane(copy, plane.rowStride, plane.pixelStride)
    }
    val plane = planeCopies[0]
    val bytes = ByteArray(width * height)
    var output = 0
    for (row in 0 until height) {
        val rowStart = row * plane.rowStride
        for (column in 0 until width) bytes[output++] = plane.bytes[rowStart + column * plane.pixelStride]
    }
    return DetectionFrame(
        luminance = bytes,
        width = width,
        height = height,
        rotationDegrees = imageInfo.rotationDegrees,
        timestampMillis = imageInfo.timestamp / 1_000_000,
        rgb = if (planeCopies.size >= 3) Yuv420Converter.toRgb(width, height, planeCopies[0], planeCopies[1], planeCopies[2]) else null,
        tilesPerSide = tiles,
    )
}

/**
 * CameraX converts to RGBA natively, much faster than a YUV loop in Kotlin: that is
 * what makes 1280x960 and 1920x1440 affordable.
 */
private fun ImageProxy.toRgbaFrame(tiles: Int, scene: ScreenshotScene?): DetectionFrame {
    val plane = planes[0]
    val buffer = plane.buffer.duplicate()
    val rowBytes = width * 4
    val rgba = scene?.rgbaFrame(width, height, imageInfo.rotationDegrees) ?: ByteArray(rowBytes * height)
    if (scene != null) {
        // The camera's pixels are ignored: the scene stands in for them.
    } else if (plane.rowStride == rowBytes) {
        buffer.get(rgba)
    } else {
        for (row in 0 until height) {
            buffer.position(row * plane.rowStride)
            buffer.get(rgba, row * rowBytes, rowBytes)
        }
    }
    val step = maxOf(1, width / LUMINANCE_WIDTH)
    val sampledWidth = width / step
    val sampledHeight = height / step
    val luminance = ByteArray(sampledWidth * sampledHeight)
    var output = 0
    for (row in 0 until sampledHeight) {
        var offset = row * step * rowBytes
        for (column in 0 until sampledWidth) {
            val r = rgba[offset].toInt() and 0xff
            val g = rgba[offset + 1].toInt() and 0xff
            val b = rgba[offset + 2].toInt() and 0xff
            luminance[output++] = ((r * 77 + g * 150 + b * 29) shr 8).toByte()
            offset += step * 4
        }
    }
    return DetectionFrame(
        luminance = luminance,
        width = width,
        height = height,
        rotationDegrees = imageInfo.rotationDegrees,
        timestampMillis = imageInfo.timestamp / 1_000_000,
        rgba = rgba,
        tilesPerSide = tiles,
    )
}

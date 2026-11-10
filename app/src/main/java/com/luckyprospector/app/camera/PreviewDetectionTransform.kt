package com.luckyprospector.app.camera

import android.graphics.RectF
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.OutputTransform
import com.luckyprospector.app.inference.Detection
import com.luckyprospector.app.inference.NormalizedBox

@androidx.annotation.OptIn(TransformExperimental::class)
data class CameraFrameTransform(val output: OutputTransform, val width: Int, val height: Int)

/** Map the upright model buffer into the actual preview crop, including rotation and zoom. */
@androidx.annotation.OptIn(TransformExperimental::class)
internal fun AnalysisState.forPreview(preview: PreviewView): AnalysisState? {
    val source = frameTransform ?: return null
    val target = preview.outputTransform ?: return null
    if (preview.width <= 0 || preview.height <= 0) return null
    val transform = CoordinateTransform(source.output, target)
    fun map(detection: Detection): Detection {
        val box = detection.box
        val rect = RectF(box.left * source.width, box.top * source.height, box.right * source.width, box.bottom * source.height)
        transform.mapRect(rect)
        return detection.copy(box = NormalizedBox(
            rect.left / preview.width, rect.top / preview.height,
            rect.right / preview.width, rect.bottom / preview.height,
        ))
    }
    fun Detection.visible() = listOf(this).captureCandidate() != null
    val guide = quality?.guideWidthFraction?.let { fraction ->
        val points = floatArrayOf((.5f - fraction / 2) * source.width, source.height / 2f,
            (.5f + fraction / 2) * source.width, source.height / 2f)
        transform.mapPoints(points)
        kotlin.math.abs(points[2] - points[0]) / preview.width
    }
    return copy(
        detections = detections.map(::map).filter { it.visible() },
        candidates = candidates.map(::map).filter { it.visible() },
        confirmed = confirmed.map { it.copy(detection = map(it.detection)) }.filter { it.detection.visible() },
        newlyConfirmed = newlyConfirmed.map(::map).filter { it.visible() },
        quality = quality?.copy(guideWidthFraction = guide),
    )
}

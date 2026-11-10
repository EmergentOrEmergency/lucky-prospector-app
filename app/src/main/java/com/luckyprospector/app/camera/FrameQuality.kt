package com.luckyprospector.app.camera

import com.luckyprospector.app.inference.Detection
import com.luckyprospector.app.inference.DetectionFrame
import com.luckyprospector.app.inference.SizeGuide

enum class CameraHint { MORE_LIGHT, HOLD_STILL, MOVE_CLOSER, MOVE_BACK, SEARCHING, GOOD }

data class FrameQuality(
    val meanLuminance: Float,
    val edgeEnergy: Float,
    val hint: CameraHint,
    /** Ideal clover width as a fraction of the frame width, for the on-screen guide. */
    val guideWidthFraction: Float? = null,
)

class FrameQualityEvaluator(
    private val minimumLuminance: Float = 42f,
    private val minimumEdgeEnergy: Float = 7f,
    private val minimumSubjectArea: Float = .055f,
    /** The model's size guide; without it, "closer" falls back to [minimumSubjectArea]. */
    private val sizeGuide: SizeGuide? = null,
) {
    fun evaluate(frame: DetectionFrame, detections: List<Detection>): FrameQuality {
        if (frame.luminance.isEmpty()) return FrameQuality(0f, 0f, CameraHint.MORE_LIGHT)
        val step = 4
        var sum = 0L
        var sampled = 0
        var edges = 0L
        var previous = frame.luminance[0].toInt() and 0xff
        for (index in frame.luminance.indices step step) {
            val value = frame.luminance[index].toInt() and 0xff
            sum += value
            edges += kotlin.math.abs(value - previous)
            previous = value
            sampled++
        }
        val luminance = sum.toFloat() / sampled
        val edgeEnergy = edges.toFloat() / sampled
        // The largest leaf decides: if even that one is too small the model misses the
        // rest, and if it is too big there is too little lawn in view.
        val sizeHint = when {
            detections.isEmpty() -> null
            sizeGuide != null -> {
                val largest = detections.maxOf {
                    sizeGuide.leafPx(it.box, frame)
                }
                when {
                    largest < sizeGuide.minPx -> CameraHint.MOVE_CLOSER
                    largest > sizeGuide.maxPx -> CameraHint.MOVE_BACK
                    else -> null
                }
            }
            detections.maxOf { it.box.area } < minimumSubjectArea -> CameraHint.MOVE_CLOSER
            else -> null
        }
        val hint = when {
            luminance < minimumLuminance -> CameraHint.MORE_LIGHT
            edgeEnergy < minimumEdgeEnergy -> CameraHint.HOLD_STILL
            sizeHint != null -> sizeHint
            detections.isEmpty() -> CameraHint.SEARCHING
            else -> CameraHint.GOOD
        }
        val guide = sizeGuide?.idealWidthFraction(frame)
        return FrameQuality(luminance, edgeEnergy, hint, guide)
    }
}


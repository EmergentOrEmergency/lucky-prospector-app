package com.luckyprospector.app.camera

import com.luckyprospector.app.inference.Detection
import com.luckyprospector.app.inference.DetectionLabel

/** Only current, visible detections can enable capture; tracking history alone cannot. */
fun List<Detection>.captureCandidate(): Detection? =
    filter { detection ->
        val box = detection.box
        detection.confidence.isFinite() && detection.confidence in 0f..1f &&
            listOf(box.left, box.top, box.right, box.bottom).all(Float::isFinite) &&
            box.area > 0f && box.right > 0f && box.bottom > 0f && box.left < 1f && box.top < 1f
    }.maxWithOrNull(
        compareBy<Detection> { it.label == DetectionLabel.FOUR_LEAF_CLOVER }
            .thenBy { it.confidence },
    )

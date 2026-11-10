package com.luckyprospector.app.inference

/** Demo-only detector. Its predictable cycle lets the complete camera UX run without a model. */
class FakeDetector(
    private val detectionPeriod: Int = 24,
) : Detector {
    private var frameNumber = 0

    override suspend fun detect(frame: DetectionFrame): List<Detection> {
        frameNumber++
        val phase = frameNumber % detectionPeriod
        return when {
            phase in 4..10 -> listOf(
                Detection(
                    label = DetectionLabel.CLOVER_PATCH,
                    confidence = 0.76f,
                    box = NormalizedBox(.12f, .24f, .88f, .82f),
                ),
            )
            phase in 14..19 -> listOf(
                Detection(
                    label = DetectionLabel.FOUR_LEAF_CLOVER,
                    confidence = 0.91f,
                    box = NormalizedBox(.35f, .32f, .68f, .7f),
                ),
            )
            else -> emptyList()
        }
    }
}


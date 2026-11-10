package com.luckyprospector.app.inference

/**
 * Detector-neutral frame. Keeps CameraX and ML runtimes decoupled.
 *
 * [luminance] is only used for light and sharpness statistics and may be subsampled;
 * the pixels the model sees are in [rgba] (packed, 4 bytes per pixel) or [rgb].
 */
data class DetectionFrame(
    val luminance: ByteArray,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val timestampMillis: Long,
    val rgb: ByteArray? = null,
    val rgba: ByteArray? = null,
    /** The upright frame is analysed as tilesPerSide x tilesPerSide tiles (see [FrameTiling]). */
    val tilesPerSide: Int = 1,
)

enum class DetectionLabel { FOUR_LEAF_CLOVER, CLOVER_PATCH }

data class NormalizedBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val area: Float get() = (right - left).coerceAtLeast(0f) * (bottom - top).coerceAtLeast(0f)

    fun intersectionOverUnion(other: NormalizedBox): Float {
        val intersectionWidth = (minOf(right, other.right) - maxOf(left, other.left)).coerceAtLeast(0f)
        val intersectionHeight = (minOf(bottom, other.bottom) - maxOf(top, other.top)).coerceAtLeast(0f)
        val intersection = intersectionWidth * intersectionHeight
        val union = area + other.area - intersection
        return if (union > 0f) intersection / union else 0f
    }
}

data class Detection(
    val label: DetectionLabel,
    val confidence: Float,
    val box: NormalizedBox,
)

fun interface Detector {
    suspend fun detect(frame: DetectionFrame): List<Detection>

    /** How big a leaf should look for this detector, when its model declares it. */
    val sizeGuide: SizeGuide? get() = null
}

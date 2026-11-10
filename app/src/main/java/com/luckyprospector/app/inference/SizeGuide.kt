package com.luckyprospector.app.inference

/**
 * How big a leaf must look for the model to recognise it, measured by the lab as
 * the longest side of the box in pixels on the model input (report T19 size bands).
 *
 * RC1 is a close-up detector: its recall is 61% at 80-120 px, 88% at 120-200 px and
 * 97% from 200 px, and collapses below 80 px (7% at 30-50 px). The limit comes from
 * the training data, not the architecture, so each model release carries its own
 * values in model-metadata.json.
 */
data class SizeGuide(
    /** Below this the model misses too many leaves: the user should move closer. */
    val minPx: Int,
    /** The size the on-screen guide shows. */
    val idealPx: Int,
    /** Above this the frame holds too little lawn to search: the user should step back. */
    val maxPx: Int,
    val inputWidth: Int,
    val inputHeight: Int,
) {
    init {
        require(minPx in 1 until idealPx && idealPx < maxPx) { "Expected min < ideal < max" }
        require(inputWidth > 0 && inputHeight > 0)
    }

    /** Longest side of [box], in model-input pixels, as the model sees it in its tile. */
    fun leafPx(box: NormalizedBox, frame: DetectionFrame): Float {
        val tiling = tiling(frame)
        return maxOf((box.right - box.left) * tiling.frameWidth, (box.bottom - box.top) * tiling.frameHeight) *
            scale(tiling)
    }

    /** The ideal clover as a fraction of the upright frame's width: what the guide draws. */
    fun idealWidthFraction(frame: DetectionFrame): Float {
        val tiling = tiling(frame)
        return idealPx / scale(tiling) / tiling.frameWidth
    }

    private fun tiling(frame: DetectionFrame): FrameTiling {
        val (width, height) = FrameTiling.uprightSize(frame.width, frame.height, frame.rotationDegrees)
        return FrameTiling(width, height, frame.tilesPerSide)
    }

    private fun scale(tiling: FrameTiling) =
        minOf(inputWidth.toFloat() / tiling.tileWidth, inputHeight.toFloat() / tiling.tileHeight)

    companion object {
        /** RC1 on the full frame, from the lab's size-band evaluation. */
        val RC1 = SizeGuide(minPx = 120, idealPx = 240, maxPx = 400, inputWidth = 640, inputHeight = 640)
    }
}

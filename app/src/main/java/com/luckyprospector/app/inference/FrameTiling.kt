package com.luckyprospector.app.inference

import kotlin.math.ceil

/**
 * How an upright frame is cut into tiles for the model. The model always sees
 * 640x640: a bigger camera frame alone would just be shrunk back, and the clovers
 * would look as small to the model as before. Cutting it into [perSide] x [perSide]
 * tiles keeps each tile near the model's input size, so a clover may cover a smaller
 * part of the screen and still be seen.
 *
 * Tiles overlap by [OVERLAP] of their nominal size, so a clover on the seam is whole
 * in at least one of them. All tiles have the same size, hence the same scale.
 */
data class FrameTiling(val frameWidth: Int, val frameHeight: Int, val perSide: Int) {
    init {
        require(frameWidth > 0 && frameHeight > 0 && perSide in 1..MAX_PER_SIDE)
    }

    val tileWidth: Int = tileSide(frameWidth)
    val tileHeight: Int = tileSide(frameHeight)

    /** Top-left corner of each tile, row by row. */
    val origins: List<Pair<Int, Int>> = (0 until perSide).flatMap { row ->
        (0 until perSide).map { column -> offset(column, frameWidth, tileWidth) to offset(row, frameHeight, tileHeight) }
    }

    private fun tileSide(side: Int) =
        if (perSide == 1) side else minOf(side, ceil(side.toFloat() / perSide * (1 + OVERLAP)).toInt())

    private fun offset(index: Int, side: Int, tile: Int) =
        if (perSide == 1) 0 else index * (side - tile) / (perSide - 1)

    companion object {
        const val OVERLAP = .2f
        const val MAX_PER_SIDE = 3

        /** About one tile per model input side: 640x480 is one tile, 1280x960 is 2x2. */
        fun perSideFor(width: Int, height: Int, inputSide: Int = 640) =
            Math.round(maxOf(width, height).toFloat() / inputSide).coerceIn(1, MAX_PER_SIDE)

        /** Upright size of a frame, whatever the sensor's orientation. */
        fun uprightSize(width: Int, height: Int, rotationDegrees: Int) =
            if (rotationDegrees % 180 == 0) width to height else height to width
    }
}

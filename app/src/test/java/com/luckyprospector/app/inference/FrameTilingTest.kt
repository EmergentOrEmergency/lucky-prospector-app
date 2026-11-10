package com.luckyprospector.app.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameTilingTest {
    @Test fun `the tile count follows the camera frame`() {
        assertEquals(1, FrameTiling.perSideFor(640, 480))
        assertEquals(2, FrameTiling.perSideFor(1280, 960))
        assertEquals(2, FrameTiling.perSideFor(1440, 1080))
        assertEquals(3, FrameTiling.perSideFor(1920, 1440))
        assertEquals(3, FrameTiling.perSideFor(4000, 3000))
    }

    @Test fun `one tile is the whole frame`() {
        val tiling = FrameTiling(480, 640, 1)
        assertEquals(480, tiling.tileWidth)
        assertEquals(640, tiling.tileHeight)
        assertEquals(listOf(0 to 0), tiling.origins)
    }

    @Test fun `tiles overlap and cover the frame edge to edge`() {
        val tiling = FrameTiling(960, 1280, 2)
        assertEquals(576, tiling.tileWidth)
        assertEquals(768, tiling.tileHeight)
        assertEquals(listOf(0 to 0, 384 to 0, 0 to 512, 384 to 512), tiling.origins)
        tiling.origins.forEach { (x, y) ->
            assert(x + tiling.tileWidth <= 960 && y + tiling.tileHeight <= 1280)
        }
    }

    @Test fun `a box found in a tile lands in the right place of the frame`() {
        val tiling = FrameTiling(960, 1280, 2)
        val inTile = Detection(DetectionLabel.CLOVER_PATCH, .9f, NormalizedBox(.5f, .5f, .9f, .9f))
        val moved = inTile.fromTile(tiling, 384, 512)
        assertEquals((384 + 288) / 960f, moved.detection.box.left, .001f)
        assertEquals((512 + 384) / 1280f, moved.detection.box.top, .001f)
        assertFalse(moved.cut)
    }

    @Test fun `only the edges between tiles cut a box, not the frame edges`() {
        val tiling = FrameTiling(960, 1280, 2)
        val onFrameEdge = Detection(DetectionLabel.CLOVER_PATCH, .9f, NormalizedBox(0f, 0f, .3f, .3f))
        val onSeam = Detection(DetectionLabel.CLOVER_PATCH, .9f, NormalizedBox(.7f, .2f, 1f, .4f))
        assertFalse(onFrameEdge.fromTile(tiling, 0, 0).cut)
        assertTrue(onSeam.fromTile(tiling, 0, 0).cut)
        assertFalse(onSeam.fromTile(tiling, 384, 0).cut)
    }

    @Test fun `a clover cut by a seam is kept once, whole`() {
        val whole = four(.8f, .40f, .40f, .60f, .60f)
        val cut = four(.9f, .40f, .40f, .52f, .60f)
        val other = Detection(DetectionLabel.CLOVER_PATCH, .7f, NormalizedBox(.10f, .10f, .20f, .20f))
        val merged = mergeAcrossTiles(listOf(TileDetection(cut, true), TileDetection(other, false), TileDetection(whole, false)))
        assertEquals(listOf(whole, other), merged)
    }

    @Test fun `a clover wider than the overlap is joined from its pieces`() {
        val pieces = listOf(four(.7f, .30f, .30f, .50f, .50f), four(.9f, .45f, .30f, .70f, .50f), four(.8f, .30f, .45f, .50f, .70f))
        val merged = mergeAcrossTiles(pieces.map { TileDetection(it, true) })
        assertEquals(listOf(four(.9f, .30f, .30f, .70f, .70f)), merged)
    }

    @Test fun `separate cut clovers stay separate`() {
        val left = four(.8f, .10f, .40f, .20f, .50f)
        val right = four(.7f, .70f, .40f, .80f, .50f)
        assertEquals(2, mergeAcrossTiles(listOf(TileDetection(left, true), TileDetection(right, true))).size)
    }

    private fun four(confidence: Float, left: Float, top: Float, right: Float, bottom: Float) =
        Detection(DetectionLabel.FOUR_LEAF_CLOVER, confidence, NormalizedBox(left, top, right, bottom))
}

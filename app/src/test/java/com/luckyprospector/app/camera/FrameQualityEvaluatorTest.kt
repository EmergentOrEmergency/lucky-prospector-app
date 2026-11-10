package com.luckyprospector.app.camera

import com.luckyprospector.app.inference.Detection
import com.luckyprospector.app.inference.DetectionFrame
import com.luckyprospector.app.inference.DetectionLabel
import com.luckyprospector.app.inference.NormalizedBox
import com.luckyprospector.app.inference.SizeGuide
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameQualityEvaluatorTest {
    private val evaluator = FrameQualityEvaluator()

    @Test fun `dark frame requests more light`() {
        assertEquals(CameraHint.MORE_LIGHT, evaluator.evaluate(frame(ByteArray(100) { 10 }), emptyList()).hint)
    }

    @Test fun `flat frame requests a steady phone`() {
        assertEquals(CameraHint.HOLD_STILL, evaluator.evaluate(frame(ByteArray(100) { 100 }), emptyList()).hint)
    }

    @Test fun `small detected subject requests closer camera`() {
        val detection = Detection(DetectionLabel.FOUR_LEAF_CLOVER, .9f, NormalizedBox(.1f, .1f, .2f, .2f))
        assertEquals(CameraHint.MOVE_CLOSER, evaluator.evaluate(texturedFrame(), listOf(detection)).hint)
    }

    @Test fun `clear large subject is good`() {
        val detection = Detection(DetectionLabel.CLOVER_PATCH, .9f, NormalizedBox(.1f, .1f, .8f, .8f))
        assertEquals(CameraHint.GOOD, evaluator.evaluate(texturedFrame(), listOf(detection)).hint)
    }

    private val guided = FrameQualityEvaluator(sizeGuide = SizeGuide.RC1)

    /** A box of [side] pixels on a 480x640 upright frame (the app's 640x480 stream, rotated). */
    private fun leaf(side: Float): Detection {
        val w = side / 480f
        val h = side / 640f
        return Detection(DetectionLabel.CLOVER_PATCH, .9f, NormalizedBox(.3f, .3f, .3f + w, .3f + h))
    }

    @Test fun `with a size guide a leaf under the minimum asks to move closer`() {
        assertEquals(CameraHint.MOVE_CLOSER, guided.evaluate(cameraFrame(), listOf(leaf(90f))).hint)
    }

    @Test fun `with a size guide a leaf in the band is good`() {
        assertEquals(CameraHint.GOOD, guided.evaluate(cameraFrame(), listOf(leaf(240f))).hint)
    }

    @Test fun `with a size guide a huge leaf asks to step back`() {
        assertEquals(CameraHint.MOVE_BACK, guided.evaluate(cameraFrame(), listOf(leaf(450f))).hint)
    }

    @Test fun `the largest leaf decides`() {
        assertEquals(CameraHint.GOOD, guided.evaluate(cameraFrame(), listOf(leaf(60f), leaf(200f))).hint)
    }

    @Test fun `the guide is half the screen width for RC1 on the camera stream`() {
        assertEquals(.5f, guided.evaluate(cameraFrame(), emptyList()).guideWidthFraction!!, .001f)
    }

    /** 640x480 landscape stream rotated by 90 degrees, textured so it is not flat. */
    private fun cameraFrame() = DetectionFrame(ByteArray(640 * 480) { if ((it / 4) % 2 == 0) 70 else 180.toByte() }, 640, 480, 90, 0)

    private fun texturedFrame() = frame(ByteArray(100) { if ((it / 4) % 2 == 0) 70 else 180.toByte() })
    private fun frame(bytes: ByteArray) = DetectionFrame(bytes, 10, 10, 0, 0)
}

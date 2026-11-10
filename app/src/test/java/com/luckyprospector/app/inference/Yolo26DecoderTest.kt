package com.luckyprospector.app.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Yolo26DecoderTest {
    private val metadata = ModelMetadata.parse(validMetadataJson())

    @Test fun `synthetic golden decodes coordinates score and class`() {
        val output = floatArrayOf(
            64f, 128f, 320f, 512f, .91f, 1f,
            0f, 0f, 640f, 640f, .20f, 0f,
            10f, 10f, 20f, 20f, .80f, 99f,
        )
        val detections = Yolo26Decoder(metadata).decode(output)
        assertEquals(1, detections.size)
        assertEquals(DetectionLabel.FOUR_LEAF_CLOVER, detections.single().label)
        assertEquals(.91f, detections.single().confidence, 0.0001f)
        assertEquals(NormalizedBox(.1f, .2f, .5f, .8f), detections.single().box)
    }

    @Test fun `adapter preprocesses luminance to normalized RGB and uses injected runner`() {
        var captured = floatArrayOf()
        val runner = FloatModelRunner { input ->
            captured = input
            floatArrayOf(0f, 0f, 640f, 640f, .75f, 0f)
        }
        val detector = TfliteDetector(metadata, runner)
        val rgb = byteArrayOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 64, 65, 66)
        val output = detector.preprocessAndRun(DetectionFrame(byteArrayOf(0, 127, -1, 64), 2, 2, 0, 1, rgb))
        assertEquals(6, output.size)
        assertEquals(640 * 640 * 3, captured.size)
        assertEquals(0f, captured.first(), .0001f)
        assertTrue(captured.last() in .258f..0.260f)
    }
}

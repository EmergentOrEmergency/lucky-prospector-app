package com.luckyprospector.app.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoloxDecoderTest {
    private val metadata = ModelMetadata.parse(validYoloxMetadataJson())
    private val transform = LetterboxTransform.forSize(1_280, 720, 640, 640)

    @Test fun `decodes direct class index and undoes top-left letterbox`() {
        val output = row(cx = 320f, cy = 180f, width = 320f, height = 180f, objectness = .95f, class1 = .9f)
        val detection = YoloxDecoder(metadata).decode(output, transform).single()

        assertEquals(DetectionLabel.FOUR_LEAF_CLOVER, detection.label)
        assertEquals(.855f, detection.confidence, .0001f)
        assertEquals(NormalizedBox(.25f, .25f, .75f, .75f), detection.box)
    }

    @Test fun `applies class agnostic nms`() {
        val output = row(320f, 180f, 320f, 180f, .95f, class1 = .9f) +
            row(322f, 182f, 320f, 180f, .90f, class0 = .9f)

        val detections = YoloxDecoder(metadata).decode(output, transform)
        assertEquals(1, detections.size)
        assertTrue(detections.single().confidence > .8f)
    }

    private fun row(
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        objectness: Float,
        class0: Float = .01f,
        class1: Float = .01f,
        class2: Float = .01f,
    ) = floatArrayOf(cx, cy, width, height, objectness, class0, class1, class2)
}

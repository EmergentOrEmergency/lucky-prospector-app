package com.luckyprospector.app.camera

import com.luckyprospector.app.inference.Detection
import com.luckyprospector.app.inference.DetectionLabel
import com.luckyprospector.app.inference.NormalizedBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchCapturePolicyTest {
    private fun clover(label: DetectionLabel = DetectionLabel.CLOVER_PATCH, confidence: Float = .8f,
                       box: NormalizedBox = NormalizedBox(.2f, .2f, .7f, .7f)) = Detection(label, confidence, box)

    @Test fun `nothing to detect means nothing to save`() {
        assertNull(emptyList<Detection>().captureCandidate())
    }

    @Test fun `a patch and a four leaf clover can both be saved`() {
        for (label in DetectionLabel.entries) {
            val detection = clover(label)
            assertEquals(detection, listOf(detection).captureCandidate())
        }
    }

    @Test fun `four leaf clover wins over a surrounding patch`() {
        val fourLeaf = clover(DetectionLabel.FOUR_LEAF_CLOVER, .7f)
        assertEquals(fourLeaf, listOf(clover(confidence = .99f), fourLeaf).captureCandidate())
    }

    @Test fun `no detections outside the visible preview enable save`() {
        val boxes = listOf(NormalizedBox(-.4f, .2f, -.1f, .6f), NormalizedBox(1.1f, .2f, 1.3f, .6f),
            NormalizedBox(.2f, -1f, .6f, 0f), NormalizedBox(.2f, 1f, .6f, 1.5f))
        for (box in boxes) assertNull(listOf(clover(box = box)).captureCandidate())
    }

    @Test fun `a partially visible clover can still be saved`() {
        val detection = clover(box = NormalizedBox(-.1f, .2f, .3f, .6f))
        assertEquals(detection, listOf(detection).captureCandidate())
    }

    @Test fun `invalid model output cannot enable capture`() {
        for (confidence in listOf(Float.NaN, Float.POSITIVE_INFINITY, -.1f, 1.1f))
            assertNull(listOf(clover(confidence = confidence)).captureCandidate())
        assertNull(listOf(clover(box = NormalizedBox(Float.NaN, .2f, .3f, .6f))).captureCandidate())
        assertNull(listOf(clover(box = NormalizedBox(.6f, .2f, .3f, .6f))).captureCandidate())
    }

    @Test fun `the button loses its candidate as soon as the current detections disappear`() {
        assertEquals(DetectionLabel.CLOVER_PATCH, listOf(clover()).captureCandidate()?.label)
        assertNull(emptyList<Detection>().captureCandidate())
    }
}

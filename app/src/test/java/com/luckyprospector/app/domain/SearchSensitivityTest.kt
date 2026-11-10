package com.luckyprospector.app.domain

import com.luckyprospector.app.inference.Detection
import com.luckyprospector.app.inference.DetectionLabel
import com.luckyprospector.app.inference.NormalizedBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSensitivityTest {

    private fun clover(confidence: Float) =
        Detection(DetectionLabel.FOUR_LEAF_CLOVER, confidence, NormalizedBox(.3f, .3f, .5f, .5f))

    /** Frames needed before the first confirmation, or null if it never comes in 12. */
    private fun framesToConfirm(level: SearchSensitivity, confidence: Float): Int? {
        val tracker = level.newConfirmation()
        repeat(12) { frame -> if (tracker.update(listOf(clover(confidence))).confirmed.isNotEmpty()) return frame + 1 }
        return null
    }

    @Test
    fun `normal is the calibration chosen for the product`() {
        with(SearchSensitivity.NORMAL) {
            assertEquals(2, windowSize)
            assertEquals(2, hitsRequired)
            assertEquals(.70f, confirmConfidence)
            assertEquals(.50f, candidateConfidence)
        }
    }

    @Test
    fun `two frames confirm for careful and normal, one for sensitive`() {
        assertEquals(2, framesToConfirm(SearchSensitivity.CAREFUL, .9f))
        assertEquals(2, framesToConfirm(SearchSensitivity.NORMAL, .9f))
        assertEquals(1, framesToConfirm(SearchSensitivity.SENSITIVE, .9f))
    }

    @Test
    fun `normal confirms at the second frame, not the first`() {
        val tracker = SearchSensitivity.NORMAL.newConfirmation()
        assertTrue(tracker.update(listOf(clover(.9f))).confirmed.isEmpty())
        assertTrue(tracker.update(listOf(clover(.9f))).confirmed.isNotEmpty())
    }

    @Test
    fun `normal needs the two frames in a row`() {
        val tracker = SearchSensitivity.NORMAL.newConfirmation()
        tracker.update(listOf(clover(.9f)))
        assertTrue(tracker.update(emptyList()).confirmed.isEmpty())
        assertTrue(tracker.update(listOf(clover(.9f))).confirmed.isEmpty())
        assertTrue(tracker.update(listOf(clover(.9f))).confirmed.isNotEmpty())
    }

    @Test
    fun `a confidence of 0_65 is announced only by the sensitive level`() {
        assertNull(framesToConfirm(SearchSensitivity.CAREFUL, .65f))
        assertNull(framesToConfirm(SearchSensitivity.NORMAL, .65f))
        assertNotNull(framesToConfirm(SearchSensitivity.SENSITIVE, .65f))
    }
}

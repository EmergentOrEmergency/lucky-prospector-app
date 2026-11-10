package com.luckyprospector.app.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporalConfirmationTest {
    private val clover = Detection(
        DetectionLabel.FOUR_LEAF_CLOVER,
        .9f,
        NormalizedBox(.2f, .2f, .4f, .4f),
    )

    /** The same clover, slightly shifted as a hand-held phone moves. */
    private fun cloverAt(dx: Float, confidence: Float = .9f) = clover.copy(
        confidence = confidence,
        box = NormalizedBox(.2f + dx, .2f, .4f + dx, .4f),
    )

    /** A detection somewhere else in the frame. */
    private val elsewhere = clover.copy(box = NormalizedBox(.6f, .6f, .8f, .8f))

    @Test fun `a single frame is never announced`() {
        val tracking = TemporalConfirmation().update(listOf(clover))
        assertTrue(tracking.confirmed.isEmpty())
        assertTrue(tracking.candidates.isEmpty())
    }

    @Test fun `two frames at the same place make a candidate`() {
        val confirmation = TemporalConfirmation()
        confirmation.update(listOf(clover))
        val tracking = confirmation.update(listOf(cloverAt(.01f)))
        assertTrue(tracking.candidates.isNotEmpty())
        assertTrue(tracking.confirmed.isEmpty())
    }

    @Test fun `confirms after four steady frames at the same place`() {
        val confirmation = TemporalConfirmation()
        repeat(3) { assertTrue(confirmation.update(listOf(cloverAt(it * .01f))).confirmed.isEmpty()) }
        assertEquals(4, confirmation.update(listOf(cloverAt(.03f))).confirmed.single().consecutiveFrames)
    }

    @Test fun `one missed frame is tolerated`() {
        val confirmation = TemporalConfirmation()
        confirmation.update(listOf(clover))
        confirmation.update(listOf(clover))
        confirmation.update(emptyList())
        confirmation.update(listOf(clover))
        assertTrue(confirmation.update(listOf(clover)).confirmed.isNotEmpty())
    }

    @Test fun `detections jumping around the frame never add up`() {
        val confirmation = TemporalConfirmation()
        repeat(10) { frame ->
            val tracking = confirmation.update(listOf(if (frame % 2 == 0) clover else elsewhere))
            assertTrue(tracking.confirmed.isEmpty())
        }
    }

    @Test fun `medium confidence can be a candidate but is never confirmed`() {
        val confirmation = TemporalConfirmation()
        repeat(10) {
            val tracking = confirmation.update(listOf(clover.copy(confidence = .6f)))
            assertTrue(tracking.confirmed.isEmpty())
            if (it >= 1) assertTrue(tracking.candidates.isNotEmpty())
        }
    }

    @Test fun `low confidence is ignored entirely`() {
        val confirmation = TemporalConfirmation()
        repeat(10) {
            val tracking = confirmation.update(listOf(clover.copy(confidence = .4f)))
            assertTrue(tracking.candidates.isEmpty())
            assertTrue(tracking.confirmed.isEmpty())
        }
    }

    @Test fun `ordinary clovers are not tracked`() {
        val confirmation = TemporalConfirmation()
        repeat(10) {
            val tracking = confirmation.update(listOf(clover.copy(label = DetectionLabel.CLOVER_PATCH)))
            assertTrue(tracking.candidates.isEmpty())
            assertTrue(tracking.confirmed.isEmpty())
        }
    }

    @Test fun `a lost clover is forgotten and a new place can be tracked`() {
        val confirmation = TemporalConfirmation()
        repeat(4) { confirmation.update(listOf(clover)) }
        repeat(5) { confirmation.update(emptyList()) }
        repeat(3) { assertTrue(confirmation.update(listOf(elsewhere)).confirmed.isEmpty()) }
        assertTrue(confirmation.update(listOf(elsewhere)).confirmed.isNotEmpty())
    }

    @Test fun `two clovers side by side are both confirmed`() {
        val confirmation = TemporalConfirmation()
        repeat(3) { assertTrue(confirmation.update(listOf(clover, elsewhere)).confirmed.isEmpty()) }
        val tracking = confirmation.update(listOf(clover, elsewhere))
        assertEquals(2, tracking.confirmed.size)
        assertEquals(2, tracking.newlyConfirmed.size)
    }

    @Test fun `a second clover is found while the first stays in view`() {
        val confirmation = TemporalConfirmation()
        repeat(4) { confirmation.update(listOf(clover)) }
        repeat(3) { assertEquals(1, confirmation.update(listOf(clover, elsewhere)).confirmed.size) }
        val tracking = confirmation.update(listOf(clover, elsewhere))
        assertEquals(2, tracking.confirmed.size)
        assertEquals(listOf(elsewhere), tracking.newlyConfirmed)
    }

    @Test fun `a confirmed clover is announced once while it stays confirmed`() {
        val confirmation = TemporalConfirmation()
        val announced = (1..10).sumOf { confirmation.update(listOf(clover)).newlyConfirmed.size }
        assertEquals(1, announced)
    }

    @Test fun `a duplicate box of the same clover is not a second clover`() {
        val confirmation = TemporalConfirmation()
        val duplicate = cloverAt(.03f, confidence = .8f)
        repeat(6) {
            val tracking = confirmation.update(listOf(clover, duplicate))
            assertTrue(tracking.confirmed.size <= 1)
            assertTrue(tracking.candidates.size + tracking.confirmed.size <= 1)
        }
    }
}

package com.luckyprospector.app.domain

import com.luckyprospector.app.inference.Detection
import com.luckyprospector.app.inference.DetectionLabel
import com.luckyprospector.app.inference.NormalizedBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharePhotoPolicyTest {

    private fun fourLeaf(confidence: Float, box: NormalizedBox = NormalizedBox(.4f, .4f, .5f, .5f)) =
        Detection(DetectionLabel.FOUR_LEAF_CLOVER, confidence, box)

    private fun clover(confidence: Float, box: NormalizedBox = NormalizedBox(.1f, .1f, .2f, .2f)) =
        Detection(DetectionLabel.CLOVER_PATCH, confidence, box)

    @Test
    fun `a confident four-leaf detection allows a four-leaf pin`() {
        val decision = SharePhotoPolicy.decide(FindingType.FOUR_LEAF_CLOVER, listOf(fourLeaf(.85f)))
        assertEquals(ShareDecision.Allowed(FindingType.FOUR_LEAF_CLOVER, fourLeaf(.85f).box, downgraded = false), decision)
    }

    @Test
    fun `without a confident four-leaf detection the pin becomes a patch`() {
        val decision = SharePhotoPolicy.decide(FindingType.FOUR_LEAF_CLOVER, listOf(fourLeaf(.6f), clover(.8f)))
        decision as ShareDecision.Allowed
        assertEquals(FindingType.CLOVER_PATCH, decision.type)
        assertTrue(decision.downgraded)
    }

    @Test
    fun `a patch needs the classifier to see clovers`() {
        assertEquals(ShareDecision.NoClover, SharePhotoPolicy.decide(FindingType.CLOVER_PATCH, emptyList()))
        assertEquals(ShareDecision.NoClover, SharePhotoPolicy.decide(FindingType.CLOVER_PATCH, listOf(clover(.3f))))
        val decision = SharePhotoPolicy.decide(FindingType.CLOVER_PATCH, listOf(clover(.6f)))
        assertEquals(FindingType.CLOVER_PATCH, (decision as ShareDecision.Allowed).type)
    }

    @Test
    fun `asking for a patch never produces a four-leaf pin`() {
        val decision = SharePhotoPolicy.decide(FindingType.CLOVER_PATCH, listOf(fourLeaf(.95f)))
        assertEquals(FindingType.CLOVER_PATCH, (decision as ShareDecision.Allowed).type)
    }

    @Test
    fun `a patch crop covers every confident clover`() {
        val decision = SharePhotoPolicy.decide(
            FindingType.CLOVER_PATCH,
            listOf(clover(.7f, NormalizedBox(.1f, .1f, .2f, .2f)), clover(.7f, NormalizedBox(.6f, .5f, .7f, .6f))),
        ) as ShareDecision.Allowed
        assertEquals(NormalizedBox(.1f, .1f, .7f, .6f), decision.photoRegion)
    }

    @Test
    fun `the crop is square, contains the detection and stays inside the photo`() {
        val region = NormalizedBox(.45f, .45f, .55f, .55f)
        val rect = SharePhotoPolicy.cropRect(region, width = 4000, height = 3000)
        assertEquals(rect.width, rect.height)
        assertTrue(rect.left <= 1800 && rect.left + rect.width >= 2200)
        assertTrue(rect.top <= 1350 && rect.top + rect.height >= 1650)
        assertTrue(rect.left >= 0 && rect.top >= 0 && rect.left + rect.width <= 4000 && rect.top + rect.height <= 3000)
    }

    @Test
    fun `the crop never shows much more than the clovers`() {
        // A tiny detection still yields a crop far smaller than the whole photo.
        val rect = SharePhotoPolicy.cropRect(NormalizedBox(.5f, .5f, .51f, .51f), width = 4000, height = 3000)
        assertEquals(750, rect.width)
    }

    @Test
    fun `a detection at the edge is clamped into the photo`() {
        val rect = SharePhotoPolicy.cropRect(NormalizedBox(.9f, .9f, 1f, 1f), width = 1000, height = 1000)
        assertEquals(1000, rect.left + rect.width)
        assertEquals(1000, rect.top + rect.height)
    }
}

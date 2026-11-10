package com.luckyprospector.app.domain

import com.luckyprospector.app.inference.Detection
import com.luckyprospector.app.inference.DetectionLabel
import com.luckyprospector.app.inference.NormalizedBox

/** What the classifier allows a finding to be on the shared map. */
sealed interface ShareDecision {
    /** Publish as [type]; [photoRegion] is the part of the photo that may leave the phone. */
    data class Allowed(
        val type: FindingType,
        val photoRegion: NormalizedBox,
        /** True when the user said four-leaf but the classifier only supports "patch". */
        val downgraded: Boolean,
    ) : ShareDecision

    /** The classifier sees no clover at all in the photo: nothing to share. */
    data object NoClover : ShareDecision
}

/**
 * The shared map only shows what the on-device classifier can see in the photo:
 * a four-leaf clover only when it detects one, a patch only when it detects
 * clovers. The user's own classification stays private and untouched.
 *
 * The photo is also reduced to the region around the detections, so houses,
 * gates, plates and people in the background do not undo the 150 m coarsening
 * of the position.
 */
object SharePhotoPolicy {

    /** A still photo gets no temporal confirmation, so the bar is the strict one. */
    const val FOUR_LEAF_CONFIDENCE = .70f
    const val CLOVER_CONFIDENCE = .50f

    /** Context kept around the detections, as a fraction of their size. */
    private const val MARGIN = .35f

    /** Never crop tighter than this share of the shorter photo side. */
    private const val MIN_SIDE = .25f

    fun decide(requested: FindingType, detections: List<Detection>): ShareDecision {
        val fourLeaf = detections
            .filter { it.label == DetectionLabel.FOUR_LEAF_CLOVER && it.confidence >= FOUR_LEAF_CONFIDENCE }
            .maxByOrNull(Detection::confidence)
        val clovers = detections.filter { it.confidence >= CLOVER_CONFIDENCE }

        return when {
            requested == FindingType.FOUR_LEAF_CLOVER && fourLeaf != null ->
                ShareDecision.Allowed(FindingType.FOUR_LEAF_CLOVER, fourLeaf.box, downgraded = false)
            clovers.isNotEmpty() ->
                ShareDecision.Allowed(
                    FindingType.CLOVER_PATCH,
                    union(clovers.map(Detection::box)),
                    downgraded = requested == FindingType.FOUR_LEAF_CLOVER,
                )
            else -> ShareDecision.NoClover
        }
    }

    /**
     * A square region, in pixels, around [region] with some margin, clamped to the
     * photo. Square because the map shows photos in round pins and square cards.
     */
    fun cropRect(region: NormalizedBox, width: Int, height: Int): PixelRect {
        require(width > 0 && height > 0)
        val boxWidth = (region.right - region.left) * width
        val boxHeight = (region.bottom - region.top) * height
        val shorter = minOf(width, height)
        val side = (maxOf(boxWidth, boxHeight) * (1 + 2 * MARGIN))
            .coerceAtLeast(shorter * MIN_SIDE)
            .coerceAtMost(shorter.toFloat())
            .toInt()
            .coerceAtLeast(1)
        val centerX = (region.left + region.right) / 2 * width
        val centerY = (region.top + region.bottom) / 2 * height
        val left = (centerX - side / 2f).toInt().coerceIn(0, width - side)
        val top = (centerY - side / 2f).toInt().coerceIn(0, height - side)
        return PixelRect(left, top, side, side)
    }

    private fun union(boxes: List<NormalizedBox>) = NormalizedBox(
        left = boxes.minOf(NormalizedBox::left),
        top = boxes.minOf(NormalizedBox::top),
        right = boxes.maxOf(NormalizedBox::right),
        bottom = boxes.maxOf(NormalizedBox::bottom),
    )
}

data class PixelRect(val left: Int, val top: Int, val width: Int, val height: Int)

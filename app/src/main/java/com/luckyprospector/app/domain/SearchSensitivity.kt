package com.luckyprospector.app.domain

import com.luckyprospector.app.inference.TemporalConfirmation

/**
 * How eager the search is to call a four-leaf clover, offered to the user as three
 * words rather than a raw confidence number: a threshold alone means little, and
 * what changes the experience is the trade between missed clovers and false alarms.
 *
 * Each level moves the confidence thresholds and how many recent frames must agree.
 * The confidence levels are the calibration the product chose (report T07: precision
 * ~0.64 at 0.7). The frame counts are short on purpose: with 4 of 5 frames the
 * search felt slow to react, so two frames in a row now confirm (one for SENSITIVE).
 */
enum class SearchSensitivity(
    val windowSize: Int,
    val hitsRequired: Int,
    val confirmConfidence: Float,
    val candidateConfidence: Float,
) {
    /** Fewer false alarms: the last 2 frames at 0.8. */
    CAREFUL(windowSize = 2, hitsRequired = 2, confirmConfidence = .80f, candidateConfidence = .60f),

    /** The last 2 frames at 0.7. */
    NORMAL(windowSize = 2, hitsRequired = 2, confirmConfidence = .70f, candidateConfidence = .50f),

    /** Finds more, errs more: a single frame at 0.6. */
    SENSITIVE(windowSize = 1, hitsRequired = 1, confirmConfidence = .60f, candidateConfidence = .45f),
    ;

    fun newConfirmation() = TemporalConfirmation(
        windowSize = windowSize,
        hitsRequired = hitsRequired,
        confidenceThreshold = confirmConfidence,
        candidateThreshold = candidateConfidence,
        candidateHits = minOf(CANDIDATE_HITS, windowSize),
    )

    private companion object {
        /** A weaker sighting is pointed out after two frames, or one if the window is one frame. */
        const val CANDIDATE_HITS = 2
    }
}

package com.luckyprospector.app.inference

data class ConfirmedDetection(val detection: Detection, val consecutiveFrames: Int)

/** What the search screen should say about four-leaf clovers after this frame. */
data class FourLeafTracking(
    /** Seen in a few recent frames at the same place: worth pointing out, not yet announced. */
    val candidates: List<Detection> = emptyList(),
    /** Seen steadily at the same place with high confidence: announced as found. */
    val confirmed: List<ConfirmedDetection> = emptyList(),
    /** The confirmed clovers that became confirmed in this very frame: the moment to celebrate. */
    val newlyConfirmed: List<Detection> = emptyList(),
)

/**
 * Turns single-frame four-leaf detections into a claim the product can make.
 *
 * The model is only right about 45% of the time at its default threshold and about
 * 64% of the time at 0.7 (report T07), so a single frame is announced only when the
 * window is one frame long (the "sensitive" setting). A clover is confirmed when it
 * is seen with high confidence in most of the recent frames **at the same place**:
 * each track follows its box by overlap, so unrelated false positives flickering
 * around the frame never add up to one.
 *
 * Several clovers are tracked at once, each with its own history: two four-leaf
 * clovers side by side are both found, and one does not have to leave the frame
 * before the other is noticed.
 */
class TemporalConfirmation(
    private val windowSize: Int = 5,
    private val hitsRequired: Int = 4,
    private val confidenceThreshold: Float = .70f,
    private val candidateThreshold: Float = .50f,
    private val candidateHits: Int = 2,
    private val minOverlap: Float = .30f,
    private val maxTracks: Int = 8,
) {
    init {
        require(windowSize > 0 && hitsRequired in 1..windowSize && candidateHits in 1..windowSize)
        require(candidateThreshold <= confidenceThreshold)
        require(maxTracks > 0)
    }

    private class Track(var box: NormalizedBox) {
        val history = ArrayDeque<Detection?>()
        /** Already announced: a flicker of the same clover is not celebrated again. */
        var announced = false
    }

    private val tracks = mutableListOf<Track>()

    fun update(detections: List<Detection>): FourLeafTracking {
        val fourLeaf = detections.filter {
            it.label == DetectionLabel.FOUR_LEAF_CLOVER && it.confidence >= candidateThreshold
        }

        // Pair tracks and detections by overlap, best overlap first, one to one.
        val pairs = tracks.flatMap { track ->
            fourLeaf.mapNotNull { detection ->
                val overlap = detection.box.intersectionOverUnion(track.box)
                if (overlap >= minOverlap) Triple(track, detection, overlap) else null
            }
        }.sortedByDescending { it.third }
        val matches = HashMap<Track, Detection>()
        val used = HashSet<Detection>()
        pairs.forEach { (track, detection) ->
            if (track !in matches && detection !in used) {
                matches[track] = detection
                used += detection
            }
        }

        // A detection that overlaps a track already taken is a duplicate box of the
        // same clover, not a new one.
        fourLeaf
            .filter { it !in used }
            .sortedByDescending(Detection::confidence)
            .forEach { detection ->
                val duplicate = tracks.any { detection.box.intersectionOverUnion(it.box) >= minOverlap }
                if (!duplicate && tracks.size < maxTracks) {
                    val track = Track(detection.box)
                    tracks += track
                    matches[track] = detection
                }
            }

        val candidates = mutableListOf<Detection>()
        val confirmed = mutableListOf<ConfirmedDetection>()
        val newlyConfirmed = mutableListOf<Detection>()
        tracks.forEach { track ->
            val match = matches[track]
            // Follow the clover as the phone moves, one frame at a time.
            if (match != null) track.box = match.box
            track.history.addLast(match)
            while (track.history.size > windowSize) track.history.removeFirst()
            if (match == null) return@forEach

            val strongHits = track.history.count { it != null && it.confidence >= confidenceThreshold }
            if (strongHits >= hitsRequired) {
                confirmed += ConfirmedDetection(match, strongHits)
                if (!track.announced) {
                    track.announced = true
                    newlyConfirmed += match
                }
            } else if (track.history.count { it != null } >= candidateHits) {
                candidates += match
            }
        }
        tracks.removeAll { track -> track.history.all { it == null } }

        return FourLeafTracking(
            candidates = candidates.sortedByDescending(Detection::confidence),
            confirmed = confirmed.sortedByDescending { it.detection.confidence },
            newlyConfirmed = newlyConfirmed,
        )
    }

    fun reset() = tracks.clear()
}

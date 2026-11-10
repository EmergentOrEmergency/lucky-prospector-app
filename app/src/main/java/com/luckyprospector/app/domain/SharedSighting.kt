package com.luckyprospector.app.domain

import java.time.Instant

/**
 * A sighting as it appears on the map shared with every user of the app.
 *
 * Note what is absent: no owner identity, no wish, no photo, no precise position,
 * no creation time of the original finding. A shared document is readable by
 * every signed-in client, so anything present here is public in practice no
 * matter how the UI chooses to render it -- the protection has to be in what
 * gets written, not in what gets displayed.
 *
 * Deletion stays possible without an owner field: the app keeps a private claim
 * document under the user's own subtree, and the security rules allow deleting a
 * shared sighting only when the matching claim exists.
 */
data class SharedSighting(
    val id: String,
    val location: GeoPoint,
    val cellSizeMeters: Int,
    val type: FindingType,
    val sharedAt: Instant,
    /**
     * How many people have confirmed the sighting is still there. A count only:
     * who confirmed is never recorded publicly, so contributing to the map does
     * not build the trail that anonymous pins exist to prevent.
     */
    val confirmations: Int = 0,
    val lastConfirmedAt: Instant? = null,
    /** Anonymous aggregate only; reporter identities live in private receipts. */
    val reportCount: Int = 0,
    /**
     * How many different people said the place is gone. Pins never expire on their
     * own: at [GONE_HIDE_THRESHOLD] the pin leaves the map, logically, without the
     * document being deleted.
     */
    val goneCount: Int = 0,
    /**
     * Whether a cropped, EXIF-free photo is stored beside the pin. The photo lives
     * in its own document, readable only while the pin itself is not hidden.
     */
    val hasPhoto: Boolean = false,
) {
    init {
        require(cellSizeMeters >= GeoCoarsening.MIN_CELL_METERS) {
            "a shared sighting must be coarsened to at least " +
                "${GeoCoarsening.MIN_CELL_METERS} m, was $cellSizeMeters"
        }
        require(confirmations >= 0) { "confirmations cannot be negative, was $confirmations" }
        require(reportCount >= 0) { "reportCount cannot be negative, was $reportCount" }
        require(goneCount >= 0) { "goneCount cannot be negative, was $goneCount" }
    }

    val isHiddenByReports: Boolean get() = reportCount >= REPORT_HIDE_THRESHOLD

    /** Mirrors hiddenFor() in firestore.rules, which recomputes it on every write. */
    val isHidden: Boolean get() = isHiddenByReports || goneCount >= GONE_HIDE_THRESHOLD

    /** Derived from the coarsened position, so it reveals nothing the coordinates do not. */
    val geohash: String get() = Geohash.encode(location.latitude, location.longitude)

    companion object {
        /** Mirrored in firestore.rules, which refuses to serve pins at or above it. */
        const val REPORT_HIDE_THRESHOLD = 3

        /** Two different people, so that one mistaken or spiteful account cannot remove a pin. */
        const val GONE_HIDE_THRESHOLD = 2
        /**
         * Builds the public form of a finding. The caller supplies the id so the
         * private claim document and the public document can be written in one
         * batch under the same key.
         */
        fun from(
            id: String,
            finding: Finding,
            cellMeters: Int = GeoCoarsening.DEFAULT_CELL_METERS,
            sharedAt: Instant = Instant.now(),
            hasPhoto: Boolean = false,
        ): SharedSighting? {
            val precise = finding.location ?: return null
            return SharedSighting(
                id = id,
                location = GeoCoarsening.coarsen(precise, cellMeters),
                cellSizeMeters = cellMeters,
                type = finding.type,
                sharedAt = sharedAt,
                hasPhoto = hasPhoto,
            )
        }
    }
}

package com.luckyprospector.app.domain

import kotlinx.coroutines.flow.Flow

/**
 * Writes to the shared map. Local search and saving never depend on this boundary.
 * Implementations must create the public sighting and its private ownership claim
 * atomically, and do the same for confirmations.
 */
interface SharedSightingRepository {
    /**
     * Pins whose geohash starts with one of [areaPrefixes] (see [Geohash.coveringPrefixes]).
     * Bounded by area so that opening the map never downloads the whole collection.
     * Removed pins (reports or "gone") are not served at all.
     */
    fun observeSharedSightings(areaPrefixes: Set<String>): Flow<List<SharedSighting>>
    fun observeOwnedSightingIds(): Flow<Set<String>>
    fun observeConfirmedSightingIds(): Flow<Set<String>>
    fun observeReportedSightingIds(): Flow<Set<String>>
    fun observeGoneSightingIds(): Flow<Set<String>>
    /**
     * Idempotent: when this account's claim and the public pin already exist, as
     * after a lost response, the existing publication is reported as success
     * instead of failing on the second write.
     */
    suspend fun publish(finding: Finding, photoJpeg: ByteArray? = null): SharedSighting

    /** The shared photo of a pin, or null when it has none or the pin is hidden. */
    suspend fun loadPhoto(sightingId: String): ByteArray?
    suspend fun updateType(sightingId: String, type: FindingType)
    suspend fun confirm(sighting: SharedSighting)
    suspend fun report(sighting: SharedSighting, reason: SightingReportReason)

    /** "It is not there any more." One per account; the author cannot send it. */
    suspend fun markGone(sighting: SharedSighting)
    suspend fun withdraw(sightingId: String)

    /**
     * Withdraws every pin this account published, with its photo, before the account
     * is deleted: afterwards no claim would be left to prove ownership. Returns how
     * many pins were withdrawn.
     */
    suspend fun withdrawAll(): Int

    /**
     * Takes back every confirmation, report and "not there any more" this account
     * gave, deleting the private receipts. Each vote leaves the public count in the
     * same batch, so nothing is counted twice and nothing links back to the account.
     */
    suspend fun withdrawVotes()
}

class SharingRequiresSignInException : IllegalStateException("Sign in is required only to share")

class FindingHasNoLocationException : IllegalArgumentException("A finding without a location cannot be shared")

/**
 * Raised before writing when the account has used its daily publications.
 * The same limit is enforced by firestore.rules; this only gives a clear message.
 */
class PublishLimitReachedException : IllegalStateException("Daily sharing limit reached")

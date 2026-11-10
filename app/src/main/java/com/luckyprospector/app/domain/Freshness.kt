package com.luckyprospector.app.domain

import java.time.Duration
import java.time.Instant

enum class FreshnessLevel {
    /** Recently published or recently confirmed by someone. Drawn at full strength. */
    FRESH,

    /** Getting old with nobody confirming. Drawn faded, still useful as a hint. */
    FADING,

    /** Old and unconfirmed for a long time. Still shown, drawn faded. */
    STALE,

    /**
     * Very old with nobody confirming. Still shown: age alone never removes a pin,
     * only people saying it is gone do (see SharedSighting.GONE_HIDE_THRESHOLD).
     */
    EXPIRED,
}

data class Freshness(
    val level: FreshnessLevel,
    val ageDays: Long,
    val confirmations: Int,
)

/**
 * How long a shared sighting stays useful.
 *
 * Freshness only describes a pin; it never removes one. A patch can last for
 * years, so a pin leaves the map only when two different people say the place is
 * gone. A mown meadow is still not abuse: "not there any more" is its own signal,
 * separate from abuse reports.
 *
 * Confirmations are what keep a pin alive: someone standing in front of the same
 * clover patch is the only evidence that it is still there. A confirmation never
 * records who gave it -- only the count -- so contributing does not build the
 * trail that anonymity exists to prevent.
 */
object FreshnessPolicy {

    /**
     * Lifetimes differ by what the pin claims.
     *
     * A single four-leaf clover is usually picked by the person who found it, so
     * the pin never meant "this clover is here" -- it meant "clovers with four
     * leaves turn up around here", which is a weaker claim that ages faster.
     *
     * A patch is a place, and places last a season or two unless something
     * happens to them.
     */
    private data class Lifetime(val fresh: Duration, val fading: Duration, val stale: Duration)

    private val singleClover = Lifetime(
        fresh = Duration.ofDays(30),
        fading = Duration.ofDays(90),
        stale = Duration.ofDays(180),
    )

    private val patch = Lifetime(
        fresh = Duration.ofDays(60),
        fading = Duration.ofDays(180),
        stale = Duration.ofDays(365),
    )

    /**
     * Each confirmation buys the pin extra life, with diminishing returns: the
     * first confirmation is strong evidence, the twentieth adds little and should
     * not make a pin effectively immortal.
     */
    private const val DAYS_PER_CONFIRMATION = 15L
    private const val MAX_CONFIRMATION_BONUS_DAYS = 90L

    fun evaluate(
        type: FindingType,
        sharedAt: Instant,
        lastConfirmedAt: Instant? = null,
        confirmations: Int = 0,
        now: Instant = Instant.now(),
    ): Freshness {
        require(confirmations >= 0) { "confirmations cannot be negative, was $confirmations" }

        // The clock restarts at the last confirmation: a pin confirmed yesterday is
        // current regardless of when it was first published.
        val since = maxOf(sharedAt, lastConfirmedAt ?: sharedAt)
        val elapsed = Duration.between(since, now).coerceAtLeast(Duration.ZERO)

        val lifetime = lifetimeOf(type)
        val bonus = confirmationBonus(confirmations)

        val level = when {
            elapsed <= lifetime.fresh + bonus -> FreshnessLevel.FRESH
            elapsed <= lifetime.fading + bonus -> FreshnessLevel.FADING
            elapsed <= lifetime.stale + bonus -> FreshnessLevel.STALE
            else -> FreshnessLevel.EXPIRED
        }

        return Freshness(
            level = level,
            ageDays = Duration.between(sharedAt, now).coerceAtLeast(Duration.ZERO).toDays(),
            confirmations = confirmations,
        )
    }

    private fun lifetimeOf(type: FindingType): Lifetime = when (type) {
        FindingType.FOUR_LEAF_CLOVER -> singleClover
        FindingType.CLOVER_PATCH -> patch
    }

    private fun confirmationBonus(confirmations: Int): Duration = Duration.ofDays(
        (confirmations * DAYS_PER_CONFIRMATION).coerceAtMost(MAX_CONFIRMATION_BONUS_DAYS),
    )

    fun evaluate(sighting: SharedSighting, now: Instant = Instant.now()): Freshness = evaluate(
        type = sighting.type,
        sharedAt = sighting.sharedAt,
        lastConfirmedAt = sighting.lastConfirmedAt,
        confirmations = sighting.confirmations,
        now = now,
    )
}

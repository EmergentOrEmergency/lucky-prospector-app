package com.luckyprospector.app.domain

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FreshnessPolicyTest {

    private val now: Instant = Instant.parse("2026-09-22T10:00:00Z")

    private fun daysAgo(days: Long): Instant = now.minus(Duration.ofDays(days))

    @Test
    fun `a sighting published today is fresh`() {
        val freshness = FreshnessPolicy.evaluate(
            type = FindingType.CLOVER_PATCH,
            sharedAt = now,
            now = now,
        )

        assertEquals(FreshnessLevel.FRESH, freshness.level)
        assertEquals(0L, freshness.ageDays)
    }

    @Test
    fun `an unconfirmed patch fades, then goes stale, then expires`() {
        fun levelAfter(days: Long) = FreshnessPolicy.evaluate(
            type = FindingType.CLOVER_PATCH,
            sharedAt = daysAgo(days),
            now = now,
        ).level

        assertEquals(FreshnessLevel.FRESH, levelAfter(30))
        assertEquals(FreshnessLevel.FADING, levelAfter(120))
        assertEquals(FreshnessLevel.STALE, levelAfter(300))
        assertEquals(FreshnessLevel.EXPIRED, levelAfter(400))
    }

    @Test
    fun `a single clover ages faster than a patch`() {
        // The pin for a picked clover only ever claimed "four-leaf clovers turn up
        // around here", which is a weaker claim than "there is a patch at this spot".
        val age = daysAgo(45)

        assertEquals(
            FreshnessLevel.FADING,
            FreshnessPolicy.evaluate(FindingType.FOUR_LEAF_CLOVER, age, now = now).level,
        )
        assertEquals(
            FreshnessLevel.FRESH,
            FreshnessPolicy.evaluate(FindingType.CLOVER_PATCH, age, now = now).level,
        )
    }

    @Test
    fun `a recent confirmation restarts the clock`() {
        val stale = FreshnessPolicy.evaluate(
            type = FindingType.CLOVER_PATCH,
            sharedAt = daysAgo(300),
            now = now,
        )
        val confirmedYesterday = FreshnessPolicy.evaluate(
            type = FindingType.CLOVER_PATCH,
            sharedAt = daysAgo(300),
            lastConfirmedAt = daysAgo(1),
            confirmations = 1,
            now = now,
        )

        assertEquals(FreshnessLevel.STALE, stale.level)
        assertEquals(FreshnessLevel.FRESH, confirmedYesterday.level)
    }

    @Test
    fun `age is always measured from publication, not from the last confirmation`() {
        // The level restarts on confirmation, but the reported age must not: the UI
        // should be able to say "trovato 300 giorni fa, confermato ieri".
        val freshness = FreshnessPolicy.evaluate(
            type = FindingType.CLOVER_PATCH,
            sharedAt = daysAgo(300),
            lastConfirmedAt = daysAgo(1),
            confirmations = 1,
            now = now,
        )

        assertEquals(300L, freshness.ageDays)
    }

    @Test
    fun `confirmations extend life but cannot make a pin immortal`() {
        fun levelWith(confirmations: Int) = FreshnessPolicy.evaluate(
            type = FindingType.CLOVER_PATCH,
            sharedAt = daysAgo(500),
            lastConfirmedAt = daysAgo(500),
            confirmations = confirmations,
            now = now,
        ).level

        // The bonus is capped, so a pin nobody has visited in over a year expires
        // however popular it once was.
        assertEquals(FreshnessLevel.EXPIRED, levelWith(0))
        assertEquals(FreshnessLevel.EXPIRED, levelWith(500))
    }

    @Test
    fun `a clock skewed into the future does not produce a negative age`() {
        val freshness = FreshnessPolicy.evaluate(
            type = FindingType.CLOVER_PATCH,
            sharedAt = now.plus(Duration.ofDays(2)),
            now = now,
        )

        assertEquals(0L, freshness.ageDays)
        assertEquals(FreshnessLevel.FRESH, freshness.level)
    }

    @Test
    fun `a negative confirmation count is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            FreshnessPolicy.evaluate(
                type = FindingType.CLOVER_PATCH,
                sharedAt = now,
                confirmations = -1,
                now = now,
            )
        }
    }

    @Test
    fun `the sighting overload agrees with the explicit one`() {
        val sighting = SharedSighting(
            id = "s1",
            location = GeoPoint(44.6, 10.9, 150f),
            cellSizeMeters = 150,
            type = FindingType.CLOVER_PATCH,
            sharedAt = daysAgo(200),
            confirmations = 2,
            lastConfirmedAt = daysAgo(10),
        )

        assertEquals(
            FreshnessPolicy.evaluate(
                type = sighting.type,
                sharedAt = sighting.sharedAt,
                lastConfirmedAt = sighting.lastConfirmedAt,
                confirmations = sighting.confirmations,
                now = now,
            ),
            FreshnessPolicy.evaluate(sighting, now),
        )
    }
}

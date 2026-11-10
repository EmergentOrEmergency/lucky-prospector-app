package com.luckyprospector.app.data.remote

import com.google.firebase.Timestamp
import com.luckyprospector.app.data.remote.FirestoreSharedSightingRepository.Companion.MAX_PUBLICATIONS_PER_DAY
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishQuotaTest {

    private val now = Instant.parse("2026-09-26T12:00:00Z")

    private fun quota(startedAgo: Duration, count: Int) = mapOf<String, Any?>(
        "windowStart" to now.minus(startedAgo).let { Timestamp(it.epochSecond, it.nano) },
        "count" to count,
    )

    @Test
    fun `first publication opens a window`() {
        val write = nextQuotaWrite(null, now)!!
        assertTrue(write.startsNewWindow)
        assertEquals(1, write.fields["count"])
    }

    @Test
    fun `publications inside the window increment the counter`() {
        val write = nextQuotaWrite(quota(Duration.ofHours(3), MAX_PUBLICATIONS_PER_DAY - 1), now)!!
        assertFalse(write.startsNewWindow)
        assertFalse(write.fields.containsKey("windowStart"))
    }

    @Test
    fun `the limit is refused before writing`() {
        assertNull(nextQuotaWrite(quota(Duration.ofHours(3), MAX_PUBLICATIONS_PER_DAY), now))
    }

    @Test
    fun `a window just over a day old by the device clock is still treated as open`() {
        // The server may not agree yet; a new window it would refuse must not be sent.
        assertNull(nextQuotaWrite(quota(Duration.ofDays(1).plusMinutes(5), MAX_PUBLICATIONS_PER_DAY), now))
    }

    @Test
    fun `an old window is replaced`() {
        val write = nextQuotaWrite(quota(Duration.ofHours(25), MAX_PUBLICATIONS_PER_DAY), now)!!
        assertTrue(write.startsNewWindow)
    }
}

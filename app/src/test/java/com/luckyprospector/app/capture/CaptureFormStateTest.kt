package com.luckyprospector.app.capture

import com.luckyprospector.app.domain.FindingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureFormStateTest {
    @Test fun blankWishBecomesNull() {
        assertNull(CaptureFormState(wish = "  \n ").normalizedWish())
    }

    @Test fun wishIsTrimmedAndSelectedTypeIsPreserved() {
        val state = CaptureFormState(FindingType.CLOVER_PATCH, "  Più fortuna  ")
        assertEquals("Più fortuna", state.normalizedWish())
        assertEquals(FindingType.CLOVER_PATCH, state.type)
    }

    @Test fun wishInputIsLimitedToRepositoryMaximum() {
        assertEquals(500, CaptureFormState().withWish("x".repeat(501)).wish.length)
    }
}

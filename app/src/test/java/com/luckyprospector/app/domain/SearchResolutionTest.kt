package com.luckyprospector.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchResolutionTest {
    @Test fun `a new installation or invalid preference starts in full hd`() {
        assertEquals(SearchResolution.FULL_HD, SearchResolution.fromStored(null))
        assertEquals(SearchResolution.FULL_HD, SearchResolution.fromStored("unknown"))
        assertEquals(1920, SearchResolution.DEFAULT.width)
        assertEquals(1080, SearchResolution.DEFAULT.height)
    }

    @Test fun `all saved choices including legacy resolutions are preserved`() {
        for (resolution in SearchResolution.entries)
            assertEquals(resolution, SearchResolution.fromStored(resolution.name))
    }
}

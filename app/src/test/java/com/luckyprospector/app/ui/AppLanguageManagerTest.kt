package com.luckyprospector.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageManagerTest {
    @Test
    fun explicitLanguageOverridesDeviceLanguage() {
        assertEquals("it", AppLanguageManager.resolveLanguageTag(AppLanguage.ITALIAN, listOf("de-DE")))
    }

    @Test
    fun systemModeUsesFirstSupportedDeviceLanguage() {
        assertEquals("fr", AppLanguageManager.resolveLanguageTag(AppLanguage.SYSTEM, listOf("fr-CA", "en-US")))
    }

    @Test
    fun systemModeSkipsUnsupportedLanguages() {
        assertEquals("es", AppLanguageManager.resolveLanguageTag(AppLanguage.SYSTEM, listOf("ja-JP", "es-MX")))
    }

    @Test
    fun unsupportedDeviceLanguagesFallBackToEnglish() {
        assertEquals("en", AppLanguageManager.resolveLanguageTag(AppLanguage.SYSTEM, listOf("ja-JP", "zh-Hans")))
    }
}

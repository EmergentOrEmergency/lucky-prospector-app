package com.luckyprospector.app.ui

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.LocaleList
import androidx.annotation.StringRes
import com.luckyprospector.app.R
import java.util.Locale

enum class AppLanguage(
    val preferenceValue: String,
    val languageTag: String?,
    @param:StringRes val displayNameRes: Int,
) {
    SYSTEM("system", null, R.string.language_system),
    ENGLISH("en", "en", R.string.language_english),
    ITALIAN("it", "it", R.string.language_italian),
    FRENCH("fr", "fr", R.string.language_french),
    GERMAN("de", "de", R.string.language_german),
    SPANISH("es", "es", R.string.language_spanish),
}

object AppLanguageManager {
    private const val PREFERENCES = "app_language"
    private const val SELECTED_LANGUAGE = "selected_language"
    private val supportedByTag = AppLanguage.entries
        .filter { it.languageTag != null }
        .associateBy { requireNotNull(it.languageTag) }

    fun wrap(base: Context): Context {
        val locale = resolveLocale(selectedLanguage(base))
        val configuration = base.resources.configuration
        val localized = android.content.res.Configuration(configuration).apply {
            setLocales(LocaleList(locale))
        }
        return base.createConfigurationContext(localized)
    }

    fun selectedLanguage(context: Context): AppLanguage {
        val stored = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(SELECTED_LANGUAGE, AppLanguage.SYSTEM.preferenceValue)
        return AppLanguage.entries.firstOrNull { it.preferenceValue == stored } ?: AppLanguage.SYSTEM
    }

    fun setLanguage(activity: Activity, language: AppLanguage) {
        activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(SELECTED_LANGUAGE, language.preferenceValue)
            .apply()
        activity.recreate()
    }

    private fun resolveLocale(language: AppLanguage): Locale {
        val systemLocales = Resources.getSystem().configuration.locales
        val systemTags = buildList {
            for (index in 0 until systemLocales.size()) add(systemLocales[index].toLanguageTag())
        }
        return Locale.forLanguageTag(resolveLanguageTag(language, systemTags))
    }

    internal fun resolveLanguageTag(language: AppLanguage, systemLanguageTags: List<String>): String {
        language.languageTag?.let { return it }
        return systemLanguageTags.firstNotNullOfOrNull { tag ->
            val normalized = Locale.forLanguageTag(tag).language
            supportedByTag[normalized]?.languageTag
        } ?: AppLanguage.ENGLISH.languageTag!!
    }
}

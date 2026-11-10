package com.luckyprospector.app.ui

import android.content.Context
import com.luckyprospector.app.domain.SearchResolution
import com.luckyprospector.app.domain.SearchSensitivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Settings(
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val sensitivity: SearchSensitivity = SearchSensitivity.NORMAL,
    val resolution: SearchResolution = SearchResolution.DEFAULT,
    /** "Did you know?" in the findings list, open or folded to its title. */
    val factsExpanded: Boolean = true,
    /** The shared map's rules were accepted, before the first publication (Play UGC policy). */
    val communityRulesAccepted: Boolean = false,
)

/**
 * User preferences kept on the device only. A plain SharedPreferences file, like
 * the language choice: three values do not justify another storage library.
 */
class AppSettings private constructor(context: Context) {

    private val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        preferences.edit()
            .putBoolean(SOUND, next.sound)
            .putBoolean(VIBRATION, next.vibration)
            .putString(SENSITIVITY, next.sensitivity.name)
            .putString(RESOLUTION, next.resolution.name)
            .putBoolean(FACTS_EXPANDED, next.factsExpanded)
            .putBoolean(COMMUNITY_RULES_ACCEPTED, next.communityRulesAccepted)
            .apply()
        _settings.value = next
    }

    private fun read() = Settings(
        sound = preferences.getBoolean(SOUND, true),
        vibration = preferences.getBoolean(VIBRATION, true),
        sensitivity = preferences.getString(SENSITIVITY, null)
            ?.let { stored -> SearchSensitivity.entries.firstOrNull { it.name == stored } }
            ?: SearchSensitivity.NORMAL,
        resolution = SearchResolution.fromStored(preferences.getString(RESOLUTION, null)),
        factsExpanded = preferences.getBoolean(FACTS_EXPANDED, true),
        communityRulesAccepted = preferences.getBoolean(COMMUNITY_RULES_ACCEPTED, false),
    )

    companion object {
        private const val FILE = "lucky_settings"
        private const val SOUND = "sound"
        private const val VIBRATION = "vibration"
        private const val SENSITIVITY = "sensitivity"
        private const val RESOLUTION = "resolution"
        private const val FACTS_EXPANDED = "facts_expanded"
        private const val COMMUNITY_RULES_ACCEPTED = "community_rules_accepted"

        @Volatile private var instance: AppSettings? = null

        fun get(context: Context): AppSettings = instance ?: synchronized(this) {
            instance ?: AppSettings(context.applicationContext).also { instance = it }
        }
    }
}

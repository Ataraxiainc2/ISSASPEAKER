package com.issasoda.issaspeaker.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun of(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/**
 * Plain user settings (no secrets). SharedPreferences on purpose: read synchronously at start, so the first
 * frame already has the right theme. The language is not here: Android keeps it (LocaleManager).
 */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val theme = MutableStateFlow(ThemeMode.of(prefs.getString(KEY_THEME, null)))

    val themeMode: StateFlow<ThemeMode> = theme.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.key) }
        theme.value = mode
    }

    /** Back to defaults (part of "Erase everything"). */
    fun clear() {
        prefs.edit { clear() }
        theme.value = ThemeMode.SYSTEM
    }

    private companion object {
        const val FILE = "settings"
        const val KEY_THEME = "theme_mode"
    }
}

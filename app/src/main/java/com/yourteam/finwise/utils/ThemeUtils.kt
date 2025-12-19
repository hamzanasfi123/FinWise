package com.yourteam.finwise.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {
    const val THEME_LIGHT = "Light"
    const val THEME_DARK = "Dark"
    const val THEME_AUTO = "Auto"

    fun applyTheme(theme: String) {
        println("🔄 DEBUG: Applying theme: $theme")
        when (theme) {
            THEME_LIGHT -> {
                println("🌞 DEBUG: Setting MODE_NIGHT_NO")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            THEME_DARK -> {
                println("🌙 DEBUG: Setting MODE_NIGHT_YES")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            THEME_AUTO -> {
                println("⚡ DEBUG: Setting MODE_NIGHT_FOLLOW_SYSTEM")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    fun getCurrentTheme(context: Context): String {
        // USE ENCRYPTED SHAREDPREFERENCES like SettingsFragment
        val prefs = SecurityUtils.getEncryptedPreferences(context)
        val theme = prefs.getString("theme", THEME_LIGHT) ?: THEME_LIGHT
        println("📁 DEBUG: Current theme from EncryptedSharedPreferences: $theme")
        return theme
    }

    fun saveTheme(context: Context, theme: String) {
        println("💾 DEBUG: Saving theme to EncryptedSharedPreferences: $theme")
        val prefs = SecurityUtils.getEncryptedPreferences(context)
        prefs.edit().putString("theme", theme).apply()
    }

    fun initializeTheme(context: Context) {
        val currentTheme = getCurrentTheme(context)
        println("🚀 DEBUG: Initializing app with theme: $currentTheme")
        applyTheme(currentTheme)
    }
}
package io.github.hronek.filipneriquotes.data

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val FILE = "settings"
    private const val KEY_LANGUAGE = "language" // values: auto, cs, pl, it, de, es, fr, en
    private const val KEY_NOTIF_ENABLED = "notif_enabled"
    private const val KEY_NOTIF_HOUR = "notif_hour"
    private const val KEY_NOTIF_MINUTE = "notif_minute"
    private const val KEY_PREFACE_SHOWN = "preface_shown"
    private const val KEY_THEME = "app_theme" // values: system, light, dark
    private const val KEY_NAV_BUTTONS = "nav_buttons" // show navigation buttons on main screen

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getLanguage(ctx: Context): String = prefs(ctx).getString(KEY_LANGUAGE, "auto") ?: "auto"
    fun setLanguage(ctx: Context, lang: String) { prefs(ctx).edit().putString(KEY_LANGUAGE, lang).apply() }

    fun isNotifEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_NOTIF_ENABLED, false)
    fun setNotifEnabled(ctx: Context, enabled: Boolean) { prefs(ctx).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply() }

    fun getNotifTime(ctx: Context): Pair<Int, Int> =
        prefs(ctx).let { it.getInt(KEY_NOTIF_HOUR, 8) to it.getInt(KEY_NOTIF_MINUTE, 0) }
    fun setNotifTime(ctx: Context, hour: Int, minute: Int) {
        prefs(ctx).edit().putInt(KEY_NOTIF_HOUR, hour).putInt(KEY_NOTIF_MINUTE, minute).apply()
    }

    fun isPrefaceShown(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_PREFACE_SHOWN, false)
    fun setPrefaceShown(ctx: Context, shown: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_PREFACE_SHOWN, shown).apply()
    }

    fun getTheme(ctx: Context): String = prefs(ctx).getString(KEY_THEME, "system") ?: "system"
    fun setTheme(ctx: Context, theme: String) { prefs(ctx).edit().putString(KEY_THEME, theme).apply() }

    fun isNavButtonsEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_NAV_BUTTONS, false)
    fun setNavButtonsEnabled(ctx: Context, enabled: Boolean) { prefs(ctx).edit().putBoolean(KEY_NAV_BUTTONS, enabled).apply() }
}


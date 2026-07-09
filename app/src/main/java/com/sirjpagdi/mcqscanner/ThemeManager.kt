package com.sirjpagdi.mcqscanner

import android.app.Activity

/**
 * Call ThemeManager.apply(this) at the very start of onCreate,
 * BEFORE super.onCreate() and setContentView().
 */
object ThemeManager {

    fun apply(activity: Activity) {
        val theme = Prefs.loadTheme(activity)
        val styleRes = when (theme) {
            Prefs.AppTheme.LIGHT -> R.style.Theme_MCQScanner_Light
            Prefs.AppTheme.DARK -> R.style.Theme_MCQScanner_Dark
            Prefs.AppTheme.BLACK -> R.style.Theme_MCQScanner_Black
        }
        activity.setTheme(styleRes)
    }

    /** Cycles Light -> Dark -> Black -> Light, saves it, and returns the new theme. */
    fun cycle(activity: Activity): Prefs.AppTheme {
        val current = Prefs.loadTheme(activity)
        val next = when (current) {
            Prefs.AppTheme.LIGHT -> Prefs.AppTheme.DARK
            Prefs.AppTheme.DARK -> Prefs.AppTheme.BLACK
            Prefs.AppTheme.BLACK -> Prefs.AppTheme.LIGHT
        }
        Prefs.saveTheme(activity, next)
        return next
    }

    fun iconFor(theme: Prefs.AppTheme): String = when (theme) {
        Prefs.AppTheme.LIGHT -> "☀"
        Prefs.AppTheme.DARK -> "☾"
        Prefs.AppTheme.BLACK -> "●"
    }
}

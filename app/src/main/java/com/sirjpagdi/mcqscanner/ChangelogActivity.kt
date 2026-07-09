package com.sirjpagdi.mcqscanner

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ChangelogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changelog)

        findViewById<TextView>(R.id.tvChangelog).text = CHANGELOG_TEXT
        findViewById<Button>(R.id.btnClose).setOnClickListener { finish() }
    }

    companion object {
        // Keep this in sync with CHANGELOG.md in the repo root.
        const val CHANGELOG_TEXT = """v0.9 — Development build (current)

• Redesigned setup screen: test name, question count, choice range, and paper size all on one screen
• Save Test / Load Test — keep multiple named tests and reload them anytime
• Scan Answer Key Instead — bubble the key on a printed sheet and scan it in, instead of tapping it by hand
• Generate & Share PDF — export a printable, pre-formatted answer sheet straight from your settings
• Light, Dark, and Black themes — tap the icon top-right of the home screen to cycle
• Corner-alignment guide while scanning, with haptic feedback on capture
• Real Tarlac National High School logo and colors

v0.0 — Initial development build

• Tap-to-select answer key entry
• Core scanning flow: Template → Answer Key → Scan → Corner alignment → Results
• Maroon & gold placeholder theme
"""
    }
}

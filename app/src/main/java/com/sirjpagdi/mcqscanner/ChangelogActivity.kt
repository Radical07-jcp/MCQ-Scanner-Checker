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
        const val CHANGELOG_TEXT = """v0.3 — Development build (current)

• Answer sheet and grading now share one geometry source, fixing the main cause of inaccurate scans — bubble positions no longer drift out of sync between what's printed and what's read back
• Fixed a crash on capture caused by OpenCV's native library being loaded from multiple places at once
• Removed the manual drag-to-align step — scanning goes straight from capture to grading
• New corner-square guide: 4 brackets you align the sheet's printed corners into, turning green per corner as they lock in
• Removed paper size options (Full/Half/Quarter) — one consistent layout now
• Answer sheet: actual test title instead of "MCQ Template", student info now in a bordered table instead of underscores
• Preview screen before generating/sharing the answer sheet PDF
• Removed Export CSV from the results screen

v0.2 — OpenCV live-scan pass

• Attempted live camera contour detection for sheet alignment
• Reintroduced saved-results history (not yet reachable from the home screen)

v0.1 — TNHS branding & redesign

• Redesigned setup screen: test name, question count, choice range, and paper size all on one screen
• Save Test / Load Test — keep multiple named tests and reload them anytime
• Scan Answer Key Instead — bubble the key on a printed sheet and scan it in, instead of tapping it by hand
• Generate & Share PDF
• Light, Dark, and Black themes
• Real Tarlac National High School logo and school facade watermark
• In-app changelog (this page, opened from the version number — no browser needed)

v0.0 — Initial development build

• Tap-to-select answer key entry
• Core scanning flow: Template → Answer Key → Scan → Corner alignment → Results
• Maroon & gold placeholder theme
"""
    }
}

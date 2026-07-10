package com.sirjpagdi.mcqscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Scan-and-go by design: shows the score immediately and offers a shortcut
 * straight back into scanning the next sheet. Nothing is kept in the app
 * between sheets.
 */
class ResultsActivity : AppCompatActivity() {

    companion object {
        var pendingDetected: List<String>? = null
    }

    private var detected: List<String> = emptyList()
    private var key: List<String> = emptyList()
    private var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        detected = pendingDetected ?: emptyList()
        key = Prefs.loadAnswerKey(this) ?: emptyList()
        pendingDetected = null

        if (detected.isEmpty() || key.isEmpty()) {
            Toast.makeText(this, "No scan data found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val tvScore = findViewById<TextView>(R.id.tvScore)
        val tvDetail = findViewById<TextView>(R.id.tvDetail)

        score = 0
        val sb = StringBuilder()
        for (i in key.indices) {
            val correct = key.getOrElse(i) { "" }
            val given = detected.getOrElse(i) { "" }
            val isCorrect = correct.isNotEmpty() && given == correct
            if (isCorrect) score++

            val mark = when {
                correct.isEmpty() -> "-"
                isCorrect -> "✓"
                given.isEmpty() -> "✗ (blank)"
                given == "?" -> "✗ (unclear mark)"
                else -> "✗"
            }
            sb.append("Q${i + 1}: given=${given.ifEmpty { "-" }}  key=${correct.ifEmpty { "-" }}  $mark\n")
        }

        val totalScored = key.count { it.isNotEmpty() }
        tvScore.text = "Score: $score / $totalScored"
        tvDetail.text = sb.toString()

        findViewById<Button>(R.id.btnScanAnother).setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java).putExtra("mode", "GRADE"))
            finish()
        }
    }

}

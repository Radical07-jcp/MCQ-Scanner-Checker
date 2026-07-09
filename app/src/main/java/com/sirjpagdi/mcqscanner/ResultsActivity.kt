package com.sirjpagdi.mcqscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultsActivity : AppCompatActivity() {

    companion object {
        // Set by CornerSelectActivity right before launching this activity.
        var pendingDetected: List<String>? = null
    }

    private var detected: List<String> = emptyList()
    private var key: List<String> = emptyList()
    private var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
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
                correct.isEmpty() -> "-"           // void question, not counted
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

        findViewById<Button>(R.id.btnSaveResult).setOnClickListener {
            val name = findViewById<EditText>(R.id.etStudentName).text.toString().ifBlank { "Unnamed" }
            Prefs.addResult(
                this,
                Prefs.ResultRecord(name, score, totalScored, detected, key, System.currentTimeMillis())
            )
            Toast.makeText(this, "Saved for $name", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnExportCsv).setOnClickListener {
            val name = findViewById<EditText>(R.id.etStudentName).text.toString().ifBlank { "Unnamed" }
            exportSingleCsv(name)
        }
    }

    private fun exportSingleCsv(name: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val dir = getExternalFilesDir(null) ?: filesDir
            val file = File(dir, "MCQ_${name.replace(" ", "_")}_$timestamp.csv")
            FileOutputStream(file).use { out ->
                out.write("Question,Detected,Key,Correct\n".toByteArray())
                for (i in key.indices) {
                    val correct = key.getOrElse(i) { "" }
                    val given = detected.getOrElse(i) { "" }
                    val isCorrect = if (correct.isNotEmpty() && given == correct) "YES" else "NO"
                    out.write("${i + 1},$given,$correct,$isCorrect\n".toByteArray())
                }
                out.write("\nStudent,Score,Total\n".toByteArray())
                val totalScored = key.count { it.isNotEmpty() }
                out.write("$name,$score,$totalScored\n".toByteArray())
            }
            Toast.makeText(this, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

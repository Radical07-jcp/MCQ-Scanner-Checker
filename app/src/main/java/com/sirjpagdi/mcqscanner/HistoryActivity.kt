package com.sirjpagdi.mcqscanner

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val records = Prefs.loadHistory(this)
        val tvHistory = findViewById<TextView>(R.id.tvHistory)

        if (records.isEmpty()) {
            tvHistory.text = "No results saved yet.\nScan and save a sheet to see it here."
        } else {
            val df = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
            tvHistory.text = records.reversed().joinToString("\n\n") {
                "${it.studentName}\nScore: ${it.score}/${it.total}\n${df.format(Date(it.timestamp))}"
            }
        }

        findViewById<Button>(R.id.btnExportAll).setOnClickListener {
            exportAll(records)
        }
    }

    private fun exportAll(records: List<Prefs.ResultRecord>) {
        if (records.isEmpty()) {
            Toast.makeText(this, "Nothing to export yet", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val dir = getExternalFilesDir(null) ?: filesDir
            val file = File(dir, "MCQ_All_Results_$timestamp.csv")
            FileOutputStream(file).use { out ->
                out.write("Student,Score,Total,Percentage,Date\n".toByteArray())
                val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                for (r in records) {
                    val pct = if (r.total > 0) String.format(Locale.US, "%.1f", r.score * 100.0 / r.total) else "0.0"
                    out.write("${r.studentName},${r.score},${r.total},$pct%,${df.format(Date(r.timestamp))}\n".toByteArray())
                }
            }
            Toast.makeText(this, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

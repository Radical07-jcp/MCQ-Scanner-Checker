package com.sirjpagdi.mcqscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnTemplate).setOnClickListener {
            startActivity(Intent(this, TemplateActivity::class.java))
        }
        findViewById<Button>(R.id.btnAnswerKey).setOnClickListener {
            if (Prefs.loadTemplate(this) == null) {
                android.widget.Toast.makeText(this, "Set up the template first", android.widget.Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, TemplateActivity::class.java))
            } else {
                startActivity(Intent(this, AnswerKeyActivity::class.java))
            }
        }
        findViewById<Button>(R.id.btnScan).setOnClickListener {
            if (Prefs.loadTemplate(this) == null || Prefs.loadAnswerKey(this) == null) {
                android.widget.Toast.makeText(this, "Set up template and answer key first", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, ScanActivity::class.java))
            }
        }
        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }
}

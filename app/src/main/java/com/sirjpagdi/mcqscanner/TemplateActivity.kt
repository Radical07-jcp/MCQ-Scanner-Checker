package com.sirjpagdi.mcqscanner

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TemplateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template)

        val etQuestions = findViewById<EditText>(R.id.etQuestions)
        val etChoices = findViewById<EditText>(R.id.etChoices)
        val etColumns = findViewById<EditText>(R.id.etColumns)

        Prefs.loadTemplate(this)?.let {
            etQuestions.setText(it.numQuestions.toString())
            etChoices.setText(it.choicesPerQuestion.toString())
            etColumns.setText(it.columns.toString())
        }

        findViewById<Button>(R.id.btnSaveTemplate).setOnClickListener {
            val q = etQuestions.text.toString().toIntOrNull()
            val c = etChoices.text.toString().toIntOrNull()
            val cols = etColumns.text.toString().toIntOrNull() ?: 1

            if (q == null || q <= 0 || c == null || c < 2) {
                Toast.makeText(this, "Enter a valid number of questions and choices", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (q % cols != 0) {
                Toast.makeText(this, "Questions must divide evenly across columns", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Prefs.saveTemplate(this, Prefs.Template(q, c, cols))
            Toast.makeText(this, "Template saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

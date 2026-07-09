package com.sirjpagdi.mcqscanner

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AnswerKeyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer_key)

        val template = Prefs.loadTemplate(this)!!
        val etAnswers = findViewById<EditText>(R.id.etAnswers)

        Prefs.loadAnswerKey(this)?.let {
            etAnswers.setText(it.joinToString(","))
        }

        findViewById<Button>(R.id.btnSaveKey).setOnClickListener {
            val raw = etAnswers.text.toString().trim()
            val parts = raw.split(",").map { it.trim().uppercase() }

            if (parts.size != template.numQuestions) {
                Toast.makeText(
                    this,
                    "Expected ${template.numQuestions} answers, got ${parts.size}",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val validLetters = ('A' until ('A' + template.choicesPerQuestion)).toSet()
            for (p in parts) {
                if (p.isNotEmpty() && (p.length != 1 || p[0] !in validLetters)) {
                    Toast.makeText(this, "Invalid answer '$p'. Use A-${'A' + template.choicesPerQuestion - 1} or leave blank.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            Prefs.saveAnswerKey(this, parts)
            Toast.makeText(this, "Answer key saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

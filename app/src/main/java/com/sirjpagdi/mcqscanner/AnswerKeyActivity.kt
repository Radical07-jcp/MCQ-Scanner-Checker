package com.sirjpagdi.mcqscanner

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Tap-to-select answer key screen.
 * For each question, shows the question number and one button per choice
 * letter (A, B, C...). Tapping a letter selects it as the correct answer;
 * tapping the same letter again clears it (void/no-answer question).
 */
class AnswerKeyActivity : AppCompatActivity() {

    private lateinit var template: Prefs.Template
    private lateinit var answers: MutableList<String>
    private val choiceButtons = mutableListOf<MutableList<Button>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer_key)

        template = Prefs.loadTemplate(this)!!
        answers = (Prefs.loadAnswerKey(this) ?: List(template.numQuestions) { "" }).toMutableList()
        if (answers.size != template.numQuestions) {
            answers = MutableList(template.numQuestions) { i -> answers.getOrElse(i) { "" } }
        }

        buildGrid()

        findViewById<Button>(R.id.btnSaveKey).setOnClickListener {
            Prefs.saveAnswerKey(this, answers)
            Toast.makeText(this, "Answer key saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun themeColor(attrResId: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attrResId, typedValue, true)
        return typedValue.data
    }

    private fun buildGrid() {
        val container = findViewById<LinearLayout>(R.id.containerAnswerGrid)
        container.removeAllViews()
        choiceButtons.clear()

        val goldColor = ContextCompat.getColor(this, R.color.tnhs_gold)
        val maroonDark = ContextCompat.getColor(this, R.color.tnhs_maroon_dark)
        val textPrimary = themeColor(R.attr.appTextPrimary)
        val unselectedColor = themeColor(R.attr.appSurface)

        for (q in 0 until template.numQuestions) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 10 }
            }

            val label = TextView(this).apply {
                text = "Q${q + 1}"
                setTextColor(textPrimary)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(label)

            val rowButtons = mutableListOf<Button>()
            for (c in 0 until template.choicesPerQuestion) {
                val letter = ('A' + c).toString()
                val btn = Button(this).apply {
                    text = letter
                    textSize = 13f
                    setPadding(0, 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(0, 110, 1f).apply {
                        marginStart = 6
                    }
                    setTextColor(if (answers[q] == letter) maroonDark else textPrimary)
                    setBackgroundColor(if (answers[q] == letter) goldColor else unselectedColor)
                }
                val qIndex = q
                btn.setOnClickListener {
                    answers[qIndex] = if (answers[qIndex] == letter) "" else letter
                    refreshRow(qIndex)
                }
                row.addView(btn)
                rowButtons.add(btn)
            }
            choiceButtons.add(rowButtons)
            container.addView(row)
        }
    }

    private fun refreshRow(qIndex: Int) {
        val goldColor = ContextCompat.getColor(this, R.color.tnhs_gold)
        val maroonDark = ContextCompat.getColor(this, R.color.tnhs_maroon_dark)
        val unselectedColor = themeColor(R.attr.appSurface)
        val unselectedText = themeColor(R.attr.appTextPrimary)

        choiceButtons[qIndex].forEachIndexed { i, btn ->
            val letter = ('A' + i).toString()
            val selected = answers[qIndex] == letter
            btn.setBackgroundColor(if (selected) goldColor else unselectedColor)
            btn.setTextColor(if (selected) maroonDark else unselectedText)
        }
    }
}

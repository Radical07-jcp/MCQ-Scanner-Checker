package com.sirjpagdi.mcqscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var etTestName: EditText
    private lateinit var etQuestions: EditText
    private lateinit var rgChoices: RadioGroup
    private lateinit var rgPaperSize: RadioGroup
    private lateinit var tvKeyStatus: TextView
    private lateinit var tvVersion: TextView
    private lateinit var btnThemeToggle: TextView

    private val loadTestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val name = result.data?.getStringExtra("test_name") ?: return@registerForActivityResult
        val test = Prefs.loadTest(this, name) ?: return@registerForActivityResult
        applyTestToFields(test)
        Toast.makeText(this, "Loaded \"$name\"", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etTestName = findViewById(R.id.etTestName)
        etQuestions = findViewById(R.id.etQuestions)
        rgChoices = findViewById(R.id.rgChoices)
        rgPaperSize = findViewById(R.id.rgPaperSize)
        tvKeyStatus = findViewById(R.id.tvKeyStatus)
        tvVersion = findViewById(R.id.tvVersion)
        btnThemeToggle = findViewById(R.id.btnThemeToggle)

        prefillFromActiveState()
        btnThemeToggle.text = ThemeManager.iconFor(Prefs.loadTheme(this))

        findViewById<Button>(R.id.btnCreateKey).setOnClickListener {
            val t = buildTemplateOrToast() ?: return@setOnClickListener
            Prefs.saveTemplate(this, t)
            Prefs.setActiveTestName(this, etTestName.text.toString())
            startActivity(Intent(this, AnswerKeyActivity::class.java))
        }

        findViewById<TextView>(R.id.btnScanKeyInstead).setOnClickListener {
            val t = buildTemplateOrToast() ?: return@setOnClickListener
            Prefs.saveTemplate(this, t)
            startActivity(Intent(this, ScanActivity::class.java).putExtra("mode", "KEY"))
        }

        findViewById<Button>(R.id.btnGeneratePdf).setOnClickListener {
            val t = buildTemplateOrToast() ?: return@setOnClickListener
            val bitmap = TemplateRenderer.render(t)
            PdfGenerator.generateAndShare(this, bitmap, etTestName.text.toString().ifBlank { "MCQ_Template" })
        }

        findViewById<Button>(R.id.btnSaveTest).setOnClickListener {
            val name = etTestName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Give the test a name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val t = buildTemplateOrToast() ?: return@setOnClickListener
            Prefs.saveTemplate(this, t)
            val key = Prefs.loadAnswerKey(this) ?: List(t.numQuestions) { "" }
            Prefs.saveTest(this, Prefs.Test(name, t, key))
            Prefs.setActiveTestName(this, name)
            Toast.makeText(this, "Saved \"$name\"", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnLoadTest).setOnClickListener {
            loadTestLauncher.launch(Intent(this, LoadTestActivity::class.java))
        }

        findViewById<Button>(R.id.btnScan).setOnClickListener {
            val t = buildTemplateOrToast() ?: return@setOnClickListener
            val key = Prefs.loadAnswerKey(this)
            if (key == null || key.size != t.numQuestions || key.all { it.isEmpty() }) {
                Toast.makeText(this, "Set an answer key before scanning student sheets", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, ScanActivity::class.java).putExtra("mode", "GRADE"))
        }

        btnThemeToggle.setOnClickListener {
            ThemeManager.cycle(this)
            recreate()
        }

        tvVersion.setOnClickListener {
            startActivity(Intent(this, ChangelogActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateKeyStatus()
    }

    private fun prefillFromActiveState() {
        val t = Prefs.loadTemplate(this)
        etTestName.setText(Prefs.activeTestName(this))
        if (t != null) {
            etQuestions.setText(t.numQuestions.toString())
            rgChoices.check(if (t.choicesPerQuestion >= 5) R.id.rbAE else R.id.rbAD)
            rgPaperSize.check(
                when (t.paperSize) {
                    Prefs.PaperSize.FULL -> R.id.rbFull
                    Prefs.PaperSize.HALF -> R.id.rbHalf
                    Prefs.PaperSize.QUARTER -> R.id.rbQuarter
                }
            )
        }
        updateKeyStatus()
    }

    private fun applyTestToFields(test: Prefs.Test) {
        etTestName.setText(test.name)
        etQuestions.setText(test.template.numQuestions.toString())
        rgChoices.check(if (test.template.choicesPerQuestion >= 5) R.id.rbAE else R.id.rbAD)
        rgPaperSize.check(
            when (test.template.paperSize) {
                Prefs.PaperSize.FULL -> R.id.rbFull
                Prefs.PaperSize.HALF -> R.id.rbHalf
                Prefs.PaperSize.QUARTER -> R.id.rbQuarter
            }
        )
        Prefs.saveTemplate(this, test.template)
        Prefs.saveAnswerKey(this, test.answerKey)
        Prefs.setActiveTestName(this, test.name)
        updateKeyStatus()
    }

    private fun updateKeyStatus() {
        val t = Prefs.loadTemplate(this)
        val key = Prefs.loadAnswerKey(this)
        tvKeyStatus.text = if (t == null || key == null || key.all { it.isEmpty() }) {
            "No answer key set yet"
        } else {
            val filled = key.count { it.isNotEmpty() }
            "Answer key set ($filled of ${t.numQuestions} questions)"
        }
    }

    private fun buildTemplateOrToast(): Prefs.Template? {
        val q = etQuestions.text.toString().toIntOrNull()
        if (q == null || q <= 0) {
            Toast.makeText(this, "Enter a valid number of questions", Toast.LENGTH_SHORT).show()
            return null
        }
        val choices = if (rgChoices.checkedRadioButtonId == R.id.rbAE) 5 else 4
        val paperSize = when (rgPaperSize.checkedRadioButtonId) {
            R.id.rbHalf -> Prefs.PaperSize.HALF
            R.id.rbQuarter -> Prefs.PaperSize.QUARTER
            else -> Prefs.PaperSize.FULL
        }
        // Single-column layout to match the current design; ask if you'd like
        // a multi-column option back for long tests.
        return Prefs.Template(q, choices, columns = 1, paperSize = paperSize)
    }
}

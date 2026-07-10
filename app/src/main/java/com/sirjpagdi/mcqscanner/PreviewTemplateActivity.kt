package com.sirjpagdi.mcqscanner

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PreviewTemplateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_template)

        val title = intent.getStringExtra("title").orEmpty()
        val template = Prefs.loadTemplate(this)
        if (template == null) {
            Toast.makeText(this, "No template found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val bitmap = TemplateRenderer.render(template, title)
        findViewById<ImageView>(R.id.ivPreview).setImageBitmap(bitmap)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSharePdf).setOnClickListener {
            PdfGenerator.generateAndShare(this, bitmap, title)
        }
    }
}

package com.sirjpagdi.mcqscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CornerSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_corner_select)

        val cornerView = findViewById<CornerSelectView>(R.id.cornerView)
        val bitmap = SheetImageHolder.capturedBitmap
        if (bitmap == null) {
            finish()
            return
        }
        cornerView.bitmap = bitmap

        findViewById<Button>(R.id.btnConfirmCorners).setOnClickListener {
            val corners = cornerView.getCornersInBitmapSpace()
            if (corners == null) {
                Toast.makeText(this, "Could not read corners, try again", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val template = Prefs.loadTemplate(this)!!
            val warped = OMRProcessor.warpPerspective(bitmap, corners)
            val detected = OMRProcessor.grade(
                warped,
                template.numQuestions,
                template.columns,
                template.choicesPerQuestion
            )

            SheetImageHolder.warpedBitmap = warped
            ResultsActivity.pendingDetected = detected
            startActivity(Intent(this, ResultsActivity::class.java))
            finish()
        }
    }
}

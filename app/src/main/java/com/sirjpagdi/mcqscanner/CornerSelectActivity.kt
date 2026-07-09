package com.sirjpagdi.mcqscanner

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CornerSelectActivity : AppCompatActivity() {

    private var scanMode: String = "GRADE"

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_corner_select)

        scanMode = intent.getStringExtra("mode") ?: "GRADE"

        val cornerView = findViewById<CornerSelectView>(R.id.cornerView)
        val tvStatus = findViewById<TextView>(R.id.tvAlignStatus)
        val bitmap = SheetImageHolder.capturedBitmap
        if (bitmap == null) {
            finish()
            return
        }
        cornerView.bitmap = bitmap

        cornerView.setOnAlignmentChangedListener { aligned ->
            if (aligned) {
                tvStatus.text = "Looks good — tap Confirm & Continue"
                tvStatus.setTextColor(Color.parseColor("#2FA84F"))
                buzz(15)
            } else {
                tvStatus.text = "Drag the 4 corners to match the sheet's edges"
                tvStatus.setTextColor(Color.parseColor("#F2B705"))
            }
        }

        findViewById<Button>(R.id.btnConfirmCorners).setOnClickListener {
            val corners = cornerView.getCornersInBitmapSpace()
            if (corners == null) {
                Toast.makeText(this, "Could not read corners, try again", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            buzz(40)

            val template = Prefs.loadTemplate(this)!!
            val warped = OMRProcessor.warpPerspective(bitmap, corners)
            val detected = OMRProcessor.grade(
                warped,
                template.numQuestions,
                template.columns,
                template.choicesPerQuestion
            )

            if (scanMode == "KEY") {
                Prefs.saveAnswerKey(this, detected)
                Toast.makeText(this, "Answer key captured from scan", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            } else {
                SheetImageHolder.warpedBitmap = warped
                ResultsActivity.pendingDetected = detected
                startActivity(Intent(this, ResultsActivity::class.java))
                finish()
            }
        }
    }

    private fun buzz(ms: Long) {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

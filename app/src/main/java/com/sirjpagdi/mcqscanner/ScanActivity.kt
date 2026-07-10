package com.sirjpagdi.mcqscanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

/**
 * Scanning screen. Instead of a full-frame contour search (fragile, and
 * heavy on OpenCV per frame), this samples plain luminance near 4 fixed
 * corner brackets — matching where the sheet's printed corner squares
 * (see TemplateRenderer) should sit once the teacher lines things up.
 * There's no manual corner-dragging step after capture anymore: the photo
 * is warped straight away using those same 4 bracket positions.
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: ScanOverlayView
    private var imageCapture: ImageCapture? = null
    private var scanMode: String = "GRADE"

    // Must match ScanOverlayView's insetFraction / bracketSizeFraction.
    private val insetFraction = 0.10f
    private val bracketSizeFraction = 0.09f
    private val darknessThreshold = 110 // 0-255 luminance; below this counts as "dark ink present"
    private val darkPixelFractionNeeded = 0.12

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else {
            Toast.makeText(this, "Camera permission is required to scan sheets", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.scanOverlay)
        scanMode = intent.getStringExtra("mode") ?: "GRADE"

        findViewById<Button>(R.id.btnCapture).setOnClickListener { captureSheet() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { image ->
                val aligned = checkCornerAlignment(image)
                image.close()
                runOnUiThread { overlayView.updateOverlay(aligned) }
            }

            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll()
            provider.bindToLifecycle(this, selector, preview, imageCapture, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    /** Samples the Y (luminance) plane directly — no OpenCV needed for this
     *  lightweight per-frame check, which keeps live scanning smooth and
     *  avoids repeated native calls that were triggering crashes before. */
    private fun checkCornerAlignment(image: ImageProxy): BooleanArray {
        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val w = image.width
        val h = image.height

        val inset = w * insetFraction
        val bracket = minOf(w, h) * bracketSizeFraction

        // Build the 4 corner sample rects directly in image pixel space, mirroring ScanOverlayView.
        val insetY = h * insetFraction
        val corners = arrayOf(
            intArrayOf(inset.toInt(), insetY.toInt(), (inset + bracket).toInt(), (insetY + bracket).toInt()),
            intArrayOf((w - inset - bracket).toInt(), insetY.toInt(), (w - inset).toInt(), (insetY + bracket).toInt()),
            intArrayOf(inset.toInt(), (h - insetY - bracket).toInt(), (inset + bracket).toInt(), (h - insetY).toInt()),
            intArrayOf((w - inset - bracket).toInt(), (h - insetY - bracket).toInt(), (w - inset).toInt(), (h - insetY).toInt())
        )

        val result = BooleanArray(4)
        for (i in corners.indices) {
            val (x0, y0, x1, y1) = corners[i].let { listOf(it[0], it[1], it[2], it[3]) }
            result[i] = regionHasDarkMarker(buffer, rowStride, w, h, x0, y0, x1, y1)
        }
        return result
    }

    private fun regionHasDarkMarker(
        buffer: java.nio.ByteBuffer, rowStride: Int, imgW: Int, imgH: Int,
        x0: Int, y0: Int, x1: Int, y1: Int
    ): Boolean {
        val cx0 = x0.coerceIn(0, imgW - 1)
        val cy0 = y0.coerceIn(0, imgH - 1)
        val cx1 = x1.coerceIn(cx0 + 1, imgW)
        val cy1 = y1.coerceIn(cy0 + 1, imgH)

        var dark = 0
        var total = 0
        val step = 2 // sample every other pixel — plenty for a darkness ratio, much cheaper
        val row = ByteArray(cx1 - cx0)
        for (y in cy0 until cy1 step step) {
            val rowStart = y * rowStride + cx0
            if (rowStart + row.size > buffer.capacity()) break
            buffer.position(rowStart)
            buffer.get(row, 0, row.size)
            for (x in row.indices step step) {
                total++
                val lum = row[x].toInt() and 0xFF
                if (lum < darknessThreshold) dark++
            }
        }
        buffer.rewind()
        if (total == 0) return false
        return dark.toDouble() / total >= darkPixelFractionNeeded
    }

    private fun captureSheet() {
        val capture = imageCapture ?: return
        buzz()
        capture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = ImageUtils.imageProxyToBitmap(image)
                image.close()

                val corners = bracketCorners(bitmap)
                val warped = OMRProcessor.warpPerspective(bitmap, corners)
                val template = Prefs.loadTemplate(this@ScanActivity)!!
                val detected = OMRProcessor.grade(warped, template)

                if (scanMode == "KEY") {
                    Prefs.saveAnswerKey(this@ScanActivity, detected)
                    Toast.makeText(this@ScanActivity, "Answer key captured from scan", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@ScanActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                } else {
                    SheetImageHolder.warpedBitmap = warped
                    ResultsActivity.pendingDetected = detected
                    startActivity(Intent(this@ScanActivity, ResultsActivity::class.java))
                    finish()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(this@ScanActivity, "Capture failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    /** The 4 points where the sheet's corner markers should be, in the
     *  captured bitmap's own pixel space — same fractional layout as the
     *  on-screen guide, so what the teacher aligned to is what gets warped. */
    private fun bracketCorners(bitmap: Bitmap): Array<PointF> {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val insetX = w * insetFraction
        val insetY = h * insetFraction
        val bracket = minOf(w, h) * bracketSizeFraction
        val half = bracket / 2f
        return arrayOf(
            PointF(insetX + half, insetY + half),
            PointF(w - insetX - half, insetY + half),
            PointF(w - insetX - half, h - insetY - half),
            PointF(insetX + half, h - insetY - half)
        )
    }

    private fun buzz() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

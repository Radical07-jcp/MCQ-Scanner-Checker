package com.sirjpagdi.mcqscanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: ScanOverlayView
    private var imageCapture: ImageCapture? = null
    private var scanMode: String = "GRADE"
    private var lastDetectedCorners: Array<PointF>? = null

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
                val corners = detectSheetCorners(image)
                val aligned = corners != null
                if (aligned) {
                    lastDetectedCorners = corners
                }
                runOnUiThread {
                    overlayView.updateOverlay(
                        corners?.map { toViewPoint(it, image.width, image.height) }?.toTypedArray(),
                        aligned
                    )
                }
                image.close()
            }

            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll()
            provider.bindToLifecycle(this, selector, preview, imageCapture, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun detectSheetCorners(image: ImageProxy): Array<PointF>? {
        val gray = ImageUtils.imageProxyToGrayMat(image)
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
        val edges = Mat()
        Imgproc.Canny(blurred, edges, 75.0, 200.0)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        gray.release()
        blurred.release()
        edges.release()

        var bestCorners: Array<PointF>? = null
        var bestArea = 0.0

        for (contour in contours) {
            val contourArea = Imgproc.contourArea(contour)
            if (contourArea < 10000) {
                contour.release()
                continue
            }

            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(
                MatOfPoint2f(*contour.toArray()),
                approx,
                Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true) * 0.02,
                true
            )

            if (approx.total() == 4L && contourArea > bestArea) {
                bestArea = contourArea
                bestCorners = orderPoints(approx.toList())
            }
            contour.release()
            approx.release()
        }

        contours.forEach { it.release() }
        return bestCorners
    }

    private fun orderPoints(points: List<Point>): Array<PointF> {
        val tl = points.minByOrNull { it.x + it.y } ?: points[0]
        val br = points.maxByOrNull { it.x + it.y } ?: points[0]
        val tr = points.minByOrNull { it.x - it.y } ?: points[0]
        val bl = points.maxByOrNull { it.x - it.y } ?: points[0]
        return arrayOf(
            PointF(tl.x.toFloat(), tl.y.toFloat()),
            PointF(tr.x.toFloat(), tr.y.toFloat()),
            PointF(br.x.toFloat(), br.y.toFloat()),
            PointF(bl.x.toFloat(), bl.y.toFloat())
        )
    }

    private fun toViewPoint(point: PointF, imageWidth: Int, imageHeight: Int): PointF {
        val scaleX = previewView.width.toFloat() / imageWidth.toFloat()
        val scaleY = previewView.height.toFloat() / imageHeight.toFloat()
        return PointF(point.x * scaleX, point.y * scaleY)
    }

    private fun captureSheet() {
        val capture = imageCapture ?: return
        buzz()
        capture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = ImageUtils.imageProxyToBitmap(image)
                image.close()

                val corners = lastDetectedCorners ?: defaultBitmapCorners(bitmap)
                val warped = OMRProcessor.warpPerspective(bitmap, corners)
                val template = Prefs.loadTemplate(this@ScanActivity)!!
                val detected = OMRProcessor.grade(
                    warped,
                    template.numQuestions,
                    template.columns,
                    template.choicesPerQuestion
                )

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

    private fun defaultBitmapCorners(bitmap: android.graphics.Bitmap): Array<PointF> {
        return arrayOf(
            PointF(0f, 0f),
            PointF(bitmap.width.toFloat(), 0f),
            PointF(bitmap.width.toFloat(), bitmap.height.toFloat()),
            PointF(0f, bitmap.height.toFloat())
        )
    }

    private fun buzz() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

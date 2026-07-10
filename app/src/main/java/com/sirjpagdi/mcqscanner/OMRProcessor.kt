package com.sirjpagdi.mcqscanner

import android.graphics.Bitmap
import android.graphics.PointF
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min

/**
 * Core OMR grading logic.
 *
 *  1. Perspective-warp the photographed sheet to a flat rectangle.
 *  2. Convert to grayscale and apply Otsu thresholding to separate pencil/pen
 *     marks from paper.
 *  3. Sample each bubble position using SheetGeometry — the same geometry
 *     TemplateRenderer used to draw the sheet in the first place, so the
 *     sampling regions always match where the bubbles actually are.
 *  4. For each question, the "darkest" cell (highest ratio of dark pixels)
 *     above a minimum-fill threshold is the marked answer.
 */
object OMRProcessor {

    private const val WARPED_WIDTH = 1240
    private const val WARPED_HEIGHT = 1754

    // Fraction of a cell that must be dark for it to count as "marked" at all.
    private const val FILL_THRESHOLD = 0.32
    // Minimum lead the darkest cell needs over the second-darkest to count as unambiguous.
    private const val AMBIGUITY_MARGIN = 0.07

    fun warpPerspective(src: Bitmap, corners: Array<PointF>): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(src, srcMat)

        val srcPoints = MatOfPoint2f(
            Point(corners[0].x.toDouble(), corners[0].y.toDouble()),
            Point(corners[1].x.toDouble(), corners[1].y.toDouble()),
            Point(corners[2].x.toDouble(), corners[2].y.toDouble()),
            Point(corners[3].x.toDouble(), corners[3].y.toDouble())
        )
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(WARPED_WIDTH - 1.0, 0.0),
            Point(WARPED_WIDTH - 1.0, WARPED_HEIGHT - 1.0),
            Point(0.0, WARPED_HEIGHT - 1.0)
        )

        val transform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
        val dstMat = Mat()
        Imgproc.warpPerspective(srcMat, dstMat, transform, Size(WARPED_WIDTH.toDouble(), WARPED_HEIGHT.toDouble()))

        val outBitmap = Bitmap.createBitmap(WARPED_WIDTH, WARPED_HEIGHT, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dstMat, outBitmap)

        srcMat.release(); dstMat.release()
        return outBitmap
    }

    fun grade(warped: Bitmap, template: Prefs.Template): List<String> {
        val mat = Mat()
        Utils.bitmapToMat(warped, mat)
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        val thresh = Mat()
        Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)

        val width = WARPED_WIDTH.toFloat()
        val height = WARPED_HEIGHT.toFloat()
        val geometry = SheetGeometry.compute(template, width, height)
        val bubbleSpacing = SheetGeometry.bubbleSpacing(geometry, template.choicesPerQuestion, width)
        // Sample window is a fraction of the bubble's own footprint, centered on it,
        // so we're reading the bubble itself rather than the gap between bubbles.
        val sampleHalfW = min(bubbleSpacing * 0.32f, geometry.bubbleRadius * 1.15f)
        val sampleHalfH = min(geometry.rowHeight * 0.32f, geometry.bubbleRadius * 1.15f)

        val answers = mutableListOf<String>()

        for (q in 0 until template.numQuestions) {
            val col = q / geometry.rowsPerCol
            val rowInCol = q % geometry.rowsPerCol
            val rowY = SheetGeometry.rowCenterY(geometry, rowInCol)

            val fillRatios = DoubleArray(template.choicesPerQuestion)
            for (c in 0 until template.choicesPerQuestion) {
                val cx = SheetGeometry.bubbleCenterX(geometry, col, c, template.choicesPerQuestion, width)

                val rx0 = max((cx - sampleHalfW).toInt(), 0)
                val rx1 = min((cx + sampleHalfW).toInt(), WARPED_WIDTH)
                val ry0 = max((rowY - sampleHalfH).toInt(), 0)
                val ry1 = min((rowY + sampleHalfH).toInt(), WARPED_HEIGHT)
                if (rx1 <= rx0 || ry1 <= ry0) { fillRatios[c] = 0.0; continue }

                val roi = thresh.submat(ry0, ry1, rx0, rx1)
                val nonZero = Core.countNonZero(roi)
                val area = roi.rows() * roi.cols()
                fillRatios[c] = if (area > 0) nonZero.toDouble() / area else 0.0
                roi.release()
            }

            answers.add(pickAnswer(fillRatios))
        }

        mat.release(); gray.release(); thresh.release()
        return answers
    }

    private fun pickAnswer(fillRatios: DoubleArray): String {
        var best = -1
        var bestVal = -1.0
        var secondVal = -1.0
        for (i in fillRatios.indices) {
            if (fillRatios[i] > bestVal) {
                secondVal = bestVal
                bestVal = fillRatios[i]
                best = i
            } else if (fillRatios[i] > secondVal) {
                secondVal = fillRatios[i]
            }
        }
        if (best == -1 || bestVal < FILL_THRESHOLD) return "" // blank
        if (bestVal - secondVal < AMBIGUITY_MARGIN) return "?" // multiple/ambiguous marks
        return ('A' + best).toString()
    }
}

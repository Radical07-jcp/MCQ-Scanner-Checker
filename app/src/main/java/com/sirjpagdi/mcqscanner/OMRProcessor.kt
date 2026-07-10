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
 * Approach (adapted from OMRChecker's philosophy, simplified for on-device use):
 *  1. Perspective-warp the photographed sheet to a flat rectangle using the 4 corners
 *     the teacher marked.
 *  2. Convert to grayscale and apply Otsu thresholding to separate pencil/pen marks
 *     from paper.
 *  3. Slice the sheet into a grid of question-rows x choice-columns (per template),
 *     one cell per bubble.
 *  4. For each question, the "darkest" cell (highest ratio of dark pixels) above a
 *     minimum-fill threshold is the marked answer. Ties / no dark-enough cell => blank.
 */
object OMRProcessor {

    init {
        try {
            System.loadLibrary("opencv_java4")
        } catch (_: UnsatisfiedLinkError) {
        }
    }

    private const val WARPED_WIDTH = 1000
    private const val WARPED_HEIGHT = 1400

    // Fraction of a cell that must be dark for it to count as "marked" at all.
    private const val FILL_THRESHOLD = 0.35
    // Minimum lead the darkest cell needs over the second-darkest to count as unambiguous.
    private const val AMBIGUITY_MARGIN = 0.08

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

    /**
     * Grades a warped (already perspective-corrected) sheet bitmap.
     * @param totalQuestions total number of questions on the sheet
     * @param columns how many side-by-side question blocks the sheet has
     * @param choices number of bubble choices per question (e.g. 4 for A-D)
     */
    fun grade(
        warped: Bitmap,
        totalQuestions: Int,
        columns: Int,
        choices: Int
    ): List<String> {
        val mat = Mat()
        Utils.bitmapToMat(warped, mat)
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        val thresh = Mat()
        Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)

        val questionsPerColumn = totalQuestions / columns
        val colWidth = WARPED_WIDTH / columns
        // Leave small margins so we don't sample question-number labels at the cell edge.
        val rowHeight = WARPED_HEIGHT.toDouble() / questionsPerColumn
        val cellWidth = colWidth.toDouble() / choices

        val answers = mutableListOf<String>()

        for (q in 0 until totalQuestions) {
            val col = q / questionsPerColumn
            val rowInCol = q % questionsPerColumn

            val fillRatios = DoubleArray(choices)
            for (c in 0 until choices) {
                val x0 = (col * colWidth + c * cellWidth).toInt()
                val x1 = (col * colWidth + (c + 1) * cellWidth).toInt()
                val y0 = (rowInCol * rowHeight).toInt()
                val y1 = ((rowInCol + 1) * rowHeight).toInt()

                // Shrink the sampling window slightly to avoid grid-line bleed.
                val padX = ((x1 - x0) * 0.15).toInt()
                val padY = ((y1 - y0) * 0.15).toInt()
                val rx0 = max(x0 + padX, 0)
                val rx1 = min(x1 - padX, WARPED_WIDTH)
                val ry0 = max(y0 + padY, 0)
                val ry1 = min(y1 - padY, WARPED_HEIGHT)
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

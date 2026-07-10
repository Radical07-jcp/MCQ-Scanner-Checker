package com.sirjpagdi.mcqscanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Four corner-bracket guides — one near each corner of the frame — matching
 * the four black squares printed on the answer sheet (see TemplateRenderer).
 * The teacher aligns each printed corner square into its matching bracket.
 * Brackets turn green once ScanActivity's per-corner darkness check thinks
 * a marker is sitting in each one.
 */
class ScanOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var cornersAligned = BooleanArray(4)
    private var allAligned = false

    private val guidePaint = Paint().apply {
        color = Color.parseColor("#F2B705")
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }
    private val goodPaint = Paint().apply {
        color = Color.parseColor("#2FA84F")
        style = Paint.Style.STROKE
        strokeWidth = 9f
        isAntiAlias = true
    }
    private val statusPaint = Paint().apply {
        color = Color.WHITE
        textSize = 42f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    /** Same fractional inset/size used by ScanActivity to sample corner darkness — keep in sync. */
    private val insetFraction = 0.10f
    private val bracketSizeFraction = 0.09f
    private val armLengthFraction = 0.55f // fraction of bracket size used for each L-shaped arm

    fun updateOverlay(perCornerAligned: BooleanArray) {
        cornersAligned = perCornerAligned
        allAligned = perCornerAligned.size == 4 && perCornerAligned.all { it }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val inset = width * insetFraction
        val bracketSize = minOf(width, height) * bracketSizeFraction
        val arm = bracketSize * armLengthFraction

        val rects = arrayOf(
            RectF(inset, inset, inset + bracketSize, inset + bracketSize),                                   // top-left
            RectF(width - inset - bracketSize, inset, width - inset, inset + bracketSize),                    // top-right
            RectF(inset, height - inset - bracketSize, inset + bracketSize, height - inset),                   // bottom-left
            RectF(width - inset - bracketSize, height - inset - bracketSize, width - inset, height - inset)   // bottom-right
        )

        rects.forEachIndexed { i, r ->
            val aligned = cornersAligned.getOrElse(i) { false }
            val paint = if (aligned) goodPaint else guidePaint
            drawBracket(canvas, r, i, arm, paint)
        }

        val statusText = if (allAligned) "Ready — capture now" else "Fit the 4 sheet corners into the guides"
        canvas.drawText(statusText, width / 2f, height * 0.08f, statusPaint)
    }

    /** Draws an L-shaped corner bracket inside rect r, oriented toward the frame's actual corner. */
    private fun drawBracket(canvas: Canvas, r: RectF, cornerIndex: Int, arm: Float, paint: Paint) {
        when (cornerIndex) {
            0 -> { // top-left: corner point is r's top-left
                canvas.drawLine(r.left, r.top, r.left + arm, r.top, paint)
                canvas.drawLine(r.left, r.top, r.left, r.top + arm, paint)
            }
            1 -> { // top-right
                canvas.drawLine(r.right, r.top, r.right - arm, r.top, paint)
                canvas.drawLine(r.right, r.top, r.right, r.top + arm, paint)
            }
            2 -> { // bottom-left
                canvas.drawLine(r.left, r.bottom, r.left + arm, r.bottom, paint)
                canvas.drawLine(r.left, r.bottom, r.left, r.bottom - arm, paint)
            }
            3 -> { // bottom-right
                canvas.drawLine(r.right, r.bottom, r.right - arm, r.bottom, paint)
                canvas.drawLine(r.right, r.bottom, r.right, r.bottom - arm, paint)
            }
        }
    }
}

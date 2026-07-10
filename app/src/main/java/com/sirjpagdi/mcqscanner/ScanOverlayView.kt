package com.sirjpagdi.mcqscanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

class ScanOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var corners: Array<PointF>? = null
    private var aligned: Boolean = false

    private val guidePaint = Paint().apply {
        color = Color.parseColor("#F2B705")
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }
    private val goodPaint = Paint().apply {
        color = Color.parseColor("#2FA84F")
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }
    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val handleBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val statusPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    fun updateOverlay(corners: Array<PointF>?, aligned: Boolean) {
        this.corners = corners
        this.aligned = aligned
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val path = Path()
        val paint = if (aligned) goodPaint else guidePaint

        if (corners == null) {
            val inset = 80f
            val left = inset
            val top = inset
            val right = width - inset
            val bottom = height - inset
            path.moveTo(left, top)
            path.lineTo(right, top)
            path.lineTo(right, bottom)
            path.lineTo(left, bottom)
            path.close()
            canvas.drawPath(path, paint)
            drawCornerHandles(canvas, left, top, right, bottom)
        } else {
            val quad = corners!!
            path.moveTo(quad[0].x, quad[0].y)
            for (i in 1 until quad.size) path.lineTo(quad[i].x, quad[i].y)
            path.close()
            canvas.drawPath(path, paint)
            quad.forEach { canvas.drawCircle(it.x, it.y, 14f, handlePaint); canvas.drawCircle(it.x, it.y, 14f, handleBorderPaint) }
        }

        val statusText = if (aligned) "Ready to capture" else "Align the sheet inside the guide"
        canvas.drawText(statusText, width / 2f, 60f, statusPaint)
    }

    private fun drawCornerHandles(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val radius = 16f
        listOf(
            PointF(left, top),
            PointF(right, top),
            PointF(right, bottom),
            PointF(left, bottom)
        ).forEach {
            canvas.drawCircle(it.x, it.y, radius, handlePaint)
            canvas.drawCircle(it.x, it.y, radius, handleBorderPaint)
        }
    }
}

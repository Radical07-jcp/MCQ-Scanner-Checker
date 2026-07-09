package com.sirjpagdi.mcqscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Displays the captured sheet photo and lets the user drag 4 corner handles
 * to mark the sheet's actual boundary (for perspective correction).
 */
class CornerSelectView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var bitmap: Bitmap? = null
        set(value) {
            field = value
            resetCorners()
            invalidate()
        }

    // Corners in view coordinates, order: TL, TR, BR, BL
    private val corners = Array(4) { PointF() }
    private var dragIndex = -1
    private var imageRect = RectF()

    private val linePaint = Paint().apply {
        color = Color.parseColor("#F2B705")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val linePaintGood = Paint().apply {
        color = Color.parseColor("#2FA84F")
        strokeWidth = 7f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val handlePaint = Paint().apply {
        color = Color.parseColor("#EB5757")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val handleRadius = 28f
    private var onAlignmentChanged: ((Boolean) -> Unit)? = null
    private var wasAligned = false

    fun setOnAlignmentChangedListener(listener: (Boolean) -> Unit) {
        onAlignmentChanged = listener
    }

    /** A rough "does this look like a properly aligned rectangle" check —
     *  not a full CV analysis, just enough to give the teacher a visual nudge. */
    private fun isWellAligned(): Boolean {
        if (imageRect.isEmpty) return false
        val area = quadArea()
        val imgArea = imageRect.width() * imageRect.height()
        if (imgArea <= 0f) return false
        val coverage = area / imgArea
        if (coverage < 0.35f) return false

        // Diagonals should be reasonably similar in length for a fair, non-skewed rectangle.
        val d1 = distance(corners[0], corners[2].x, corners[2].y)
        val d2 = distance(corners[1], corners[3].x, corners[3].y)
        val ratio = if (d2 == 0f) 0f else minOf(d1, d2) / maxOf(d1, d2)
        return ratio > 0.75f
    }

    private fun quadArea(): Float {
        // Shoelace formula
        var sum = 0f
        for (i in 0..3) {
            val p1 = corners[i]
            val p2 = corners[(i + 1) % 4]
            sum += p1.x * p2.y - p2.x * p1.y
        }
        return kotlin.math.abs(sum) / 2f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeImageRect()
        resetCorners()
    }

    private fun computeImageRect() {
        val bmp = bitmap ?: return
        val viewRatio = width.toFloat() / height.toFloat()
        val bmpRatio = bmp.width.toFloat() / bmp.height.toFloat()
        if (bmpRatio > viewRatio) {
            val h = width / bmpRatio
            val top = (height - h) / 2f
            imageRect = RectF(0f, top, width.toFloat(), top + h)
        } else {
            val w = height * bmpRatio
            val left = (width - w) / 2f
            imageRect = RectF(left, 0f, left + w, height.toFloat())
        }
    }

    private fun resetCorners() {
        if (imageRect.isEmpty && width > 0 && height > 0) computeImageRect()
        val inset = 0.06f
        val w = imageRect.width()
        val h = imageRect.height()
        corners[0].set(imageRect.left + w * inset, imageRect.top + h * inset) // TL
        corners[1].set(imageRect.right - w * inset, imageRect.top + h * inset) // TR
        corners[2].set(imageRect.right - w * inset, imageRect.bottom - h * inset) // BR
        corners[3].set(imageRect.left + w * inset, imageRect.bottom - h * inset) // BL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return
        if (imageRect.isEmpty) computeImageRect()
        canvas.drawBitmap(bmp, null, imageRect, null)

        val path = android.graphics.Path()
        path.moveTo(corners[0].x, corners[0].y)
        for (i in 1..3) path.lineTo(corners[i].x, corners[i].y)
        path.close()

        val aligned = isWellAligned()
        canvas.drawPath(path, if (aligned) linePaintGood else linePaint)
        if (aligned != wasAligned) {
            wasAligned = aligned
            onAlignmentChanged?.invoke(aligned)
        }

        corners.forEach { canvas.drawCircle(it.x, it.y, handleRadius, handlePaint) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragIndex = corners.indexOfFirst { distance(it, event.x, event.y) < handleRadius * 2 }
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragIndex >= 0) {
                    corners[dragIndex].set(event.x, event.y)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragIndex = -1
        }
        return true
    }

    private fun distance(p: PointF, x: Float, y: Float): Float {
        val dx = p.x - x
        val dy = p.y - y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /** Returns the 4 corners mapped from view space back into original bitmap pixel space. */
    fun getCornersInBitmapSpace(): Array<PointF>? {
        val bmp = bitmap ?: return null
        if (imageRect.isEmpty) return null
        val scaleX = bmp.width / imageRect.width()
        val scaleY = bmp.height / imageRect.height()
        return Array(4) { i ->
            PointF(
                (corners[i].x - imageRect.left) * scaleX,
                (corners[i].y - imageRect.top) * scaleY
            )
        }
    }
}

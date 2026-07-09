package com.sirjpagdi.mcqscanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Renders a printable blank answer sheet bitmap from a saved Template.
 * Students bubble this exact layout in; corner markers at each edge
 * make it easy to align during scanning.
 */
object TemplateRenderer {

    private const val WIDTH = 1240   // ~A4 at 150dpi
    private const val HEIGHT = 1754

    fun render(template: Prefs.Template, schoolName: String = "Tarlac National High School"): Bitmap {
        val scale = when (template.paperSize) {
            Prefs.PaperSize.FULL -> 1.0
            Prefs.PaperSize.HALF -> 0.72
            Prefs.PaperSize.QUARTER -> 0.5
        }
        val width = (WIDTH * scale).toInt()
        val height = (HEIGHT * scale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        renderContent(canvas, width, height, template, schoolName)
        return bitmap
    }

    private fun renderContent(canvas: Canvas, WIDTH: Int, HEIGHT: Int, template: Prefs.Template, schoolName: String) {

        val maroon = Color.parseColor("#7A0C2E")
        val black = Color.BLACK

        val titlePaint = Paint().apply {
            color = maroon
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            color = black
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
        val labelPaint = Paint().apply {
            color = black
            textSize = 18f
            isAntiAlias = true
        }
        val bubblePaint = Paint().apply {
            color = black
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }
        val cornerPaint = Paint().apply {
            color = maroon
            style = Paint.Style.FILL
        }

        // Corner alignment markers (match what the app looks for when scanning)
        val markerSize = 30f
        canvas.drawRect(20f, 20f, 20f + markerSize, 20f + markerSize, cornerPaint)
        canvas.drawRect(WIDTH - 20f - markerSize, 20f, WIDTH - 20f, 20f + markerSize, cornerPaint)
        canvas.drawRect(20f, HEIGHT - 20f - markerSize, 20f + markerSize, HEIGHT - 20f, cornerPaint)
        canvas.drawRect(WIDTH - 20f - markerSize, HEIGHT - 20f - markerSize, WIDTH - 20f, HEIGHT - 20f, cornerPaint)

        var y = 90f
        canvas.drawText(schoolName, WIDTH / 2f, y, titlePaint)
        y += 40f
        canvas.drawText("Multiple Choice Answer Sheet", WIDTH / 2f, y, subPaint.apply { textAlign = Paint.Align.CENTER })
        subPaint.textAlign = Paint.Align.LEFT

        y += 50f
        canvas.drawText("Name: ______________________________", 60f, y, subPaint)
        canvas.drawText("Date: ____________", WIDTH - 320f, y, subPaint)
        y += 30f
        canvas.drawText("Section: ___________________________", 60f, y, subPaint)
        canvas.drawText("Score: ____________", WIDTH - 320f, y, subPaint)
        y += 40f

        val gridTop = y
        val gridBottom = HEIGHT - 80f
        val colWidth = (WIDTH - 100f) / template.columns
        val rowsPerCol = template.numQuestions / template.columns
        val rowHeight = (gridBottom - gridTop) / rowsPerCol
        val bubbleRadius = minOf(rowHeight * 0.28f, 16f)

        for (q in 0 until template.numQuestions) {
            val col = q / rowsPerCol
            val rowInCol = q % rowsPerCol
            val rowY = gridTop + rowInCol * rowHeight + rowHeight / 2f
            val colLeft = 50f + col * colWidth

            canvas.drawText("${q + 1}.".padStart(3), colLeft, rowY + 6f, labelPaint)

            val bubblesStartX = colLeft + 48f
            val bubbleSpacing = (colWidth - 60f) / template.choicesPerQuestion
            for (c in 0 until template.choicesPerQuestion) {
                val cx = bubblesStartX + c * bubbleSpacing + bubbleSpacing / 2f
                canvas.drawCircle(cx, rowY, bubbleRadius, bubblePaint)
                val letterPaint = Paint(labelPaint).apply { textAlign = Paint.Align.CENTER; textSize = 14f }
                canvas.drawText(('A' + c).toString(), cx, rowY + bubbleRadius + 22f, letterPaint)
            }
        }
    }
}

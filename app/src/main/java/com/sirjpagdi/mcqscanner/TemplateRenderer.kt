package com.sirjpagdi.mcqscanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Renders a printable blank answer sheet bitmap from a saved Template.
 * Bubble positions come from SheetGeometry — the exact same geometry
 * OMRProcessor uses to grade a photographed copy of this sheet, so what
 * gets drawn here is guaranteed to match what gets read back later.
 */
object TemplateRenderer {

    private const val WIDTH = 1240   // ~A4 at 150dpi
    private const val HEIGHT = 1754

    fun render(template: Prefs.Template, sheetTitle: String): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        renderContent(canvas, WIDTH.toFloat(), HEIGHT.toFloat(), template, sheetTitle)
        return bitmap
    }

    private fun renderContent(canvas: Canvas, width: Float, height: Float, template: Prefs.Template, sheetTitle: String) {
        val black = Color.BLACK

        val titlePaint = Paint().apply {
            color = black
            textSize = width * 0.028f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val tableLabelPaint = Paint().apply {
            color = black
            textSize = width * 0.014f
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = black
            textSize = width * 0.0145f
            isAntiAlias = true
        }
        val columnLabelPaint = Paint(labelPaint).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val bubblePaint = Paint().apply {
            color = black
            style = Paint.Style.STROKE
            strokeWidth = width * 0.002f
            isAntiAlias = true
        }
        val tableBorderPaint = Paint().apply {
            color = black
            style = Paint.Style.STROKE
            strokeWidth = width * 0.0016f
            isAntiAlias = true
        }
        val cornerPaint = Paint().apply {
            color = black
            style = Paint.Style.FILL
        }

        // Corner alignment markers — the live scanning screen looks for these
        // same 4 black squares to know the sheet is properly framed.
        val markerSize = width * SheetGeometry.CORNER_MARKER_SIZE
        val inset = width * SheetGeometry.CORNER_MARKER_INSET
        canvas.drawRect(inset, inset, inset + markerSize, inset + markerSize, cornerPaint)
        canvas.drawRect(width - inset - markerSize, inset, width - inset, inset + markerSize, cornerPaint)
        canvas.drawRect(inset, height - inset - markerSize, inset + markerSize, height - inset, cornerPaint)
        canvas.drawRect(width - inset - markerSize, height - inset - markerSize, width - inset, height - inset, cornerPaint)

        val title = sheetTitle.ifBlank { "Untitled Test" }
        val titleY = height * SheetGeometry.TITLE_BAND * 0.6f
        canvas.drawText(title, width / 2f, titleY, titlePaint)

        // Bordered student-info table instead of underscore blanks.
        val tableTop = height * SheetGeometry.TITLE_BAND
        val tableBottom = height * (SheetGeometry.TITLE_BAND + SheetGeometry.INFO_TABLE_BAND)
        val tableLeft = width * SheetGeometry.MARGIN_X
        val tableRight = width - tableLeft
        val tableMidY = tableTop + (tableBottom - tableTop) / 2f
        val tableMidX = tableLeft + (tableRight - tableLeft) * 0.62f

        canvas.drawRect(tableLeft, tableTop, tableRight, tableBottom, tableBorderPaint)
        canvas.drawLine(tableLeft, tableMidY, tableRight, tableMidY, tableBorderPaint)
        canvas.drawLine(tableMidX, tableTop, tableMidX, tableBottom, tableBorderPaint)

        val cellPad = width * 0.012f
        val cellTextY1 = tableTop + (tableMidY - tableTop) / 2f + tableLabelPaint.textSize * 0.35f
        val cellTextY2 = tableMidY + (tableBottom - tableMidY) / 2f + tableLabelPaint.textSize * 0.35f
        canvas.drawText("Name:", tableLeft + cellPad, cellTextY1, tableLabelPaint)
        canvas.drawText("Section:", tableLeft + cellPad, cellTextY2, tableLabelPaint)
        canvas.drawText("Date:", tableMidX + cellPad, cellTextY1, tableLabelPaint)
        canvas.drawText("Score:", tableMidX + cellPad, cellTextY2, tableLabelPaint)

        val geometry = SheetGeometry.compute(template, width, height)

        // Column headers: one "A  B  C  D" row per column block.
        for (col in 0 until template.columns) {
            for (c in 0 until template.choicesPerQuestion) {
                val cx = SheetGeometry.bubbleCenterX(geometry, col, c, template.choicesPerQuestion, width)
                canvas.drawText(('A' + c).toString(), cx, geometry.gridTop - height * 0.012f, columnLabelPaint)
            }
        }

        for (q in 0 until template.numQuestions) {
            val col = q / geometry.rowsPerCol
            val rowInCol = q % geometry.rowsPerCol
            val rowY = SheetGeometry.rowCenterY(geometry, rowInCol)
            val colLeft = geometry.marginX + col * geometry.colWidth

            canvas.drawText("${q + 1}.", colLeft, rowY + labelPaint.textSize * 0.35f, labelPaint)

            for (c in 0 until template.choicesPerQuestion) {
                val cx = SheetGeometry.bubbleCenterX(geometry, col, c, template.choicesPerQuestion, width)
                canvas.drawCircle(cx, rowY, geometry.bubbleRadius, bubblePaint)
            }
        }
    }
}

package com.sirjpagdi.mcqscanner

/**
 * Both the printed answer sheet (TemplateRenderer) and the grader
 * (OMRProcessor) need to agree on exactly where each bubble sits.
 * Previously they computed this independently with different assumptions
 * about header height, which was the main cause of inaccurate scans —
 * the grader was sampling pixels that didn't line up with the bubbles
 * actually printed on the sheet.
 *
 * All measurements here are fractions (0..1) of the sheet's total width/
 * height, so the same geometry works whether it's being drawn onto a
 * printable canvas or read back from a warped photo of a filled-in sheet,
 * regardless of the absolute pixel size of either.
 */
object SheetGeometry {

    const val MARGIN_X = 0.045f
    const val TITLE_BAND = 0.065f
    const val INFO_TABLE_BAND = 0.10f
    const val COLUMN_HEADER_BAND = 0.035f
    const val FOOTER_MARGIN = 0.035f
    const val CORNER_MARKER_SIZE = 0.026f
    const val CORNER_MARKER_INSET = 0.018f
    const val LABEL_WIDTH_FRACTION = 0.055f // space reserved for "12." question number, per column

    data class Geometry(
        val gridTop: Float,
        val gridBottom: Float,
        val marginX: Float,
        val colWidth: Float,
        val rowHeight: Float,
        val rowsPerCol: Int,
        val bubbleRadius: Float
    )

    fun compute(template: Prefs.Template, width: Float, height: Float): Geometry {
        val marginX = width * MARGIN_X
        val gridTop = height * (TITLE_BAND + INFO_TABLE_BAND + COLUMN_HEADER_BAND)
        val gridBottom = height * (1f - FOOTER_MARGIN)
        val colWidth = (width - marginX * 2f) / template.columns
        val rowsPerCol = (template.numQuestions + template.columns - 1) / template.columns
        val rowHeight = (gridBottom - gridTop) / rowsPerCol
        val bubbleRadius = minOf(rowHeight * 0.28f, width * 0.014f)
        return Geometry(gridTop, gridBottom, marginX, colWidth, rowHeight, rowsPerCol, bubbleRadius)
    }

    /** X center of a given choice bubble (0=A, 1=B, ...) within its column. */
    fun bubbleCenterX(geometry: Geometry, colIndex: Int, choiceIndex: Int, choicesPerQuestion: Int, sheetWidth: Float): Float {
        val colLeft = geometry.marginX + colIndex * geometry.colWidth
        val labelWidth = sheetWidth * LABEL_WIDTH_FRACTION
        val bubblesStartX = colLeft + labelWidth
        val bubbleSpacing = (geometry.colWidth - labelWidth - geometry.marginX * 0.3f) / choicesPerQuestion
        return bubblesStartX + choiceIndex * bubbleSpacing + bubbleSpacing / 2f
    }

    fun bubbleSpacing(geometry: Geometry, choicesPerQuestion: Int, sheetWidth: Float): Float {
        val labelWidth = sheetWidth * LABEL_WIDTH_FRACTION
        return (geometry.colWidth - labelWidth - geometry.marginX * 0.3f) / choicesPerQuestion
    }

    fun rowCenterY(geometry: Geometry, rowInCol: Int): Float {
        return geometry.gridTop + rowInCol * geometry.rowHeight + geometry.rowHeight / 2f
    }
}

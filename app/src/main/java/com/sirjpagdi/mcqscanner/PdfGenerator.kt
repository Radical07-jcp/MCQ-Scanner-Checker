package com.sirjpagdi.mcqscanner

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    fun generateAndShare(context: Context, bitmap: Bitmap, fileNameNoExt: String) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = doc.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        doc.finishPage(page)

        val safeName = fileNameNoExt.ifBlank { "MCQ_Template" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val dir = File(context.cacheDir, "shared_pdfs").apply { mkdirs() }
        val file = File(dir, "$safeName.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share answer sheet PDF"))
    }
}

package com.sirjpagdi.mcqscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.ByteArrayOutputStream

object ImageUtils {

    /** Converts a JPEG-backed ImageProxy (from ImageCapture) into an upright Bitmap. */
    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val rotation = image.imageInfo.rotationDegrees
        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }

    fun bitmapToJpegBytes(bitmap: Bitmap, quality: Int = 90): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    fun imageProxyToGrayMat(image: ImageProxy): Mat {
        val plane = image.planes[0]
        val width = image.width
        val height = image.height
        val mat = Mat(height, width, CvType.CV_8UC1)
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val row = ByteArray(width)

        buffer.rewind()
        for (rowIndex in 0 until height) {
            buffer.get(row, 0, width)
            mat.put(rowIndex, 0, row)
            if (rowStride > width) {
                buffer.position(buffer.position() + rowStride - width)
            }
        }
        return mat
    }
}

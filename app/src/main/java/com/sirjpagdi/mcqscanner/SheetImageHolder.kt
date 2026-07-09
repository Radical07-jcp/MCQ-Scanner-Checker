package com.sirjpagdi.mcqscanner

import android.graphics.Bitmap

/** Simple in-memory holder to pass a captured bitmap between activities
 *  without hitting Intent extra size limits. */
object SheetImageHolder {
    var capturedBitmap: Bitmap? = null
    var warpedBitmap: Bitmap? = null
}

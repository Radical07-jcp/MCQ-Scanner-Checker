package com.sirjpagdi.mcqscanner

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class MCQScannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Single, sole place OpenCV's native library gets loaded. Loading it
        // from multiple places (Application + individual classes) is what
        // caused the "no implementation found for ... Mat.n_Mat()" crash —
        // competing load attempts stepping on each other.
        val loaded = OpenCVLoader.initDebug()
        Log.i("MCQScannerApplication", "OpenCV loaded: $loaded")
    }
}

package com.sirjpagdi.mcqscanner

import android.app.Application
import org.opencv.android.OpenCVLoader

class MCQScannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("opencv_java4")
        } catch (e: UnsatisfiedLinkError) {
            // Fallback to OpenCV loader if the native library is not already loaded.
        }

        OpenCVLoader.initDebug()
    }
}

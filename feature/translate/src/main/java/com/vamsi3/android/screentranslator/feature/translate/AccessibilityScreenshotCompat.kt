package com.vamsi3.android.screentranslator.feature.translate

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
internal object AccessibilityScreenshotCompat {
    fun takeScreenshot(
        service: AccessibilityService,
        onSuccess: (Bitmap) -> Unit,
        onFailure: () -> Unit,
    ) {
        service.takeScreenshot(
            Display.DEFAULT_DISPLAY,
            service.mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
                    val bitmap = Bitmap.wrapHardwareBuffer(
                        screenshotResult.hardwareBuffer,
                        screenshotResult.colorSpace
                    )

                    if (bitmap == null) {
                        onFailure()
                    } else {
                        onSuccess(bitmap)
                    }
                }

                override fun onFailure(errorCode: Int) {
                    onFailure()
                }
            }
        )
    }
}

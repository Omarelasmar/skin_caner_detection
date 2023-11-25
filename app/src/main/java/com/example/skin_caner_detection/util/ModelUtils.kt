package com.example.skin_caner_detection.util

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions

object ModelUtils {

    @JvmStatic
    fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when (rotation) {
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT

            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM

            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.TOP_LEFT

            else -> ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }

    @JvmStatic
    fun getScreenOrientation(activity: Activity) : Int {
        val outMetrics = DisplayMetrics()

        val display: Display?
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display = activity.display
            display?.getRealMetrics(outMetrics)
        } else {
            @Suppress("DEPRECATION")
            display = activity.windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(outMetrics)
        }

        return display?.rotation ?: 0
    }

}
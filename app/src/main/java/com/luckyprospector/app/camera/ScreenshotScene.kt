package com.luckyprospector.app.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import com.luckyprospector.app.domain.GeoPoint
import com.luckyprospector.app.inference.FrameTiling
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * A photo shown and analysed in place of the camera image, for store screenshots
 * without a lawn at hand. Only debug builds ever create one (`screenshotScene()`):
 * the model still runs on it for real, only the camera is replaced.
 *
 * The photo is center-cropped to each frame, like the preview crops the camera, so
 * the boxes drawn on screen land where the clovers are in the photo.
 */
class ScreenshotScene(val photo: Bitmap, val location: GeoPoint?) {
    private var painted: Triple<Int, Int, Int>? = null
    private var paintedRgba = ByteArray(0)

    /** The photo in a sensor-oriented RGBA frame of this size, as the camera delivers it. */
    @Synchronized
    fun rgbaFrame(width: Int, height: Int, rotationDegrees: Int): ByteArray {
        val key = Triple(width, height, rotationDegrees)
        if (painted == key) return paintedRgba
        val (uprightW, uprightH) = FrameTiling.uprightSize(width, height, rotationDegrees)
        val upright = cropped(uprightW, uprightH)
        val sensor = Bitmap.createBitmap(upright, 0, 0, uprightW, uprightH,
            Matrix().apply { postRotate(-rotationDegrees.toFloat()) }, true)
        paintedRgba = ByteArray(width * height * 4).also { sensor.copyPixelsToBuffer(ByteBuffer.wrap(it)) }
        painted = key
        return paintedRgba
    }

    /** The photo center-cropped and scaled to exactly [width] x [height]. */
    fun cropped(width: Int, height: Int): Bitmap {
        val aspect = width.toFloat() / height
        val (cropW, cropH) = if (photo.width.toFloat() / photo.height > aspect) (photo.height * aspect).roundToInt() to photo.height
        else photo.width to (photo.width / aspect).roundToInt()
        val source = Rect((photo.width - cropW) / 2, (photo.height - cropH) / 2, (photo.width + cropW) / 2, (photo.height + cropH) / 2)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            Canvas(it).drawBitmap(photo, source, Rect(0, 0, width, height), Paint(Paint.FILTER_BITMAP_FLAG))
        }
    }

    /** Replaces a captured photo with the scene, cropped to the same size. */
    fun overwrite(file: File, width: Int, height: Int) {
        file.outputStream().use { cropped(width, height).compress(Bitmap.CompressFormat.JPEG, 92, it) }
    }
}

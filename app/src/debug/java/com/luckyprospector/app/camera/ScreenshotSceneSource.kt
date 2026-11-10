package com.luckyprospector.app.camera

import android.content.Context
import android.graphics.BitmapFactory
import com.luckyprospector.app.domain.GeoPoint
import java.io.File

/**
 * Debug builds: files/screenshot-scene.jpg, when present, replaces the camera image
 * (see [ScreenshotScene]). An optional files/screenshot-scene.txt with "lat,lon"
 * replaces the location of what is saved, so screenshots never show where the phone is.
 * Remove the files to get the camera back.
 */
internal fun screenshotScene(context: Context): ScreenshotScene? {
    val photo = File(context.filesDir, "screenshot-scene.jpg").takeIf { it.isFile }
        ?.let { BitmapFactory.decodeFile(it.path) } ?: return null
    val location = File(context.filesDir, "screenshot-scene.txt").takeIf { it.isFile }?.readText()
        ?.split(',')?.mapNotNull { it.trim().toDoubleOrNull() }?.takeIf { it.size == 2 }
        ?.let { (lat, lon) -> GeoPoint(lat, lon, 5f) }
    return ScreenshotScene(photo, location)
}

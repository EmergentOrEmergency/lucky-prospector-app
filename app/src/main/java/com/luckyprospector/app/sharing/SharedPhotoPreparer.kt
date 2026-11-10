package com.luckyprospector.app.sharing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.ShareDecision
import com.luckyprospector.app.domain.SharePhotoPolicy
import com.luckyprospector.app.inference.DetectionFrame
import com.luckyprospector.app.inference.Detector
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The outcome of checking a photo before it may be shared. */
sealed interface SharePreparation {
    /**
     * [jpeg] is the only image that would leave the phone: a square crop around the
     * detections, re-encoded from pixels, so it carries no EXIF at all.
     */
    data class Ready(val type: FindingType, val jpeg: ByteArray, val downgraded: Boolean) : SharePreparation
    data object NoClover : SharePreparation
    data object ModelUnavailable : SharePreparation
    data object PhotoUnreadable : SharePreparation
}

class SharedPhotoPreparer(private val context: Context) {

    suspend fun prepare(photoUri: String, requested: FindingType, detector: Detector?): SharePreparation {
        if (detector == null) return SharePreparation.ModelUnavailable
        val photo = withContext(Dispatchers.IO) { decodeUpright(context, Uri.parse(photoUri), DECODE_SIDE) }
            ?: return SharePreparation.PhotoUnreadable
        try {
            val detections = detector.detect(photo.toDetectionFrame())
            return when (val decision = SharePhotoPolicy.decide(requested, detections)) {
                ShareDecision.NoClover -> SharePreparation.NoClover
                is ShareDecision.Allowed -> {
                    val jpeg = withContext(Dispatchers.Default) { encodeCrop(photo, decision) }
                    SharePreparation.Ready(decision.type, jpeg, decision.downgraded)
                }
            }
        } finally {
            photo.recycle()
        }
    }

    private fun encodeCrop(photo: Bitmap, decision: ShareDecision.Allowed): ByteArray {
        val rect = SharePhotoPolicy.cropRect(decision.photoRegion, photo.width, photo.height)
        val scale = OUTPUT_SIDE.toFloat() / rect.width
        val crop = Bitmap.createBitmap(
            photo, rect.left, rect.top, rect.width, rect.height,
            Matrix().apply { if (scale < 1f) postScale(scale, scale) },
            true,
        )
        val jpeg = ByteArrayOutputStream().use { out ->
            crop.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        if (crop !== photo) crop.recycle()

        // Bitmap.compress writes no EXIF, so this cannot fail today. It is checked
        // anyway: a location leaking through the photo would undo the coarsening of
        // the pin, and a future change to the encoding must not be able to do that.
        val exif = ExifInterface(ByteArrayInputStream(jpeg))
        @Suppress("DEPRECATION")
        val hasGps = exif.getLatLong(FloatArray(2)) || exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null
        check(!hasGps) {
            "Shared photo must not carry GPS data"
        }
        check(jpeg.size <= MAX_BYTES) { "Shared photo too large: ${jpeg.size} bytes" }
        return jpeg
    }

    private fun Bitmap.toDetectionFrame(): DetectionFrame {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        val rgb = ByteArray(pixels.size * 3)
        val luminance = ByteArray(pixels.size)
        pixels.forEachIndexed { index, pixel ->
            val r = pixel shr 16 and 0xff
            val g = pixel shr 8 and 0xff
            val b = pixel and 0xff
            rgb[index * 3] = r.toByte()
            rgb[index * 3 + 1] = g.toByte()
            rgb[index * 3 + 2] = b.toByte()
            luminance[index] = ((r * 77 + g * 150 + b * 29) shr 8).toByte()
        }
        return DetectionFrame(luminance, width, height, rotationDegrees = 0, timestampMillis = 0, rgb = rgb)
    }

    companion object {
        const val OUTPUT_SIDE = 384
        private const val JPEG_QUALITY = 80
        private const val DECODE_SIDE = 1600

        /** Mirrored in firestore.rules (jpeg.size()). */
        const val MAX_BYTES = 90_000
    }
}

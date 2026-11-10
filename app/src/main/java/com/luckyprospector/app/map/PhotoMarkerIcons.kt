package com.luckyprospector.app.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import kotlin.math.max

/** Where a pin's photo comes from. */
internal sealed interface MarkerPhoto {
    val key: String

    /** A finding's own photo on this device (private map). */
    data class Local(val uri: String) : MarkerPhoto {
        override val key get() = "local:$uri"
    }

    /** The cropped, EXIF-free photo of a shared pin, fetched on demand. */
    data class Shared(val sightingId: String) : MarkerPhoto {
        override val key get() = "shared:$sightingId"
    }
}

/**
 * Round photo pins. MapLibre receives a bitmap drawn on the phone, never a URI,
 * a file or the photo bytes.
 */
internal object PhotoMarkerIcons {

    /**
     * A circle with the photo, a coloured ring and a short tail whose tip is the
     * marker position (MapLibre anchors marker icons at their bottom centre).
     */
    fun create(context: Context, photoUri: String, accentArgb: Int): Bitmap? {
        val diameter = diameterPx(context)
        return frame(context, decodeSquare(context, Uri.parse(photoUri), diameter) ?: return null, accentArgb)
    }

    fun create(context: Context, jpeg: ByteArray, accentArgb: Int): Bitmap? {
        val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return null
        val diameter = diameterPx(context)
        val side = minOf(decoded.width, decoded.height)
        val scale = diameter.toFloat() / side
        val square = Bitmap.createBitmap(
            decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side,
            Matrix().apply { postScale(scale, scale) }, true,
        )
        if (square !== decoded) decoded.recycle()
        return frame(context, square, accentArgb)
    }

    private fun diameterPx(context: Context) = (52 * context.resources.displayMetrics.density).toInt()

    private fun frame(context: Context, photo: Bitmap, accentArgb: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val diameter = diameterPx(context)
        val ring = 3 * density
        val tail = 9 * density

        val output = Bitmap.createBitmap(diameter, (diameter + tail).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val radius = diameter / 2f
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentArgb }

        val tailPath = Path().apply {
            moveTo(radius - tail * .8f, diameter - tail * .9f)
            lineTo(radius + tail * .8f, diameter - tail * .9f)
            lineTo(radius, diameter + tail)
            close()
        }
        canvas.drawPath(tailPath, accent)
        canvas.drawCircle(radius, radius, radius, accent)
        canvas.drawCircle(radius, radius, radius - ring, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })

        val inner = radius - ring * 1.8f
        val shader = BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            val scale = (inner * 2) / photo.width
            setLocalMatrix(Matrix().apply {
                setScale(scale, scale)
                postTranslate(radius - inner, radius - inner)
            })
        }
        canvas.drawCircle(radius, radius, inner, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        drawSlideSheen(canvas, radius, inner)
        photo.recycle()
        return output
    }

    /** The same glass-slide light as the in-app previews, clipped to the photo circle. */
    private fun drawSlideSheen(canvas: Canvas, center: Float, radius: Float) {
        val left = center - radius
        val right = center + radius
        val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                left, left, right, right,
                intArrayOf(0x66FFFFFF, 0x1AFFFFFF, 0x00FFFFFF, 0x38FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, .32f, .44f, .48f, .56f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(center, center, radius, sheen)
    }

    /** Decodes a small, upright, centre-cropped square without loading the full photo. */
    private fun decodeSquare(context: Context, uri: Uri, size: Int): Bitmap? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= size) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val side = minOf(decoded.width, decoded.height)
        val scale = size.toFloat() / max(side, 1)
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val square = Bitmap.createBitmap(
            decoded,
            (decoded.width - side) / 2,
            (decoded.height - side) / 2,
            side,
            side,
            matrix,
            true,
        )
        if (square !== decoded) decoded.recycle()
        square
    }.getOrNull()
}

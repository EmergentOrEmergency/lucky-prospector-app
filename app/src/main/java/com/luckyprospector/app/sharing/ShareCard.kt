package com.luckyprospector.app.sharing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The image a user sends to friends: their finding as a polaroid on a forest-green
 * background, with a lucky wish written by hand. Shared through the system share
 * sheet, so it reaches WhatsApp, Facebook, Instagram or anything else installed
 * without any social SDK in the app.
 *
 * The caption is a wish from the app, never the user's own private wish. The image
 * is drawn from pixels and re-encoded, so it carries no EXIF and no GPS position.
 */
object ShareCard {

    private const val WIDTH = 1080
    private const val HEIGHT = 1350
    private const val FRAME = 880
    private const val BORDER = 40
    private const val CAPTION_AREA = 200

    suspend fun render(context: Context, photoUri: String, caption: String, footer: String): File =
        withContext(Dispatchers.Default) {
            val photo = withContext(Dispatchers.IO) { decodeUpright(context, Uri.parse(photoUri), 1200) }
                ?: error("Photo unreadable")
            val card = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(card)
            canvas.drawPaint(Paint().apply {
                shader = LinearGradient(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                    Color.rgb(5, 47, 32), Color.rgb(21, 90, 55), Shader.TileMode.CLAMP)
            })

            val side = FRAME - 2 * BORDER
            val frameHeight = BORDER + side + CAPTION_AREA
            val left = (WIDTH - FRAME) / 2f
            val top = (HEIGHT - frameHeight) / 2f - 50f
            canvas.save()
            canvas.rotate(-2f, WIDTH / 2f, top + frameHeight / 2f)
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 0, 0, 0) }
            canvas.drawRect(left + 10, top + 16, left + FRAME + 10, top + frameHeight + 16, shadow)
            canvas.drawRect(left, top, left + FRAME, top + frameHeight, Paint().apply { color = Color.rgb(251, 250, 245) })

            // Centre-crop the photo into the square window.
            val crop = minOf(photo.width, photo.height)
            val source = Rect((photo.width - crop) / 2, (photo.height - crop) / 2, (photo.width + crop) / 2, (photo.height + crop) / 2)
            val window = RectF(left + BORDER, top + BORDER, left + BORDER + side, top + BORDER + side)
            canvas.drawBitmap(photo, source, window, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.drawRect(window, Paint().apply {
                shader = LinearGradient(window.left, window.top, window.right, window.bottom,
                    intArrayOf(0x55FFFFFF, 0x14FFFFFF, 0x00FFFFFF), floatArrayOf(0f, .32f, .55f), Shader.TileMode.CLAMP)
            })

            val handwriting = Typeface.create("casual", Typeface.NORMAL)
            val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(38, 48, 42)
                textSize = 50f
                typeface = handwriting
            }
            val layout = StaticLayout.Builder.obtain(caption, 0, caption.length, captionPaint, side)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
            canvas.save()
            canvas.translate(left + BORDER, top + BORDER + side + (CAPTION_AREA - layout.height) / 2f)
            layout.draw(canvas)
            canvas.restore()
            canvas.restore()

            val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(230, 255, 237, 176)
                textSize = 36f
                textAlign = Paint.Align.CENTER
                typeface = handwriting
            }
            canvas.drawText("☘  $footer", WIDTH / 2f, HEIGHT - 70f, footerPaint)
            photo.recycle()

            val jpeg = ByteArrayOutputStream().use { out ->
                card.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.toByteArray()
            }
            card.recycle()
            requireNoLocation(jpeg)

            val directory = File(context.cacheDir, SHARE_DIRECTORY).apply { mkdirs() }
            // One file at a time: older cards are not kept around.
            directory.listFiles()?.forEach(File::delete)
            File(directory, "lucky-${System.currentTimeMillis()}.jpg").apply { writeBytes(jpeg) }
        }

    fun shareIntent(context: Context, card: File, text: String, chooserTitle: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", card)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, chooserTitle)
    }

    internal const val SHARE_DIRECTORY = "shared_cards"
}

/** A shared image must never carry the place where the photo was taken. */
internal fun requireNoLocation(jpeg: ByteArray) {
    val exif = ExifInterface(ByteArrayInputStream(jpeg))
    @Suppress("DEPRECATION")
    val hasGps = exif.getLatLong(FloatArray(2)) || exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null
    check(!hasGps) { "Shared image must not carry GPS data" }
}

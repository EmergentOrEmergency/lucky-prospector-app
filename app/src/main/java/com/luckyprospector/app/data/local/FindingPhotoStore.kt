package com.luckyprospector.app.data.local

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.ExifInterface
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.luckyprospector.app.domain.GeoPoint
import com.luckyprospector.app.sharing.decodeUpright
import java.io.File
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StoredPhotoUris(
    val privateImageUri: String,
    /** Null for a photo picked from the gallery, which is not copied there again. */
    val galleryImageUri: String?,
)

private const val PICKED_MAX_SIDE = 2400

/** Owns the two durable copies required by the offline archive. */
class FindingPhotoStore(private val context: Context) {
    suspend fun importJpeg(source: Uri, findingId: String): StoredPhotoUris = withContext(Dispatchers.IO) {
        val privateDirectory = File(context.filesDir, "findings").apply { mkdirs() }
        val privateFile = File(privateDirectory, "$findingId.jpg")
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Impossibile leggere la foto selezionata" }
            privateFile.outputStream().use(input::copyTo)
        }

        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "lucky-$findingId.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lucky Prospector")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                put(MediaStore.Images.Media.DATE_ADDED, Instant.now().epochSecond)
            }
            val galleryUri = requireNotNull(
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values),
            ) { "Impossibile pubblicare la foto nella galleria" }
            try {
                context.contentResolver.openOutputStream(galleryUri).use { output ->
                    requireNotNull(output) { "Impossibile scrivere la foto nella galleria" }
                    privateFile.inputStream().use { it.copyTo(output) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.update(
                        galleryUri,
                        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                        null,
                        null,
                    )
                }
                StoredPhotoUris(Uri.fromFile(privateFile).toString(), galleryUri.toString())
            } catch (error: Throwable) {
                context.contentResolver.delete(galleryUri, null, null)
                throw error
            }
        } catch (error: Throwable) {
            privateFile.delete()
            throw error
        }
    }

    /**
     * Imports a photo the user picked from the gallery for a finding added by hand.
     * It is already in the gallery, so no second copy is made there. The private
     * copy is re-encoded upright from pixels, so it keeps no EXIF of its own.
     */
    suspend fun importPicked(source: Uri, findingId: String): StoredPhotoUris = withContext(Dispatchers.IO) {
        val bitmap = requireNotNull(decodeUpright(context, source, maxSide = PICKED_MAX_SIDE)) { "Impossibile leggere la foto selezionata" }
        val privateFile = File(File(context.filesDir, "findings").apply { mkdirs() }, "$findingId.jpg")
        try {
            privateFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        } catch (error: Throwable) {
            privateFile.delete()
            throw error
        } finally {
            bitmap.recycle()
        }
        StoredPhotoUris(Uri.fromFile(privateFile).toString(), galleryImageUri = null)
    }

    /** The GPS position stored in a picked photo, if the camera recorded one. */
    suspend fun locationOf(source: Uri): GeoPoint? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(source)?.use { input ->
                val coordinates = FloatArray(2)
                @Suppress("DEPRECATION")
                if (ExifInterface(input).getLatLong(coordinates)) {
                    GeoPoint(coordinates[0].toDouble(), coordinates[1].toDouble(), accuracyMeters = null)
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    suspend fun delete(uris: StoredPhotoUris) = delete(uris.privateImageUri, uris.galleryImageUri)

    /** Removes both durable copies; either URI may already have been removed outside the app. */
    suspend fun delete(privateImageUri: String, galleryImageUri: String?) = withContext(Dispatchers.IO) {
        runCatching { Uri.parse(privateImageUri).path?.let(::File)?.delete() }
        galleryImageUri?.let { uri ->
            runCatching { context.contentResolver.delete(Uri.parse(uri), null, null) }
        }
        Unit
    }
}

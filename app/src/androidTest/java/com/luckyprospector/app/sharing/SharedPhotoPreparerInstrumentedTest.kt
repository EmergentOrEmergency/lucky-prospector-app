package com.luckyprospector.app.sharing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.inference.Detection
import com.luckyprospector.app.inference.DetectionLabel
import com.luckyprospector.app.inference.Detector
import com.luckyprospector.app.inference.NormalizedBox
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The shared photo must never carry the position the pin's coarsening hides.
 * Runs on a device because it needs the real JPEG encoder and EXIF reader.
 */
@RunWith(AndroidJUnit4::class)
class SharedPhotoPreparerInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun photoWithGps(): File {
        val file = File(context.cacheDir, "gps-test.jpg")
        val bitmap = Bitmap.createBitmap(2000, 1500, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(40, 140, 60)) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "44/1,38/1,5000/100")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "10/1,55/1,3000/100")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
            setAttribute(ExifInterface.TAG_MAKE, "TestPhone")
            saveAttributes()
        }
        @Suppress("DEPRECATION")
        assertTrue("fixture must carry GPS", ExifInterface(file.absolutePath).getLatLong(FloatArray(2)))
        return file
    }

    private val seesFourLeaf = Detector {
        listOf(Detection(DetectionLabel.FOUR_LEAF_CLOVER, .9f, NormalizedBox(.45f, .45f, .55f, .55f)))
    }

    @Test
    fun sharedCropCarriesNoExifAtAll() = runBlocking {
        val prepared = SharedPhotoPreparer(context)
            .prepare(Uri.fromFile(photoWithGps()).toString(), FindingType.FOUR_LEAF_CLOVER, seesFourLeaf)

        prepared as SharePreparation.Ready
        assertEquals(FindingType.FOUR_LEAF_CLOVER, prepared.type)
        val exif = ExifInterface(ByteArrayInputStream(prepared.jpeg))
        @Suppress("DEPRECATION")
        assertFalse(exif.getLatLong(FloatArray(2)))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(exif.getAttribute(ExifInterface.TAG_MAKE))

        val decoded = BitmapFactory.decodeByteArray(prepared.jpeg, 0, prepared.jpeg.size)
        assertEquals(decoded.width, decoded.height)
        assertTrue(decoded.width <= SharedPhotoPreparer.OUTPUT_SIDE)
        assertTrue(prepared.jpeg.size <= SharedPhotoPreparer.MAX_BYTES)
    }

    @Test
    fun nothingIsPreparedWithoutClovers() = runBlocking {
        val prepared = SharedPhotoPreparer(context)
            .prepare(Uri.fromFile(photoWithGps()).toString(), FindingType.CLOVER_PATCH, Detector { emptyList() })
        assertEquals(SharePreparation.NoClover, prepared)
    }

    @Test
    fun noModelMeansNoSharing() = runBlocking {
        val prepared = SharedPhotoPreparer(context)
            .prepare(Uri.fromFile(photoWithGps()).toString(), FindingType.CLOVER_PATCH, null)
        assertEquals(SharePreparation.ModelUnavailable, prepared)
    }
}

package com.luckyprospector.app.sharing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** The card sent to friends must never reveal where the photo was taken. */
@RunWith(AndroidJUnit4::class)
class ShareCardInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun cardCarriesNoLocationAndHasTheExpectedSize() = runBlocking {
        val photo = File(context.cacheDir, "card-source.jpg")
        Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(60, 150, 70)) }
            .let { bitmap -> photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) } }
        ExifInterface(photo.absolutePath).apply {
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "44/1,38/1,5000/100")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "10/1,55/1,3000/100")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
            saveAttributes()
        }

        val card = ShareCard.render(context, Uri.fromFile(photo).toString(), "May this little leaf bring you a big smile.", "Found with Lucky Prospector")

        val exif = ExifInterface(card.absolutePath)
        @Suppress("DEPRECATION")
        assertFalse(exif.getLatLong(FloatArray(2)))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        val decoded = BitmapFactory.decodeFile(card.absolutePath)
        assertEquals(1080, decoded.width)
        assertEquals(1350, decoded.height)
        assertEquals(ShareCard.SHARE_DIRECTORY, card.parentFile?.name)
    }
}

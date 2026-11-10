package com.luckyprospector.app.inference

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Times one search frame at the fixed resolutions of the settings, on the GPU, over
 * the findings saved on the device (tag BENCH), and logs what each one finds.
 * Without saved findings there is nothing to time and the test passes trivially.
 */
@RunWith(AndroidJUnit4::class)
class TilingBenchmarkInstrumentedTest {
    @Test fun timeTheResolutions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val photos = File(context.filesDir, "findings").listFiles().orEmpty().filter { it.extension == "jpg" }.take(3)
        val detector = ModelBundleLoader(context).load(useGpu = true)
        try {
            photos.forEach { file ->
                val photo = BitmapFactory.decodeFile(file.absolutePath)
                listOf(640 to 480, 1280 to 960, 1920 to 1080, 1920 to 1440).forEach { (width, height) ->
                    // Landscape sensor frame, rotated upright like the camera's.
                    val landscape = if (photo.width >= photo.height) photo else Bitmap.createBitmap(
                        photo, 0, 0, photo.width, photo.height, android.graphics.Matrix().apply { postRotate(-90f) }, true,
                    )
                    val frame = Bitmap.createScaledBitmap(landscape, width, height, true).toRgbaFrame()
                    repeat(2) { detector.detect(frame) }
                    var total = 0L
                    lateinit var found: List<Detection>
                    repeat(5) {
                        val start = System.nanoTime()
                        found = detector.detect(frame)
                        total += System.nanoTime() - start
                    }
                    val ms = total / 5_000_000
                    Log.i("BENCH", "${file.name} ${width}x$height tiles=${frame.tilesPerSide} ${ms}ms (%.1f fps) ".format(1000f / ms) +
                        found.sortedByDescending { it.confidence }.take(4).joinToString { "${it.label.name.take(4)} %.2f".format(it.confidence) })
                }
            }
        } finally {
            (detector as? Closeable)?.close()
        }
    }

    private fun Bitmap.toRgbaFrame(): DetectionFrame {
        val rgba = ByteArray(width * height * 4).also { copyPixelsToBuffer(ByteBuffer.wrap(it)) }
        return DetectionFrame(ByteArray(0), width, height, 90, 0, rgba = rgba, tilesPerSide = FrameTiling.perSideFor(width, height))
    }
}

package com.luckyprospector.app.inference

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.Closeable
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the model on the CPU and on the GPU over the findings saved on the device,
 * logs the times (tag BENCH) and checks both see the same clovers. Without saved
 * findings there is nothing to compare and the test passes trivially.
 */
@RunWith(AndroidJUnit4::class)
class GpuBenchmarkInstrumentedTest {
    @Test fun compareCpuAndGpu() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val photos = File(context.filesDir, "findings").listFiles().orEmpty().filter { it.extension == "jpg" }
        val cpu = ModelBundleLoader(context).load(useGpu = false)
        val gpu = ModelBundleLoader(context).load(useGpu = true)
        try {
            photos.forEach { file ->
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = 4 })
                val scaled = Bitmap.createScaledBitmap(bitmap, 640, 640 * bitmap.height / bitmap.width, true)
                val frame = scaled.toFrame()
                repeat(2) { cpu.detect(frame); gpu.detect(frame) }
                var tc = 0L; var tg = 0L
                lateinit var dc: List<Detection>; lateinit var dg: List<Detection>
                repeat(5) {
                    val a = System.nanoTime(); dc = cpu.detect(frame); val b = System.nanoTime(); dg = gpu.detect(frame); val c = System.nanoTime()
                    tc += b - a; tg += c - b
                }
                Log.i("BENCH", "${file.name} cpu=${tc / 5_000_000}ms gpu=${tg / 5_000_000}ms")
                Log.i("BENCH", "  cpu ${dc.fmt()}")
                Log.i("BENCH", "  gpu ${dg.fmt()}")
                assertEquals(dc.size, dg.size)
                dc.sortedByDescending { it.confidence }.zip(dg.sortedByDescending { it.confidence }).forEach { (c, g) ->
                    assertEquals(c.label, g.label)
                    assertEquals(c.confidence, g.confidence, .05f)
                }
            }
        } finally {
            (cpu as? Closeable)?.close(); (gpu as? Closeable)?.close()
        }
    }

    private fun List<Detection>.fmt() = sortedByDescending { it.confidence }.take(6)
        .joinToString { "${it.label.name.take(4)} %.2f @%.2f,%.2f".format(it.confidence, it.box.left, it.box.top) }

    private fun Bitmap.toFrame(): DetectionFrame {
        val pixels = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
        val rgb = ByteArray(pixels.size * 3)
        pixels.forEachIndexed { i, p ->
            rgb[i * 3] = (p shr 16).toByte(); rgb[i * 3 + 1] = (p shr 8).toByte(); rgb[i * 3 + 2] = p.toByte()
        }
        return DetectionFrame(ByteArray(pixels.size), width, height, 0, 0, rgb)
    }
}

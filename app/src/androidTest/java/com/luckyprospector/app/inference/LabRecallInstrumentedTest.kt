package com.luckyprospector.app.inference

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
 * Four-leaf recall of the installed model through the app's own detector, on labelled
 * lab test photos pushed to files/labtest (image.jpg + image.txt in YOLO format).
 *
 * Each photo is fed the way the camera would: cropped to the stream's aspect, scaled
 * to the stream size, turned to the sensor's landscape orientation (rotation 90), and
 * tiled like the search. Logs (tag LABRECALL) how many labelled four-leaf clovers are
 * found at the candidate (0.5) and confirmation (0.7) thresholds. Without photos the
 * test passes trivially.
 */
@RunWith(AndroidJUnit4::class)
class LabRecallInstrumentedTest {
    private data class Stream(val name: String, val width: Int, val height: Int)

    @Test fun recallThroughTheAppPipeline() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val photos = File(context.filesDir, "labtest").listFiles().orEmpty().filter { it.extension == "jpg" }.sorted()
        if (photos.isEmpty()) return@runBlocking
        val detector = ModelBundleLoader(context).load(useGpu = true)
        val streams = listOf(Stream("640x480", 640, 480), Stream("1920x1080", 1920, 1080), Stream("4000x3000", 4000, 3000))
        try {
            streams.forEach { stream ->
                var labelled = 0
                var found50 = 0
                var found70 = 0
                var wrongFour = 0
                photos.forEach { file ->
                    val truth = File(file.path.removeSuffix(".jpg") + ".txt").readLines()
                        .map { it.trim().split(Regex("\\s+")) }.filter { it.size >= 5 }
                        .map { it[0].toInt() to it.drop(1).take(4).map(String::toFloat) }
                    val (frame, boxes) = cameraFrame(BitmapFactory.decodeFile(file.path), truth, stream)
                    val detections = detector.detect(frame)
                    val fours = boxes.filter { it.first == 1 }.map { it.second }
                    labelled += fours.size
                    fours.forEach { box ->
                        val best = detections.filter { it.label == DetectionLabel.FOUR_LEAF_CLOVER && it.box.intersectionOverUnion(box) > .3f }
                            .maxOfOrNull { it.confidence } ?: 0f
                        if (best >= .5f) found50++
                        if (best >= .7f) found70++
                    }
                    wrongFour += detections.count { d ->
                        d.label == DetectionLabel.FOUR_LEAF_CLOVER && d.confidence >= .7f &&
                            fours.none { d.box.intersectionOverUnion(it) > .3f }
                    }
                    Log.i("LABRECALL", "${stream.name} ${file.name} tiles=${frame.tilesPerSide} labelled=${fours.size} " +
                        detections.filter { it.label == DetectionLabel.FOUR_LEAF_CLOVER }
                            .joinToString { "4@%.2f".format(it.confidence) })
                }
                Log.i("LABRECALL", "SUMMARY ${stream.name}: four-leaf labelled=$labelled found@0.5=$found50 found@0.7=$found70 unlabelled-four@0.7=$wrongFour")
            }
        } finally {
            (detector as? Closeable)?.close()
        }
    }

    /** The photo as the camera would deliver it, and its labels in upright normalized coordinates. */
    private fun cameraFrame(photo: Bitmap, truth: List<Pair<Int, List<Float>>>, stream: Stream): Pair<DetectionFrame, List<Pair<Int, NormalizedBox>>> {
        // Upright stream is portrait: height x width of the landscape sensor size.
        val uprightW = stream.height
        val uprightH = stream.width
        val targetAspect = uprightW.toFloat() / uprightH
        val photoAspect = photo.width.toFloat() / photo.height
        val (cropW, cropH) = if (photoAspect > targetAspect) (photo.height * targetAspect).toInt() to photo.height
        else photo.width to (photo.width / targetAspect).toInt()
        val cropX = (photo.width - cropW) / 2
        val cropY = (photo.height - cropH) / 2
        val upright = Bitmap.createScaledBitmap(Bitmap.createBitmap(photo, cropX, cropY, cropW, cropH), uprightW, uprightH, true)
        val boxes = truth.map { (cls, v) ->
            val (cx, cy, w, h) = v
            fun x(value: Float) = ((value * photo.width - cropX) / cropW).coerceIn(0f, 1f)
            fun y(value: Float) = ((value * photo.height - cropY) / cropH).coerceIn(0f, 1f)
            cls to NormalizedBox(x(cx - w / 2), y(cy - h / 2), x(cx + w / 2), y(cy + h / 2))
        }.filter { it.second.area > 0f }
        // The sensor delivers landscape frames that the app turns by 90 degrees.
        val sensor = Bitmap.createBitmap(upright, 0, 0, uprightW, uprightH, Matrix().apply { postRotate(-90f) }, true)
        val rgba = ByteArray(sensor.width * sensor.height * 4).also { sensor.copyPixelsToBuffer(ByteBuffer.wrap(it)) }
        val frame = DetectionFrame(
            ByteArray(0), sensor.width, sensor.height, rotationDegrees = 90, timestampMillis = 0,
            rgba = rgba, tilesPerSide = FrameTiling.perSideFor(sensor.width, sensor.height),
        )
        return frame to boxes
    }
}

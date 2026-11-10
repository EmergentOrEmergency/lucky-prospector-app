package com.luckyprospector.app.inference

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.gpu.GpuDelegateFactory

fun interface YoloxModelRunner {
    fun run(input: FloatArray): FloatArray
}

class LiteRtYoloxModelRunner(
    model: ByteBuffer,
    metadata: ModelMetadata,
    /**
     * Where the GPU kernels compiled for this model are cached, or null to run on the
     * CPU only. Compiling takes seconds on the first launch; the cache makes the next
     * launches start almost at once.
     */
    gpuCacheDirectory: File? = null,
) : YoloxModelRunner, Closeable {
    private val rows = 8_400
    private val columns = 5 + metadata.classes.size

    // On the GPU the model runs 2-3 times faster than on 4 CPU threads, with the same
    // detections to the second decimal (measured on a Galaxy S23 Ultra). Devices
    // without a supported GPU, or where the delegate fails, fall back to the CPU.
    private val gpu: GpuDelegate? = gpuCacheDirectory?.let { cache ->
        runCatching {
            val compatibility = CompatibilityList()
            try {
                if (!compatibility.isDelegateSupportedOnThisDevice) return@runCatching null
                GpuDelegate(
                    compatibility.bestOptionsForThisDevice
                        .setInferencePreference(GpuDelegateFactory.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED)
                        .setSerializationParams(cache.absolutePath, metadata.artifactSha256.take(16)),
                )
            } finally {
                compatibility.close()
            }
        }.getOrNull()
    }
    private val interpreter = runCatching {
        Interpreter(model, Interpreter.Options().apply { gpu?.let(::addDelegate) ?: setNumThreads(4) })
    }.getOrElse {
        gpu?.close()
        Interpreter(model, Interpreter.Options().apply { setNumThreads(4) })
    }
    private val inputBuffer = ByteBuffer.allocateDirect(
        metadata.input.width * metadata.input.height * metadata.input.channels * Float.SIZE_BYTES,
    ).order(ByteOrder.nativeOrder())
    private val outputBuffer = ByteBuffer.allocateDirect(rows * columns * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())

    init {
        require(interpreter.inputTensorCount == 1 && interpreter.outputTensorCount == 1) { "Expected one input and one output tensor" }
        require(interpreter.getInputTensor(0).dataType() == DataType.FLOAT32) { "Expected float32 model input" }
        require(interpreter.getOutputTensor(0).dataType() == DataType.FLOAT32) { "Expected float32 model output" }
        require(interpreter.getInputTensor(0).shape().contentEquals(intArrayOf(1, metadata.input.height, metadata.input.width, 3))) {
            "Unexpected YOLOX input tensor shape"
        }
        require(interpreter.getOutputTensor(0).shape().contentEquals(intArrayOf(1, rows, columns))) {
            "Expected YOLOX output tensor [1,$rows,$columns]"
        }
    }

    @Synchronized
    override fun run(input: FloatArray): FloatArray {
        inputBuffer.rewind()
        inputBuffer.asFloatBuffer().put(input)
        outputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
        return FloatArray(rows * columns).also { outputBuffer.asFloatBuffer().get(it) }
    }

    override fun close() {
        interpreter.close()
        gpu?.close()
    }
}

data class LetterboxTransform(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val scale: Float,
    val resizedWidth: Int,
    val resizedHeight: Int,
) {
    companion object {
        fun forSize(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): LetterboxTransform {
            require(sourceWidth > 0 && sourceHeight > 0 && targetWidth > 0 && targetHeight > 0)
            val scale = minOf(targetWidth.toFloat() / sourceWidth, targetHeight.toFloat() / sourceHeight)
            return LetterboxTransform(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                scale = scale,
                resizedWidth = (sourceWidth * scale).roundToInt().coerceAtLeast(1),
                resizedHeight = (sourceHeight * scale).roundToInt().coerceAtLeast(1),
            )
        }
    }
}

class YoloxTfliteDetector(
    private val metadata: ModelMetadata,
    private val runner: YoloxModelRunner,
) : Detector, Closeable {
    private val decoder = YoloxDecoder(metadata.validate())
    override val sizeGuide: SizeGuide? = metadata.sizeGuide

    // Reused for every tile: up to nine per frame, a few megabytes each, would
    // otherwise keep the garbage collector busy. Guarded by the lock in [detect].
    private val lock = Any()
    private val target = Bitmap.createBitmap(metadata.input.width, metadata.input.height, Bitmap.Config.ARGB_8888)
    private val targetPixels = IntArray(metadata.input.width * metadata.input.height)
    private val input = FloatArray(targetPixels.size * 3)

    override suspend fun detect(frame: DetectionFrame): List<Detection> = synchronized(lock) {
        val upright = uprightBitmap(frame)
        try {
            val tiling = FrameTiling(upright.width, upright.height, frame.tilesPerSide)
            val found = tiling.origins.flatMap { (x, y) ->
                val prepared = letterbox(upright, Rect(x, y, x + tiling.tileWidth, y + tiling.tileHeight))
                decoder.decode(runner.run(prepared.input), prepared.transform).map { it.fromTile(tiling, x, y) }
            }
            if (tiling.perSide == 1) return found.map { it.detection }
            // The whole frame too: a clover as big as the guide is wider than the tiles'
            // overlap, so every tile cuts it and a cut four-leaf clover looks like a
            // three-leaf one (lab photos: 40/40 four-leaf clovers found in one pass,
            // 21/40 with 3x3 tiles alone). Tiles only add the small, distant clovers.
            val whole = letterbox(upright, Rect(0, 0, upright.width, upright.height)).let { prepared ->
                decoder.decode(runner.run(prepared.input), prepared.transform).map { TileDetection(it, cut = false) }
            }
            return mergeAcrossTiles(whole + found)
        } finally {
            upright.recycle()
        }
    }

    override fun close() {
        (runner as? Closeable)?.close()
        target.recycle()
    }

    /** Whole frame as one tile: what [detect] does for a frame with a single tile. */
    internal fun preprocess(frame: DetectionFrame): PreparedYoloxInput = synchronized(lock) {
        val upright = uprightBitmap(frame)
        try {
            val prepared = letterbox(upright, Rect(0, 0, upright.width, upright.height))
            prepared.copy(input = prepared.input.copyOf())
        } finally {
            upright.recycle()
        }
    }

    private fun uprightBitmap(frame: DetectionFrame): Bitmap {
        require(frame.rotationDegrees in setOf(0, 90, 180, 270)) { "Unsupported frame rotation" }
        val source = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
        val rgba = frame.rgba
        if (rgba != null) {
            require(rgba.size >= frame.width * frame.height * 4) { "Incomplete RGBA frame" }
            source.copyPixelsFromBuffer(ByteBuffer.wrap(rgba, 0, frame.width * frame.height * 4))
        } else {
            val rgb = requireNotNull(frame.rgb) { "RGB frame is required by the model contract" }
            require(rgb.size >= frame.width * frame.height * 3) { "Incomplete RGB frame" }
            val pixels = IntArray(frame.width * frame.height) { index ->
                val offset = index * 3
                Color.rgb(rgb[offset].toInt() and 0xff, rgb[offset + 1].toInt() and 0xff, rgb[offset + 2].toInt() and 0xff)
            }
            source.setPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
        }
        if (frame.rotationDegrees == 0) return source
        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            Matrix().apply { postRotate(frame.rotationDegrees.toFloat()) },
            true,
        ).also { source.recycle() }
    }

    /** One tile of [upright], letterboxed top-left into the model input. Its input array is reused. */
    private fun letterbox(upright: Bitmap, tile: Rect): PreparedYoloxInput {
        val transform = LetterboxTransform.forSize(tile.width(), tile.height(), metadata.input.width, metadata.input.height)
        Canvas(target).apply {
            drawColor(Color.rgb(PADDING, PADDING, PADDING))
            drawBitmap(
                upright,
                tile,
                Rect(0, 0, transform.resizedWidth, transform.resizedHeight),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
        }
        target.getPixels(targetPixels, 0, metadata.input.width, 0, 0, metadata.input.width, metadata.input.height)
        var destination = 0
        targetPixels.forEach { pixel ->
            input[destination++] = (pixel and 0xff).toFloat()
            input[destination++] = (pixel shr 8 and 0xff).toFloat()
            input[destination++] = (pixel shr 16 and 0xff).toFloat()
        }
        return PreparedYoloxInput(input, transform)
    }

    private companion object { const val PADDING = 114 }
}

data class PreparedYoloxInput(val input: FloatArray, val transform: LetterboxTransform)

/** A detection moved onto the whole frame, and whether a tile seam cut it. */
internal data class TileDetection(val detection: Detection, val cut: Boolean)

/** A box found in the tile at ([x], [y]), moved back onto the whole frame. */
internal fun Detection.fromTile(tiling: FrameTiling, x: Int, y: Int): TileDetection {
    if (tiling.perSide == 1) return TileDetection(this, cut = false)
    // An edge of the tile that is not an edge of the frame is a seam: a box touching it
    // is probably only part of a clover that goes on in the next tile.
    val cut = (box.left <= SEAM && x > 0) ||
        (box.top <= SEAM && y > 0) ||
        (box.right >= 1 - SEAM && x + tiling.tileWidth < tiling.frameWidth) ||
        (box.bottom >= 1 - SEAM && y + tiling.tileHeight < tiling.frameHeight)
    fun horizontal(value: Float) = (x + value * tiling.tileWidth) / tiling.frameWidth
    fun vertical(value: Float) = (y + value * tiling.tileHeight) / tiling.frameHeight
    val moved = copy(box = NormalizedBox(horizontal(box.left), vertical(box.top), horizontal(box.right), vertical(box.bottom)))
    return TileDetection(moved, cut)
}

private const val SEAM = .01f

/**
 * Puts the tiles back together. Tiles overlap, so a clover near a seam is usually
 * found whole in one tile and cut in the next: the cut piece is dropped. A clover
 * wider than the overlap is cut in every tile: its pieces are joined into one box
 * (without this, one big clover showed up as two or four four-leaf clovers).
 */
internal fun mergeAcrossTiles(found: List<TileDetection>, iouThreshold: Float = .5f, coveredThreshold: Float = .7f): List<Detection> {
    fun Detection.duplicates(other: Detection) =
        box.intersectionOverUnion(other.box) > iouThreshold ||
            box.coveredBy(other.box) > coveredThreshold || other.box.coveredBy(box) > coveredThreshold

    val kept = mutableListOf<Detection>()
    found.filterNot { it.cut }.map { it.detection }.sortedByDescending { it.confidence }.forEach { candidate ->
        if (kept.none { candidate.duplicates(it) }) kept += candidate
    }
    val whole = kept.toList()
    val pieces = found.filter { it.cut }.map { it.detection }
        .filter { piece -> whole.none { piece.box.intersects(it.box) } }
    // Pieces that touch or overlap belong to the same clover: join each group.
    val groups = mutableListOf<MutableList<Detection>>()
    pieces.forEach { piece ->
        val touching = groups.filter { group -> group.any { it.box.intersects(piece.box) } }
        val merged = touching.flatten().toMutableList().apply { add(piece) }
        groups.removeAll(touching)
        groups += merged
    }
    groups.forEach { group ->
        val best = group.maxBy { it.confidence }
        kept += best.copy(
            box = NormalizedBox(
                group.minOf { it.box.left },
                group.minOf { it.box.top },
                group.maxOf { it.box.right },
                group.maxOf { it.box.bottom },
            ),
        )
    }
    return kept
}

private fun NormalizedBox.intersects(other: NormalizedBox) =
    minOf(right, other.right) > maxOf(left, other.left) && minOf(bottom, other.bottom) > maxOf(top, other.top)

/** Share of this box's area that lies inside [other]. */
private fun NormalizedBox.coveredBy(other: NormalizedBox): Float {
    val width = (minOf(right, other.right) - maxOf(left, other.left)).coerceAtLeast(0f)
    val height = (minOf(bottom, other.bottom) - maxOf(top, other.top)).coerceAtLeast(0f)
    return if (area > 0f) width * height / area else 0f
}

class YoloxDecoder(private val metadata: ModelMetadata) {
    private data class Candidate(val box: NormalizedBox, val score: Float, val classId: Int)

    fun decode(
        output: FloatArray,
        transform: LetterboxTransform,
        confidenceThreshold: Float = metadata.recommendedConfidence,
        iouThreshold: Float = metadata.recommendedIou,
    ): List<Detection> {
        val columns = BOX_VALUES + metadata.classes.size
        require(output.size % columns == 0) { "YOLOX output must contain rows of $columns values" }
        val candidates = ArrayList<Candidate>()
        var offset = 0
        while (offset < output.size) {
            val objectness = output[offset + 4]
            if (!objectness.isFinite()) {
                offset += columns
                continue
            }
            var bestClassScore = Float.NEGATIVE_INFINITY
            var classId = -1
            metadata.classes.indices.forEach { candidateClass ->
                val classScore = output[offset + BOX_VALUES + candidateClass]
                if (classScore.isFinite() && classScore > bestClassScore) {
                    bestClassScore = classScore
                    classId = candidateClass
                }
            }
            val score = objectness * bestClassScore
            if (classId < 0 || !score.isFinite() || score < confidenceThreshold) {
                offset += columns
                continue
            }
            val cx = output[offset]
            val cy = output[offset + 1]
            val width = output[offset + 2]
            val height = output[offset + 3]
            if (!cx.isFinite() || !cy.isFinite() || !width.isFinite() || !height.isFinite()) {
                offset += columns
                continue
            }
            val box = NormalizedBox(
                left = ((cx - width / 2f) / transform.scale / transform.sourceWidth).coerceIn(0f, 1f),
                top = ((cy - height / 2f) / transform.scale / transform.sourceHeight).coerceIn(0f, 1f),
                right = ((cx + width / 2f) / transform.scale / transform.sourceWidth).coerceIn(0f, 1f),
                bottom = ((cy + height / 2f) / transform.scale / transform.sourceHeight).coerceIn(0f, 1f),
            )
            if (box.area > 0f) candidates += Candidate(box, score, classId)
            offset += columns
        }
        candidates.sortByDescending(Candidate::score)

        val kept = mutableListOf<Candidate>()
        candidates.forEach { candidate ->
            if (kept.none { candidate.box.intersectionOverUnion(it.box) > iouThreshold }) kept += candidate
        }
        return kept.map { candidate ->
            Detection(metadata.classes[candidate.classId].toProductLabel(), candidate.score, candidate.box)
        }
    }

    private fun String.toProductLabel(): DetectionLabel = when (this) {
        "clover_4" -> DetectionLabel.FOUR_LEAF_CLOVER
        else -> DetectionLabel.CLOVER_PATCH
    }

    private companion object { const val BOX_VALUES = 5 }
}

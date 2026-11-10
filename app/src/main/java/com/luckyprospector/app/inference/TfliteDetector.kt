package com.luckyprospector.app.inference

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter

/** Small seam around LiteRT so preprocessing and decoding can be tested without shipping model weights. */
fun interface FloatModelRunner {
    fun run(input: FloatArray): FloatArray
}

class LiteRtModelRunner(model: ByteBuffer, private val maxDetections: Int = 300) : FloatModelRunner, Closeable {
    private val interpreter = Interpreter(model, Interpreter.Options().apply { setNumThreads(4) })

    init {
        require(interpreter.inputTensorCount == 1 && interpreter.outputTensorCount == 1) { "Expected one input and one output tensor" }
        require(interpreter.getInputTensor(0).dataType() == DataType.FLOAT32) { "Expected float32 model input" }
        require(interpreter.getOutputTensor(0).dataType() == DataType.FLOAT32) { "Expected float32 model output" }
        require(interpreter.getOutputTensor(0).shape().contentEquals(intArrayOf(1, maxDetections, 6))) {
            "Expected output tensor [1,$maxDetections,6]"
        }
    }

    override fun run(input: FloatArray): FloatArray {
        val inputBuffer = ByteBuffer.allocateDirect(input.size * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
        input.forEach(inputBuffer::putFloat)
        inputBuffer.rewind()
        val output = Array(1) { Array(maxDetections) { FloatArray(Yolo26Decoder.ELEMENTS_PER_DETECTION) } }
        interpreter.run(inputBuffer, output)
        return output[0].flatMap(FloatArray::asIterable).toFloatArray()
    }

    override fun close() = interpreter.close()
}

class TfliteDetector(
    private val metadata: ModelMetadata,
    private val runner: FloatModelRunner,
) : Detector {
    private val decoder = Yolo26Decoder(metadata.validate())

    override suspend fun detect(frame: DetectionFrame): List<Detection> = decoder.decode(preprocessAndRun(frame))

    internal fun preprocessAndRun(frame: DetectionFrame): FloatArray {
        require(frame.luminance.size >= frame.width * frame.height) { "Incomplete luminance plane" }
        val rgb = requireNotNull(frame.rgb) { "RGB frame is required by the model contract" }
        require(rgb.size >= frame.width * frame.height * 3) { "Incomplete RGB frame" }
        require(frame.rotationDegrees in setOf(0, 90, 180, 270)) { "Unsupported frame rotation" }
        val rotatedWidth = if (frame.rotationDegrees % 180 == 0) frame.width else frame.height
        val rotatedHeight = if (frame.rotationDegrees % 180 == 0) frame.height else frame.width
        val output = FloatArray(metadata.input.width * metadata.input.height * 3)
        var destination = 0
        for (y in 0 until metadata.input.height) {
            val rotatedY = y * rotatedHeight / metadata.input.height
            for (x in 0 until metadata.input.width) {
                val rotatedX = x * rotatedWidth / metadata.input.width
                val (sourceX, sourceY) = when (frame.rotationDegrees) {
                    0 -> rotatedX to rotatedY
                    90 -> rotatedY to (frame.height - 1 - rotatedX)
                    180 -> (frame.width - 1 - rotatedX) to (frame.height - 1 - rotatedY)
                    else -> (frame.width - 1 - rotatedY) to rotatedX
                }
                val source = (sourceY * frame.width + sourceX) * 3
                output[destination++] = (rgb[source].toInt() and 0xff) / 255f
                output[destination++] = (rgb[source + 1].toInt() and 0xff) / 255f
                output[destination++] = (rgb[source + 2].toInt() and 0xff) / 255f
            }
        }
        return runner.run(output)
    }
}

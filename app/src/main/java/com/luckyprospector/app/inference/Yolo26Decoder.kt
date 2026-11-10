package com.luckyprospector.app.inference

class Yolo26Decoder(private val metadata: ModelMetadata) {
    fun decode(output: FloatArray, confidenceThreshold: Float = metadata.recommendedConfidence): List<Detection> {
        require(output.size % ELEMENTS_PER_DETECTION == 0) { "YOLO26 output must contain rows of 6 values" }
        return output.asSequence().chunked(ELEMENTS_PER_DETECTION).mapNotNull { row ->
            val score = row[4]
            val rawClassId = row[5]
            val classId = rawClassId.toInt()
            if (!score.isFinite() || score < confidenceThreshold || !rawClassId.isFinite() || rawClassId != classId.toFloat() || classId !in metadata.classes.indices) return@mapNotNull null
            val box = NormalizedBox(
                left = (row[0] / metadata.input.width).coerceIn(0f, 1f),
                top = (row[1] / metadata.input.height).coerceIn(0f, 1f),
                right = (row[2] / metadata.input.width).coerceIn(0f, 1f),
                bottom = (row[3] / metadata.input.height).coerceIn(0f, 1f),
            )
            if (!box.left.isFinite() || !box.top.isFinite() || !box.right.isFinite() || !box.bottom.isFinite() || box.area <= 0f) return@mapNotNull null
            Detection(metadata.classes[classId].toProductLabel(), score, box)
        }.toList()
    }

    private fun String.toProductLabel(): DetectionLabel = when (this) {
        "clover_4" -> DetectionLabel.FOUR_LEAF_CLOVER
        "clover_3", "clover_5_plus" -> DetectionLabel.CLOVER_PATCH
        else -> DetectionLabel.CLOVER_PATCH
    }

    companion object { const val ELEMENTS_PER_DETECTION = 6 }
}

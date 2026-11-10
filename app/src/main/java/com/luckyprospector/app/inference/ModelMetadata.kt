package com.luckyprospector.app.inference

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class ModelInputMetadata(
    val width: Int,
    val height: Int,
    val channels: Int,
    @SerializedName("color_order") val colorOrder: String,
    val dtype: String,
    val normalization: String,
    val resize: String? = null,
)

data class ModelMetadata(
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("model_version") val modelVersion: String,
    val task: String,
    val format: String,
    val input: ModelInputMetadata,
    val classes: List<String>,
    @SerializedName("output_decoder") val outputDecoder: String,
    @SerializedName("recommended_confidence") val recommendedConfidence: Float,
    @SerializedName("recommended_iou") val recommendedIou: Float,
    @SerializedName("artifact_sha256") val artifactSha256: String,
    @SerializedName("dataset_manifest_sha256") val datasetManifestSha256: String,
    @SerializedName("license_summary") val licenseSummary: String,
    // Optional: how big a leaf must look, in pixels on the model input. Older bundles
    // do not have them; the search then falls back to a plain area rule.
    @SerializedName("guide_leaf_px_min") val guideLeafPxMin: Int? = null,
    @SerializedName("guide_leaf_px_ideal") val guideLeafPxIdeal: Int? = null,
    @SerializedName("guide_leaf_px_max") val guideLeafPxMax: Int? = null,
    @SerializedName("guide_recall_collapse_below_px") val guideRecallCollapseBelowPx: Int? = null,
) {
    /** The size guide declared by this bundle, or null if it declares none or an incomplete one. */
    val sizeGuide: SizeGuide?
        get() {
            val min = guideLeafPxMin ?: return null
            val ideal = guideLeafPxIdeal ?: return null
            val max = guideLeafPxMax ?: return null
            return runCatching { SizeGuide(min, ideal, max, input.width, input.height) }.getOrNull()
        }

    fun validate(): ModelMetadata {
        require(schemaVersion == SUPPORTED_SCHEMA_VERSION) { "Unsupported model metadata schema: $schemaVersion" }
        require(modelVersion.matches(Regex("\\d+\\.\\d+\\.\\d+(?:[-+].+)?"))) { "model_version must be semantic" }
        require(task == "object_detection") { "Unsupported task: $task" }
        require(format == "tflite") { "Unsupported model format: $format" }
        require(input.width > 0 && input.height > 0 && input.channels == 3) { "Expected a positive, three-channel input" }
        require(input.dtype == "float32") { "Only float32 model input is supported" }
        require(classes.isNotEmpty() && classes.toSet().size == classes.size) { "classes must be unique and non-empty" }
        when (outputDecoder) {
            YOLO26_DECODER -> {
                require(input.colorOrder == "RGB") { "YOLO26 adapter requires RGB input" }
                require(input.normalization == "zero_to_one") { "YOLO26 adapter requires zero_to_one normalization" }
            }
            YOLOX_DECODER -> {
                require(input.width == 640 && input.height == 640) { "YOLOX release requires a 640x640 input" }
                require(input.colorOrder == "BGR") { "YOLOX adapter requires BGR input" }
                require(input.normalization == "none_0_255") { "YOLOX adapter requires unnormalized 0..255 values" }
                require(input.resize == "letterbox_top_left_114") { "YOLOX adapter requires top-left letterbox padding" }
                require(classes == listOf("clover_3", "clover_4", "clover_5_plus")) { "Unexpected YOLOX class order" }
            }
            else -> throw IllegalArgumentException("Unsupported decoder: $outputDecoder")
        }
        require(recommendedConfidence in 0f..1f && recommendedIou in 0f..1f) { "Thresholds must be in [0,1]" }
        require(artifactSha256.matches(SHA256)) { "artifact_sha256 must contain 64 lowercase hex characters" }
        require(datasetManifestSha256.matches(SHA256)) { "dataset_manifest_sha256 must contain 64 lowercase hex characters" }
        require(licenseSummary.isNotBlank()) { "license_summary is required" }
        return this
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val YOLO26_DECODER = "yolo26_xyxy_score_class_v1"
        const val YOLOX_DECODER = "yolox_cxcywh_objectness_classes_v1"
        private val SHA256 = Regex("[0-9a-f]{64}")

        fun parse(json: String): ModelMetadata =
            requireNotNull(Gson().fromJson(json, ModelMetadata::class.java)) { "Empty metadata" }.validate()
    }
}

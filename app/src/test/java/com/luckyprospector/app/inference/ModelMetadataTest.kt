package com.luckyprospector.app.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelMetadataTest {
    @Test fun `parses and validates approved contract`() {
        val metadata = ModelMetadata.parse(validMetadataJson())
        assertEquals(640, metadata.input.width)
        assertEquals(listOf("clover_3", "clover_4", "clover_5_plus"), metadata.classes)
    }

    @Test fun `rejects incompatible decoder before inference`() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelMetadata.parse(validMetadataJson().replace(ModelMetadata.YOLO26_DECODER, "unknown"))
        }
    }

    @Test fun `rejects malformed artifact hash`() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelMetadata.parse(validMetadataJson().replace("a".repeat(64), "abc"))
        }
    }

    @Test fun `accepts frozen yolox release contract`() {
        val metadata = ModelMetadata.parse(validYoloxMetadataJson())
        assertEquals(ModelMetadata.YOLOX_DECODER, metadata.outputDecoder)
        assertEquals("letterbox_top_left_114", metadata.input.resize)
    }

    @Test fun `rejects stretch for yolox release`() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelMetadata.parse(validYoloxMetadataJson().replace("letterbox_top_left_114", "stretch"))
        }
    }
}

internal fun validMetadataJson() = """
    {
      "schema_version": 1,
      "model_version": "0.1.0",
      "task": "object_detection",
      "format": "tflite",
      "input": {"width":640,"height":640,"channels":3,"color_order":"RGB","dtype":"float32","normalization":"zero_to_one"},
      "classes": ["clover_3", "clover_4", "clover_5_plus"],
      "output_decoder": "yolo26_xyxy_score_class_v1",
      "recommended_confidence": 0.5,
      "recommended_iou": 0.45,
      "artifact_sha256": "${"a".repeat(64)}",
      "dataset_manifest_sha256": "${"b".repeat(64)}",
      "license_summary": "Approved for distribution"
    }
""".trimIndent()

internal fun validYoloxMetadataJson() = """
    {
      "schema_version": 1,
      "model_version": "0.1.0-rc1",
      "task": "object_detection",
      "format": "tflite",
      "input": {"width":640,"height":640,"channels":3,"color_order":"BGR","dtype":"float32","normalization":"none_0_255","resize":"letterbox_top_left_114"},
      "classes": ["clover_3", "clover_4", "clover_5_plus"],
      "output_decoder": "yolox_cxcywh_objectness_classes_v1",
      "recommended_confidence": 0.25,
      "recommended_iou": 0.65,
      "artifact_sha256": "${"a".repeat(64)}",
      "dataset_manifest_sha256": "${"b".repeat(64)}",
      "license_summary": "YOLOX Apache-2.0; datasets CC BY 4.0"
    }
""".trimIndent()

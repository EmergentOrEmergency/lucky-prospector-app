package com.luckyprospector.app.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SizeGuideTest {
    @Test fun `a box measures its longest side in model input pixels`() {
        // Upright 480x640 frame, one tile, letterbox scale 1: a quarter of the width is 120 px.
        val box = NormalizedBox(0f, 0f, .25f, .1f)
        assertEquals(120f, SizeGuide.RC1.leafPx(box, frame(640, 480, tiles = 1)), .01f)
    }

    @Test fun `a larger frame in one tile is scaled down to the input first`() {
        // 1280x960 rotated to 960x1280, letterbox scale 0.5: half of 960 px is 240 input px.
        val box = NormalizedBox(0f, 0f, .5f, .1f)
        assertEquals(240f, SizeGuide.RC1.leafPx(box, frame(1280, 960, tiles = 1)), .01f)
    }

    @Test fun `tiles let a smaller clover reach the same model size`() {
        // 2x2 tiles of 576x768 on 960x1280: scale 640/768, so 240 px need 288 frame px.
        val box = NormalizedBox(0f, 0f, .3f, .1f)
        assertEquals(240f, SizeGuide.RC1.leafPx(box, frame(1280, 960, tiles = 2)), .01f)
    }

    @Test fun `the guide shrinks with the tiles`() {
        assertEquals(.5f, SizeGuide.RC1.idealWidthFraction(frame(640, 480, tiles = 1)), .001f)
        assertEquals(.3f, SizeGuide.RC1.idealWidthFraction(frame(1280, 960, tiles = 2)), .001f)
        assertEquals(.2f, SizeGuide.RC1.idealWidthFraction(frame(1920, 1440, tiles = 3)), .001f)
    }

    private fun frame(width: Int, height: Int, tiles: Int) =
        DetectionFrame(ByteArray(0), width, height, rotationDegrees = 90, timestampMillis = 0, tilesPerSide = tiles)

    @Test fun `metadata without guide fields declares no guide`() {
        val metadata = ModelMetadata.parse(METADATA.replace(GUIDE_FIELDS, ""))
        assertNull(metadata.sizeGuide)
    }

    @Test fun `metadata guide fields are read`() {
        assertEquals(SizeGuide.RC1, ModelMetadata.parse(METADATA).sizeGuide)
    }

    private companion object {
        const val GUIDE_FIELDS = """, "guide_leaf_px_min": 120, "guide_leaf_px_ideal": 240, "guide_leaf_px_max": 400"""
        val METADATA = """{"schema_version": 1, "model_version": "0.1.0-rc1", "task": "object_detection", "format": "tflite",
            "input": {"width": 640, "height": 640, "channels": 3, "color_order": "BGR", "dtype": "float32",
                "normalization": "none_0_255", "resize": "letterbox_top_left_114"},
            "classes": ["clover_3", "clover_4", "clover_5_plus"],
            "output_decoder": "yolox_cxcywh_objectness_classes_v1",
            "recommended_confidence": 0.25, "recommended_iou": 0.65,
            "artifact_sha256": "${"a".repeat(64)}", "dataset_manifest_sha256": "${"b".repeat(64)}",
            "license_summary": "test"$GUIDE_FIELDS}"""
    }
}

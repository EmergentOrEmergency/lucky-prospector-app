package com.luckyprospector.app.camera

import android.util.Size
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import com.luckyprospector.app.domain.SearchResolution

internal fun SearchResolution.aspectRatioStrategy(): AspectRatioStrategy =
    if (this == SearchResolution.FULL_HD) AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
    else AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY

internal fun SearchResolution.analysisSelector(): ResolutionSelector =
    ResolutionSelector.Builder()
        .setAspectRatioStrategy(aspectRatioStrategy())
        .apply {
            if (this@analysisSelector == SearchResolution.CAMERA_MAX) {
                setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                // Aspect-ratio preference must not put a smaller 4:3 stream ahead of a larger one.
                setResolutionFilter { sizes, _ -> sizes.sortedByDescending { it.width.toLong() * it.height } }
            } else {
                setResolutionStrategy(
                    ResolutionStrategy(Size(width, height), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                )
            }
        }
        .build()

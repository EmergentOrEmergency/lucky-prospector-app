package com.luckyprospector.app.capture

import android.net.Uri
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.GeoPoint
import com.luckyprospector.app.domain.ModelPrediction

data class CaptureDraft(
    val sourcePhoto: Uri,
    val type: FindingType,
    val wish: String?,
    val location: GeoPoint?,
    val prediction: ModelPrediction?,
)

data class CaptureFormState(
    val type: FindingType = FindingType.FOUR_LEAF_CLOVER,
    val wish: String = "",
) {
    fun normalizedWish(): String? = wish.trim().takeIf(String::isNotEmpty)
    fun withWish(value: String): CaptureFormState = copy(wish = value.take(MAX_WISH_LENGTH))

    companion object { const val MAX_WISH_LENGTH = 500 }
}

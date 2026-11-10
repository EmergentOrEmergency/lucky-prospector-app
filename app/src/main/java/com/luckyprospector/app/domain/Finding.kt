package com.luckyprospector.app.domain

import java.time.Instant
import java.util.UUID

enum class FindingType { FOUR_LEAF_CLOVER, CLOVER_PATCH }

enum class SyncState { LOCAL_ONLY, PENDING, SYNCED, FAILED }

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
)

data class ModelPrediction(
    val predictedClass: String,
    val confidence: Float,
    val modelVersion: String,
    val correctedClass: String? = null,
)

data class Finding(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Instant = Instant.now(),
    val type: FindingType,
    val privateImageUri: String,
    val galleryImageUri: String?,
    val location: GeoPoint?,
    val wish: String?,
    val prediction: ModelPrediction?,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
)


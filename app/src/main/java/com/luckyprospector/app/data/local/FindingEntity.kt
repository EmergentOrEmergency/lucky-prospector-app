package com.luckyprospector.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "findings")
data class FindingEntity(
    @PrimaryKey val id: String,
    val createdAtEpochMillis: Long,
    val type: String,
    val privateImageUri: String,
    val galleryImageUri: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val wish: String?,
    val predictedClass: String?,
    val confidence: Float?,
    val modelVersion: String?,
    val correctedClass: String?,
    val syncState: String,
)

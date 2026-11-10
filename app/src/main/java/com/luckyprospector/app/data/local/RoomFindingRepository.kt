package com.luckyprospector.app.data.local

import com.luckyprospector.app.domain.Finding
import com.luckyprospector.app.domain.FindingRepository
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.domain.FindingValidator
import com.luckyprospector.app.domain.GeoPoint
import com.luckyprospector.app.domain.ModelPrediction
import com.luckyprospector.app.domain.SyncState
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFindingRepository(private val dao: FindingDao) : FindingRepository {
    override fun observeAll(): Flow<List<Finding>> = dao.observeAll().map { rows -> rows.map(FindingEntity::toDomain) }
    override suspend fun get(id: String): Finding? = dao.get(id)?.toDomain()

    override suspend fun save(finding: Finding) {
        val errors = FindingValidator.validate(finding)
        require(errors.isEmpty()) { errors.joinToString() }
        dao.upsert(finding.toEntity())
    }

    override suspend fun delete(id: String) = dao.deleteById(id)
}

internal fun Finding.toEntity() = FindingEntity(
    id, createdAt.toEpochMilli(), type.name, privateImageUri, galleryImageUri,
    location?.latitude, location?.longitude, location?.accuracyMeters, wish?.trim()?.ifEmpty { null },
    prediction?.predictedClass, prediction?.confidence, prediction?.modelVersion,
    prediction?.correctedClass, syncState.name,
)

internal fun FindingEntity.toDomain() = Finding(
    id = id,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    type = FindingType.valueOf(type),
    privateImageUri = privateImageUri,
    galleryImageUri = galleryImageUri,
    location = if (latitude != null && longitude != null) GeoPoint(latitude, longitude, accuracyMeters) else null,
    wish = wish,
    prediction = if (predictedClass != null && confidence != null && modelVersion != null) {
        ModelPrediction(predictedClass, confidence, modelVersion, correctedClass)
    } else null,
    syncState = SyncState.valueOf(syncState),
)

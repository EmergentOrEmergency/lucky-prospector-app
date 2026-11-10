package com.luckyprospector.app.domain

import kotlinx.coroutines.flow.Flow

interface FindingRepository {
    fun observeAll(): Flow<List<Finding>>
    suspend fun get(id: String): Finding?
    suspend fun save(finding: Finding)
    suspend fun delete(id: String)
}


package com.conference.asmara.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.conference.asmara.data.dto.VenueMapDto
import com.conference.asmara.data.mapper.toDomain
import com.conference.asmara.db.ScheduleDatabase
import com.conference.asmara.domain.model.VenueMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * SQLDelight-backed cache. This is the single source of truth the repository
 * serves from.
 *
 * The document is stored as the raw RPC JSON and parsed on *read*, not on
 * write: a change to the mapper or the domain model then ships without needing
 * to invalidate anyone's cache, which for a 24-hour TTL would otherwise mean a
 * day of stale-shaped data after every such change.
 */
class VenueMapLocalDataSource(private val database: ScheduleDatabase) {
    private val queries = database.venueMapQueries
    private val json = Json { ignoreUnknownKeys = true }

    fun observeVenueMap(): Flow<VenueMap?> =
        queries.selectVenueMap()
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { row ->
                row ?: return@map null
                // Same defensiveness as the mapper, one level up: a cache row
                // that will not decode at all is treated as no cache, so the
                // next refresh replaces it instead of the screen erroring
                // forever on a row the user cannot clear.
                runCatching { json.decodeFromString<VenueMapDto>(row.documentJson).toDomain() }.getOrNull()
            }

    suspend fun replace(venueSlug: String, documentJson: String, fetchedAt: Instant) =
        withContext(Dispatchers.Default) {
            queries.upsertVenueMap(venueSlug, documentJson, fetchedAt.toEpochMilliseconds())
        }

    /** Clears the cache — used when the server reports the venue no longer exists. */
    suspend fun clear() = withContext(Dispatchers.Default) {
        queries.deleteVenueMap()
    }

    suspend fun lastSyncedAt(): Instant? = withContext(Dispatchers.Default) {
        queries.selectVenueMap().executeAsOneOrNull()?.fetchedAt?.let(Instant::fromEpochMilliseconds)
    }

    /** Feature count across every level — 0 when there is nothing cached. */
    suspend fun featureCount(): Int = withContext(Dispatchers.Default) {
        val row = queries.selectVenueMap().executeAsOneOrNull() ?: return@withContext 0
        runCatching {
            json.decodeFromString<VenueMapDto>(row.documentJson).levels.sumOf { it.features.size }
        }.getOrDefault(0)
    }
}

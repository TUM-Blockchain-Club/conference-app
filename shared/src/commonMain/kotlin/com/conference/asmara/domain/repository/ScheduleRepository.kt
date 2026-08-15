package com.conference.asmara.domain.repository

import com.conference.asmara.domain.model.Event
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface ScheduleRepository {
    /** Emits from the local cache; unaffected by [refresh]'s outcome. Survives process death. */
    fun observeSchedule(): Flow<List<Event>>

    suspend fun refresh(force: Boolean = false): Result<SyncResult>

    suspend fun lastSyncedAt(): Instant?
}

/**
 * Shared by [ScheduleRepository] and [VenueMapRepository] — the offline story
 * is identical for both, and a second vocabulary for it would be two things to
 * keep in step.
 *
 * @param itemCount how much is in the cache afterwards: events for the
 *   schedule, features for the venue map. Deliberately not `eventCount`, which
 *   would read as a lie in the map's half of the codebase.
 */
data class SyncResult(val outcome: Outcome, val syncedAt: Instant?, val itemCount: Int)

enum class Outcome { REFRESHED, SKIPPED_FRESH, FAILED_SERVING_CACHE, FAILED_NO_CACHE }

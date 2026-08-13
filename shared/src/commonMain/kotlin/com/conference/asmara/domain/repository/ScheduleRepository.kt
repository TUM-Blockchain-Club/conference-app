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

data class SyncResult(val outcome: Outcome, val syncedAt: Instant?, val eventCount: Int)

enum class Outcome { REFRESHED, SKIPPED_FRESH, FAILED_SERVING_CACHE, FAILED_NO_CACHE }

package com.conference.asmara.data.repository

import com.conference.asmara.data.local.ScheduleLocalDataSource
import com.conference.asmara.data.mapper.toDomain
import com.conference.asmara.data.remote.ScheduleRemoteDataSource
import com.conference.asmara.domain.model.Event
import com.conference.asmara.domain.repository.Outcome
import com.conference.asmara.domain.repository.ScheduleRepository
import com.conference.asmara.domain.repository.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val CACHE_TTL = 15.minutes

/**
 * DB is the single source of truth: [observeSchedule] always reads from the
 * cache, and [refresh] only decides whether/how to repopulate it. A failed
 * forced refresh with warm cache still reports success (with the old
 * timestamp), so callers can distinguish stale-but-served from truly failed.
 */
class ScheduleRepositoryImpl(
    private val remote: ScheduleRemoteDataSource,
    private val local: ScheduleLocalDataSource,
    private val clock: Clock = Clock.System,
) : ScheduleRepository {

    override fun observeSchedule(): Flow<List<Event>> = local.observeSchedule()

    override suspend fun refresh(force: Boolean): Result<SyncResult> {
        val lastSynced = local.lastSyncedAt()
        val isFresh = lastSynced != null && (clock.now() - lastSynced) < CACHE_TTL
        if (!force && isFresh) {
            return Result.success(SyncResult(Outcome.SKIPPED_FRESH, lastSynced, local.eventCount().toInt()))
        }

        return try {
            val events = remote.fetchSchedule().map { it.toDomain() }
            val syncedAt = clock.now()
            local.replaceAll(events, syncedAt)
            Result.success(SyncResult(Outcome.REFRESHED, syncedAt, events.size))
        } catch (e: Exception) {
            val cachedCount = local.eventCount().toInt()
            if (cachedCount > 0) {
                Result.success(SyncResult(Outcome.FAILED_SERVING_CACHE, lastSynced, cachedCount))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun lastSyncedAt(): Instant? = local.lastSyncedAt()
}

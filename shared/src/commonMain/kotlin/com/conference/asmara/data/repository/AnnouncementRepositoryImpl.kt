package com.conference.asmara.data.repository

import com.conference.asmara.data.local.AnnouncementLocalDataSource
import com.conference.asmara.data.mapper.toDomain
import com.conference.asmara.data.remote.AnnouncementRemoteDataSource
import com.conference.asmara.domain.model.Announcement
import com.conference.asmara.domain.repository.AnnouncementRepository
import com.conference.asmara.domain.repository.Outcome
import com.conference.asmara.domain.repository.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * 5 minutes, against the schedule's 15 and the map's 24 hours.
 *
 * An emergency notice is the most time-critical data in the app. With push
 * configured, a notification forces a refresh anyway; without it, this TTL is
 * the only thing standing between an evacuation notice and an attendee.
 */
private val CACHE_TTL = 5.minutes

/** DB is the single source of truth, exactly as in [ScheduleRepositoryImpl]. */
class AnnouncementRepositoryImpl(
    private val remote: AnnouncementRemoteDataSource,
    private val local: AnnouncementLocalDataSource,
    private val clock: Clock = Clock.System,
) : AnnouncementRepository {

    override fun observeAnnouncements(): Flow<List<Announcement>> = local.observeAnnouncements()

    override suspend fun refresh(force: Boolean): Result<SyncResult> {
        val lastSynced = local.lastSyncedAt()
        val isFresh = lastSynced != null && (clock.now() - lastSynced) < CACHE_TTL
        if (!force && isFresh) {
            return Result.success(SyncResult(Outcome.SKIPPED_FRESH, lastSynced, local.announcementCount()))
        }

        return try {
            val announcements = remote.fetchAnnouncements().map { it.toDomain() }
            val syncedAt = clock.now()
            local.replaceAll(announcements, syncedAt)
            Result.success(SyncResult(Outcome.REFRESHED, syncedAt, announcements.size))
        } catch (e: Exception) {
            val cachedCount = local.announcementCount()
            if (cachedCount > 0) {
                Result.success(SyncResult(Outcome.FAILED_SERVING_CACHE, lastSynced, cachedCount))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun lastSyncedAt(): Instant? = local.lastSyncedAt()

    override suspend fun markAllRead() = local.markRead(clock.now())
}

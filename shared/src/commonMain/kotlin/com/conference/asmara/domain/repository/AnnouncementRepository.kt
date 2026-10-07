package com.conference.asmara.domain.repository

import com.conference.asmara.domain.model.Announcement
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Same contract as [ScheduleRepository], down to reusing [SyncResult] and
 * [Outcome]: the cache is the source of truth, and a push notification only
 * ever prompts a [refresh] — it never writes to the feed itself. That is what
 * makes the feed work with no push infrastructure at all.
 */
interface AnnouncementRepository {
    /** Emits from the local cache, newest first; unaffected by [refresh]'s outcome. */
    fun observeAnnouncements(): Flow<List<Announcement>>

    suspend fun refresh(force: Boolean = false): Result<SyncResult>

    suspend fun lastSyncedAt(): Instant?

    /** Marks everything currently cached as read. Local to this device — see [Announcement.isRead]. */
    suspend fun markAllRead()
}

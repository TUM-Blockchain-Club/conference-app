package com.conference.asmara.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.conference.asmara.data.mapper.toAnnouncementCategory
import com.conference.asmara.db.ScheduleDatabase
import com.conference.asmara.domain.model.Announcement
import com.conference.asmara.domain.model.AnnouncementCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant

/**
 * SQLDelight-backed cache — the single source of truth the repository serves
 * from — plus the device's notification preferences, which share its database.
 *
 * Read state ([Announcement.isRead]) is client-local by necessity: with no user
 * accounts there is no server-side identity to attach it to. It lives in the
 * `readAt` column, survives refreshes for rows that still exist, and does not
 * sync across a person's devices.
 */
class AnnouncementLocalDataSource(private val database: ScheduleDatabase) {
    private val queries = database.announcementsQueries

    fun observeAnnouncements(): Flow<List<Announcement>> = queries.selectAnnouncements()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows ->
            rows.map { row ->
                Announcement(
                    id = row.id,
                    slug = row.slug,
                    title = row.title,
                    body = row.body,
                    category = row.category.toAnnouncementCategory(),
                    publishedAt = Instant.fromEpochMilliseconds(row.publishedAt),
                    isRead = row.readAt != null,
                )
            }
        }

    /**
     * Makes the cache match [announcements] exactly — except for `readAt`,
     * which is kept for every row that survives. A plain delete-and-insert, as
     * the schedule does, would mark the whole feed unread on every refresh.
     */
    suspend fun replaceAll(announcements: List<Announcement>, syncedAt: Instant) = withContext(Dispatchers.Default) {
        database.transaction {
            if (announcements.isEmpty()) {
                queries.deleteAllAnnouncements()
            } else {
                queries.deleteAnnouncementsNotIn(announcements.map { it.id })
            }
            announcements.forEach { announcement ->
                val publishedAt = announcement.publishedAt.toEpochMilliseconds()
                val category = announcement.category.wireName
                queries.insertAnnouncementOrIgnore(
                    announcement.id,
                    announcement.slug,
                    announcement.title,
                    announcement.body,
                    category,
                    publishedAt,
                )
                queries.updateAnnouncement(
                    slug = announcement.slug,
                    title = announcement.title,
                    body = announcement.body,
                    category = category,
                    publishedAt = publishedAt,
                    id = announcement.id,
                )
            }
            queries.upsertAnnouncementLastSyncedAt(syncedAt.toEpochMilliseconds())
        }
    }

    suspend fun lastSyncedAt(): Instant? = withContext(Dispatchers.Default) {
        queries.selectAnnouncementLastSyncedAt().executeAsOneOrNull()?.lastSyncedAt?.let(Instant::fromEpochMilliseconds)
    }

    suspend fun announcementCount(): Int = withContext(Dispatchers.Default) {
        queries.countAnnouncements().executeAsOne().toInt()
    }

    /** Marks everything currently cached as read, as of [at]. Rows already read keep their original time. */
    suspend fun markRead(at: Instant) {
        withContext(Dispatchers.Default) { queries.markAllAnnouncementsRead(at.toEpochMilliseconds()) }
    }

    fun unreadCount(): Flow<Int> =
        queries.countUnreadAnnouncements().asFlow().mapToOne(Dispatchers.Default).map { it.toInt() }

    /**
     * Every category the user has *not* turned off. Opt-out: a category with no
     * stored row is enabled, and [AnnouncementCategory.EMERGENCY] is enabled
     * whatever is stored.
     */
    fun observeEnabledCategories(): Flow<Set<AnnouncementCategory>> = queries.selectNotificationPreferences()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows ->
            val disabled = rows.filter { it.enabled == 0L }.map { it.category }.toSet()
            AnnouncementCategory.entries
                .filter { !it.canOptOut || it.wireName !in disabled }
                .toSet()
        }

    suspend fun setCategoryEnabled(category: AnnouncementCategory, enabled: Boolean) {
        withContext(Dispatchers.Default) {
            queries.upsertNotificationPreference(category.wireName, if (enabled) 1L else 0L)
        }
    }
}

package com.conference.asmara.domain.repository

import com.conference.asmara.domain.model.AnnouncementCategory
import kotlinx.coroutines.flow.Flow

/**
 * Which announcement categories this device wants. Read by the feed (to
 * filter) and by the push token syncer (to tell the server what to send), so
 * one toggle changes both.
 *
 * Its own interface rather than more methods on [AnnouncementRepository]: the
 * syncer has no business refreshing the feed, and the feed's tests have no
 * business faking preferences.
 */
interface NotificationPreferencesRepository {
    /** Always contains [AnnouncementCategory.EMERGENCY]. */
    fun observeEnabledCategories(): Flow<Set<AnnouncementCategory>>

    /** A no-op for a category that cannot be opted out of. */
    suspend fun setCategoryEnabled(category: AnnouncementCategory, enabled: Boolean)
}

package com.conference.asmara.ui.announcements

import com.conference.asmara.domain.model.AnnouncementCategory

enum class AnnouncementsBanner { OFFLINE_SHOWING_CACHE }

/** One feed row, already formatted by the state holder. */
data class AnnouncementItem(
    val id: String,
    val title: String,
    val body: String,
    val category: AnnouncementCategory,
    val timeLabel: String,
    /**
     * Unread, or was unread when this visit to the feed began. The second half
     * is what keeps the "New" marker on screen after opening the tab has
     * already marked everything read — otherwise it would vanish the instant
     * it became useful.
     */
    val isNew: Boolean,
)

/** One row of the notification settings card. */
data class CategoryToggle(
    val category: AnnouncementCategory,
    val enabled: Boolean,
    /** True for a category the user cannot opt out of — emergency. */
    val locked: Boolean,
)

data class AnnouncementsUiState(
    /** Filtered to enabled categories, newest first. */
    val announcements: List<AnnouncementItem> = emptyList(),
    /** Unfiltered count; separates "nothing published" from "all muted". */
    val totalCount: Int = 0,
    /** Unread among the *visible* announcements — a muted one never badges the tab. */
    val unreadCount: Int = 0,
    val categories: List<CategoryToggle> = emptyList(),
    val initialSyncDone: Boolean = false,
    val isRefreshing: Boolean = false,
    val banner: AnnouncementsBanner? = null,
    val lastSyncedLabel: String? = null,
    val errorMessage: String? = null,
) {
    // observeAnnouncements() emits emptyList() immediately on a cold cache, so
    // loading has to be tracked separately or first launch flashes "nothing yet".
    val isLoading: Boolean get() = !initialSyncDone && totalCount == 0 && errorMessage == null

    /** Synced, and nothing has been published — distinct from still loading. */
    val isEmpty: Boolean get() = initialSyncDone && totalCount == 0 && errorMessage == null

    /** There are announcements, but every one is in a category the user turned off. */
    val isAllMuted: Boolean get() = totalCount > 0 && announcements.isEmpty()

    /** The tab badge: null rather than "0", and capped so the pill stays a pill. */
    val unreadBadge: String? get() = when {
        unreadCount <= 0 -> null
        unreadCount > MAX_BADGE -> "$MAX_BADGE+"
        else -> unreadCount.toString()
    }
}

private const val MAX_BADGE = 99

/** The category as an attendee would name it, for badges and the settings card. */
fun AnnouncementCategory.label(): String = when (this) {
    AnnouncementCategory.GENERAL -> "General"
    AnnouncementCategory.SCHEDULE -> "Schedule changes"
    AnnouncementCategory.KEYNOTE -> "Keynote reminders"
    AnnouncementCategory.EMERGENCY -> "Emergency"
}

fun AnnouncementCategory.description(): String = when (this) {
    AnnouncementCategory.GENERAL -> "Venue news, logistics and everything else."
    AnnouncementCategory.SCHEDULE -> "Room changes, delays and cancellations."
    AnnouncementCategory.KEYNOTE -> "A heads-up before each keynote starts."
    AnnouncementCategory.EMERGENCY -> "Safety notices reach everyone and can't be turned off."
}

package com.conference.asmara.domain.model

import kotlin.time.Instant

/**
 * What an announcement is about — and so whether a device hears about it.
 *
 * Mirrors the `announcements.category` check constraint; unknown values from a
 * future admin app degrade to [GENERAL] rather than throwing, as with
 * [MapCategory].
 *
 * @param wireName the value in Postgres and in `device_tokens.categories`.
 */
enum class AnnouncementCategory(val wireName: String) {
    GENERAL("general"),
    SCHEDULE("schedule"),
    KEYNOTE("keynote"),

    /**
     * The one category nobody can opt out of: the send path delivers it to
     * every registered device regardless of preferences, and the feed always
     * shows it. An evacuation notice that a toggle could silence is a bug.
     */
    EMERGENCY("emergency"),
    ;

    val canOptOut: Boolean get() = this != EMERGENCY
}

/**
 * @param isRead local to this device. There are no user accounts, so there is
 *   no server-side identity to hang "this person has read this" on: read state
 *   lives in the on-device cache's `readAt` column and does not sync across a
 *   person's devices. That is the design, not a bug.
 */
data class Announcement(
    val id: String,
    val slug: String,
    val title: String,
    val body: String,
    val category: AnnouncementCategory,
    val publishedAt: Instant,
    val isRead: Boolean,
)

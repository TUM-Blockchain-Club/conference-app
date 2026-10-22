package com.conference.asmara.data.mapper

import com.conference.asmara.data.dto.AnnouncementDto
import com.conference.asmara.domain.model.Announcement
import com.conference.asmara.domain.model.AnnouncementCategory
import kotlin.time.Instant

/** A freshly fetched announcement is unread until the cache says otherwise. */
fun AnnouncementDto.toDomain(): Announcement = Announcement(
    id = id,
    slug = slug,
    title = title,
    body = body,
    category = category.toAnnouncementCategory(),
    publishedAt = try {
        Instant.parse(publishedAt)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Announcement \"$slug\" has a malformed published_at: \"$publishedAt\"", e)
    },
    isRead = false,
)

/**
 * Unknown values (e.g. written by a future admin app) degrade to GENERAL
 * instead of throwing — never to EMERGENCY, which would make an unrecognised
 * category impossible to mute.
 */
fun String.toAnnouncementCategory(): AnnouncementCategory =
    AnnouncementCategory.entries.firstOrNull { it.wireName == this } ?: AnnouncementCategory.GENERAL

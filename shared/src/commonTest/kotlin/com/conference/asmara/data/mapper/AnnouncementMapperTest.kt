package com.conference.asmara.data.mapper

import com.conference.asmara.data.dto.AnnouncementDto
import com.conference.asmara.domain.model.AnnouncementCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Instant

class AnnouncementMapperTest {

    private fun dto(category: String = "general", publishedAt: String = "2026-09-01T06:30:00+00:00") = AnnouncementDto(
        id = "a1",
        slug = "welcome",
        title = "Welcome",
        body = "Badges at the desk.",
        category = category,
        publishedAt = publishedAt,
    )

    @Test
    fun mapsEveryKnownCategory() {
        assertEquals(AnnouncementCategory.GENERAL, dto("general").toDomain().category)
        assertEquals(AnnouncementCategory.SCHEDULE, dto("schedule").toDomain().category)
        assertEquals(AnnouncementCategory.KEYNOTE, dto("keynote").toDomain().category)
        assertEquals(AnnouncementCategory.EMERGENCY, dto("emergency").toDomain().category)
    }

    @Test
    fun anUnknownCategoryDegradesToGeneralRatherThanThrowing() {
        // And specifically not to EMERGENCY, which nobody could then mute.
        assertEquals(AnnouncementCategory.GENERAL, dto("weather").toDomain().category)
        assertEquals(AnnouncementCategory.GENERAL, dto("").toDomain().category)
    }

    @Test
    fun parsesPostgrestTimestampsWithAnOffset() {
        assertEquals(Instant.parse("2026-09-01T06:30:00Z"), dto().toDomain().publishedAt)
    }

    @Test
    fun aFreshlyFetchedAnnouncementIsUnread() {
        assertFalse(dto().toDomain().isRead)
    }

    @Test
    fun aMalformedTimestampNamesTheAnnouncement() {
        val error = assertFailsWith<IllegalArgumentException> { dto(publishedAt = "yesterday").toDomain() }
        assertEquals(true, error.message?.contains("welcome"))
    }
}

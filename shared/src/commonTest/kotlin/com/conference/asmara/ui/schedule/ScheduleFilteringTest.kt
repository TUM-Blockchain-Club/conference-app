package com.conference.asmara.ui.schedule

import com.conference.asmara.domain.model.Event
import com.conference.asmara.domain.model.EventSpeaker
import com.conference.asmara.domain.model.EventType
import com.conference.asmara.domain.model.Location
import com.conference.asmara.domain.model.Speaker
import com.conference.asmara.domain.model.SpeakerRole
import com.conference.asmara.domain.model.Track
import com.conference.asmara.ui.common.ConferenceTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val platform = Track("t1", "platform", "Platform", null, "#4F46E5", 0)
private val security = Track("t2", "security", "Security", null, "#DC2626", 1)

private val mainHall = Location("l1", "main-hall", "Main Hall", "Ground", 400)

private fun speaker(
    id: String,
    name: String,
    company: String? = null,
) = EventSpeaker(
    speaker = Speaker(id, id, name, null, company, null, null, emptyMap()),
    role = SpeakerRole.SPEAKER,
    sortOrder = 0,
)

private fun event(
    id: String,
    title: String = "Session $id",
    description: String? = null,
    start: String = "2026-09-01T09:00:00Z",
    end: String = "2026-09-01T09:45:00Z",
    track: Track? = platform,
    location: Location? = mainHall,
    isPublished: Boolean = true,
    speakers: List<EventSpeaker> = emptyList(),
) = Event(
    id = id,
    slug = id,
    title = title,
    description = description,
    startTime = Instant.parse(start),
    endTime = Instant.parse(end),
    track = track,
    location = location,
    eventType = EventType.TALK,
    isPublished = isPublished,
    speakers = speakers,
)

private val noFilters = ScheduleFilters()
private val epoch = Instant.fromEpochMilliseconds(0)

class ScheduleFilteringTest {

    @Test
    fun visibleEventsDropsUnpublished() {
        val events = listOf(event("a"), event("b", isPublished = false))

        assertEquals(listOf("a"), events.visibleEvents().map { it.id })
    }

    @Test
    fun blankQueryIsAPassthrough() {
        val events = listOf(event("a"), event("b"))

        assertEquals(2, events.applyScheduleFilters(noFilters, epoch).size)
        assertEquals(2, events.applyScheduleFilters(ScheduleFilters(query = "   "), epoch).size)
    }

    @Test
    fun queryMatchesEveryIndexedField() {
        val events = listOf(
            event("title", title = "Auditing Supply Chains"),
            event("description", description = "A deep dive into audit trails"),
            event("track", track = security),
            event("location", location = Location("l2", "annex", "Audit Room", null, null)),
            event("speaker", speakers = listOf(speaker("s1", "Nina Auditore"))),
            event("company", speakers = listOf(speaker("s2", "Sam Vo", company = "Auditly"))),
            event("nomatch", title = "Coffee Break", track = null, location = null),
        )

        assertEquals(
            listOf("title", "description", "location", "speaker", "company"),
            events.applyScheduleFilters(ScheduleFilters(query = "audit"), epoch).map { it.id },
        )
        assertEquals(
            listOf("track"),
            events.applyScheduleFilters(ScheduleFilters(query = "secur"), epoch).map { it.id },
        )
    }

    @Test
    fun queryIsCaseInsensitive() {
        val events = listOf(event("a", title = "Observability at Scale"))

        assertEquals(1, events.applyScheduleFilters(ScheduleFilters(query = "OBSERVABILITY"), epoch).size)
        assertEquals(1, events.applyScheduleFilters(ScheduleFilters(query = "observability"), epoch).size)
    }

    @Test
    fun multipleTrackChipsUnionAndAndWithTheQuery() {
        val design = Track("t3", "design", "Design", null, null, 2)
        val events = listOf(
            event("a", title = "Kotlin at scale", track = platform),
            event("b", title = "Threat modelling", track = security),
            event("c", title = "Kotlin for designers", track = design),
        )

        assertEquals(
            listOf("a", "b"),
            events.applyScheduleFilters(ScheduleFilters(trackIds = setOf("t1", "t2")), epoch).map { it.id },
        )
        assertEquals(
            listOf("a"),
            events.applyScheduleFilters(
                ScheduleFilters(query = "kotlin", trackIds = setOf("t1", "t2")),
                epoch,
            ).map { it.id },
        )
    }

    @Test
    fun emptyTrackSelectionMeansAllTracks() {
        val events = listOf(event("a", track = platform), event("b", track = security), event("c", track = null))

        assertEquals(3, events.applyScheduleFilters(noFilters, epoch).size)
    }

    @Test
    fun upcomingOnlyExcludesEventsEndingExactlyNow() {
        val now = Instant.parse("2026-09-01T10:00:00Z")
        val events = listOf(
            event("ended", start = "2026-09-01T09:00:00Z", end = "2026-09-01T09:59:00Z"),
            event("endingNow", start = "2026-09-01T09:15:00Z", end = "2026-09-01T10:00:00Z"),
            event("running", start = "2026-09-01T09:30:00Z", end = "2026-09-01T10:01:00Z"),
        )

        assertEquals(
            listOf("running"),
            events.applyScheduleFilters(ScheduleFilters(upcomingOnly = true), now).map { it.id },
        )
    }

    @Test
    fun groupIntoDaysIsDeterministicForShuffledInput() {
        val events = listOf(
            event("day2", start = "2026-09-02T09:00:00Z", end = "2026-09-02T09:45:00Z"),
            event("late", start = "2026-09-01T14:00:00Z", end = "2026-09-01T14:45:00Z"),
            event("early", start = "2026-09-01T08:00:00Z", end = "2026-09-01T08:45:00Z"),
        ).reversed()

        val days = events.groupIntoDays(ConferenceTimeZone)

        assertEquals(2, days.size)
        assertEquals(listOf(LocalDate(2026, 9, 1), LocalDate(2026, 9, 2)), days.map { it.date })
        assertEquals(listOf("10:00", "16:00"), days[0].slots.map { it.label })
        assertEquals(listOf("early", "late"), days[0].slots.flatMap { slot -> slot.events.map { it.id } })
    }

    @Test
    fun parallelSessionsShareOneSlotOrderedByTrackSortOrder() {
        val untracked = Track("t9", "untracked", "Untracked", null, null, 9)
        val events = listOf(
            event("security", start = "2026-09-01T08:00:00Z", track = security),
            event("none", start = "2026-09-01T08:00:00Z", track = null),
            event("untracked", start = "2026-09-01T08:00:00Z", track = untracked),
            event("platform", start = "2026-09-01T08:00:00Z", track = platform),
        )

        val slots = events.groupIntoDays(ConferenceTimeZone).single().slots

        assertEquals(1, slots.size)
        assertEquals(
            listOf("platform", "security", "untracked", "none"),
            slots.single().events.map { it.id },
        )
    }

    @Test
    fun sameStartTimeLandsOnDifferentDaysInDifferentZones() {
        // 23:30 UTC on 31 August is already 01:30 on 1 September in Berlin
        // (CEST, UTC+2). This is the test that documents the fixed-zone decision:
        // rendering with the device zone would move this session's day header.
        val events = listOf(event("late", start = "2026-08-31T23:30:00Z", end = "2026-09-01T00:15:00Z"))

        assertEquals(LocalDate(2026, 9, 1), events.groupIntoDays(ConferenceTimeZone).single().date)
        assertEquals(LocalDate(2026, 8, 31), events.groupIntoDays(TimeZone.UTC).single().date)
    }

    @Test
    fun dayLabelReadsAsAConferenceDay() {
        val events = listOf(event("a", start = "2026-09-01T08:00:00Z"))

        assertEquals("Tuesday, 1 September", events.groupIntoDays(ConferenceTimeZone).single().label)
    }

    @Test
    fun availableTracksAreDistinctAndOrdered() {
        val events = listOf(
            event("a", track = security),
            event("b", track = platform),
            event("c", track = security),
            event("d", track = null),
        )

        assertEquals(listOf("Platform", "Security"), events.availableTracks().map { it.name })
    }

    // Track colour parsing lives in the design system now; TrackColorTest
    // covers it, including the shorthand and uplift cases this never did.

    @Test
    fun upcomingOnlyCombinesWithTrackAndQuery() {
        val now = Instant.parse("2026-09-01T10:00:00Z")
        val events = listOf(
            event("a", title = "Kotlin", track = platform, start = "2026-09-01T11:00:00Z", end = "2026-09-01T11:45:00Z"),
            event("b", title = "Kotlin", track = security, start = "2026-09-01T11:00:00Z", end = "2026-09-01T11:45:00Z"),
            event("c", title = "Kotlin", track = platform, start = "2026-09-01T08:00:00Z", end = "2026-09-01T08:45:00Z"),
            event("d", title = "Rust", track = platform, start = "2026-09-01T11:00:00Z", end = "2026-09-01T11:45:00Z"),
        )

        assertEquals(
            listOf("a"),
            events.applyScheduleFilters(
                ScheduleFilters(query = "kotlin", trackIds = setOf("t1"), upcomingOnly = true),
                now,
            ).map { it.id },
        )
    }

    @Test
    fun slotLabelsUseTheConferenceZoneNotUtc() {
        val events = listOf(event("a", start = "2026-09-01T08:00:00Z", end = "2026-09-01T08:45:00Z"))

        assertEquals("10:00", events.groupIntoDays(ConferenceTimeZone).single().slots.single().label)
        assertEquals("08:00", events.groupIntoDays(TimeZone.UTC).single().slots.single().label)
    }

    @Test
    fun eventsSpanningMidnightGroupByStartTimeOnly() {
        // 23:00 → 01:00 Berlin: one day header, not two.
        val events = listOf(
            event("party", start = "2026-09-01T21:00:00Z", end = "2026-09-01T23:00:00Z"),
        )

        val days = events.groupIntoDays(ConferenceTimeZone)

        assertEquals(1, days.size)
        assertEquals(LocalDate(2026, 9, 1), days.single().date)
        assertEquals("23:00", days.single().slots.single().label)
    }
}

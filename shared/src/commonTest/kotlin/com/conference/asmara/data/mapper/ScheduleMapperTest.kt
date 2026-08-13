package com.conference.asmara.data.mapper

import com.conference.asmara.data.dto.EventDto
import com.conference.asmara.data.dto.EventSpeakerDto
import com.conference.asmara.data.dto.LocationDto
import com.conference.asmara.data.dto.SpeakerDto
import com.conference.asmara.data.dto.TrackDto
import com.conference.asmara.domain.model.EventType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ScheduleMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun unknownEventTypeMapsToOther() {
        val event = sampleDto(eventType = "lightning-talk").toDomain()
        assertEquals(EventType.OTHER, event.eventType)
    }

    @Test
    fun nullTrackAndLocationMapToNull() {
        val event = sampleDto(track = null, location = null).toDomain()
        assertNull(event.track)
        assertNull(event.location)
    }

    @Test
    fun speakersAreOrderedBySortOrder() {
        val event = sampleDto(
            speakers = listOf(
                sampleSpeakerDto(slug = "b", sortOrder = 2),
                sampleSpeakerDto(slug = "a", sortOrder = 1),
            ),
        ).toDomain()
        assertEquals(listOf("a", "b"), event.speakers.map { it.speaker.slug })
    }

    @Test
    fun malformedStartTimeThrows() {
        assertFailsWith<IllegalArgumentException> { sampleDto(startTime = "not-a-timestamp").toDomain() }
    }

    @Test
    fun missingRequiredTimestampFailsToDecode() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<EventDto>("""{"id":"1","slug":"x","title":"X","end_time":"2026-09-01T09:00:00Z"}""")
        }
    }

    @Test
    fun nestedPostgrestPayloadDecodes() {
        val payload = """
            [{
              "id": "e1", "slug": "opening-keynote", "title": "Opening Keynote",
              "description": null, "start_time": "2026-09-01T09:00:00Z",
              "end_time": "2026-09-01T09:45:00Z", "event_type": "keynote",
              "is_published": true,
              "track": {"id": "t1", "slug": "defi", "name": "DeFi", "sort_order": 1},
              "location": {"id": "l1", "slug": "main-stage", "name": "Main Stage"},
              "event_speakers": [
                {"role": "speaker", "sort_order": 0, "speaker": {"id": "s1", "slug": "jane-doe", "name": "Jane Doe"}}
              ]
            }]
        """.trimIndent()

        val dtos = json.decodeFromString<List<EventDto>>(payload)
        assertEquals(1, dtos.size)

        val event = dtos.first().toDomain()
        assertEquals("defi", event.track?.slug)
        assertEquals("main-stage", event.location?.slug)
        assertEquals(listOf("jane-doe"), event.speakers.map { it.speaker.slug })
    }

    private fun sampleDto(
        eventType: String = "talk",
        track: TrackDto? = TrackDto(id = "t1", slug = "defi", name = "DeFi"),
        location: LocationDto? = LocationDto(id = "l1", slug = "main-stage", name = "Main Stage"),
        startTime: String = "2026-09-01T09:00:00Z",
        speakers: List<EventSpeakerDto> = emptyList(),
    ) = EventDto(
        id = "e1",
        slug = "sample-event",
        title = "Sample",
        startTime = startTime,
        endTime = "2026-09-01T10:00:00Z",
        eventType = eventType,
        isPublished = true,
        track = track,
        location = location,
        eventSpeakers = speakers,
    )

    private fun sampleSpeakerDto(slug: String, sortOrder: Int) = EventSpeakerDto(
        role = "speaker",
        sortOrder = sortOrder,
        speaker = SpeakerDto(id = slug, slug = slug, name = slug),
    )
}

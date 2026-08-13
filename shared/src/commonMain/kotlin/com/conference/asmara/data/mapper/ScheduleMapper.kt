package com.conference.asmara.data.mapper

import com.conference.asmara.data.dto.EventDto
import com.conference.asmara.data.dto.EventSpeakerDto
import com.conference.asmara.data.dto.LocationDto
import com.conference.asmara.data.dto.SpeakerDto
import com.conference.asmara.data.dto.TrackDto
import com.conference.asmara.domain.model.Event
import com.conference.asmara.domain.model.EventSpeaker
import com.conference.asmara.domain.model.EventType
import com.conference.asmara.domain.model.Location
import com.conference.asmara.domain.model.Speaker
import com.conference.asmara.domain.model.SpeakerRole
import com.conference.asmara.domain.model.Track
import kotlin.time.Instant

fun TrackDto.toDomain(): Track = Track(
    id = id,
    slug = slug,
    name = name,
    description = description,
    color = color,
    sortOrder = sortOrder,
)

fun LocationDto.toDomain(): Location = Location(
    id = id,
    slug = slug,
    name = name,
    floor = floor,
    capacity = capacity,
)

fun SpeakerDto.toDomain(): Speaker = Speaker(
    id = id,
    slug = slug,
    name = name,
    title = title,
    company = company,
    bio = bio,
    photoUrl = photoUrl,
    links = links,
)

/** Unknown values (e.g. written by a future admin app) degrade to OTHER instead of throwing. */
fun String.toEventType(): EventType = when (this) {
    "talk" -> EventType.TALK
    "panel" -> EventType.PANEL
    "workshop" -> EventType.WORKSHOP
    "keynote" -> EventType.KEYNOTE
    "break" -> EventType.BREAK
    else -> EventType.OTHER
}

fun String.toSpeakerRole(): SpeakerRole = when (this) {
    "speaker" -> SpeakerRole.SPEAKER
    "moderator" -> SpeakerRole.MODERATOR
    "host" -> SpeakerRole.HOST
    else -> SpeakerRole.OTHER
}

fun EventSpeakerDto.toDomain(): EventSpeaker = EventSpeaker(
    speaker = speaker.toDomain(),
    role = role.toSpeakerRole(),
    sortOrder = sortOrder,
)

fun EventDto.toDomain(): Event = Event(
    id = id,
    slug = slug,
    title = title,
    description = description,
    startTime = parseTimestamp(startTime, "start_time"),
    endTime = parseTimestamp(endTime, "end_time"),
    track = track?.toDomain(),
    location = location?.toDomain(),
    eventType = eventType.toEventType(),
    isPublished = isPublished,
    speakers = eventSpeakers.sortedBy { it.sortOrder }.map { it.toDomain() },
)

private fun EventDto.parseTimestamp(value: String, field: String): Instant =
    try {
        Instant.parse(value)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Event \"$slug\" has a malformed $field: \"$value\"", e)
    }

package com.conference.asmara.domain.model

import kotlin.time.Instant

data class Track(
    val id: String,
    val slug: String,
    val name: String,
    val description: String?,
    val color: String?,
    val sortOrder: Int,
)

data class Location(
    val id: String,
    val slug: String,
    val name: String,
    val floor: String?,
    val capacity: Int?,
)

data class Speaker(
    val id: String,
    val slug: String,
    val name: String,
    val title: String?,
    val company: String?,
    val bio: String?,
    val photoUrl: String?,
    val links: Map<String, String>,
)

enum class SpeakerRole { SPEAKER, MODERATOR, HOST, OTHER }

data class EventSpeaker(
    val speaker: Speaker,
    val role: SpeakerRole,
    val sortOrder: Int,
)

enum class EventType { TALK, PANEL, WORKSHOP, KEYNOTE, BREAK, OTHER }

data class Event(
    val id: String,
    val slug: String,
    val title: String,
    val description: String?,
    val startTime: Instant,
    val endTime: Instant,
    val track: Track?,
    val location: Location?,
    val eventType: EventType,
    val isPublished: Boolean,
    val speakers: List<EventSpeaker>,
)

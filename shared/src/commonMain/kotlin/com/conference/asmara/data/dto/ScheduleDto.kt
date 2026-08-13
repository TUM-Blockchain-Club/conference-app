package com.conference.asmara.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackDto(
    val id: String,
    val slug: String,
    val name: String,
    val description: String? = null,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class LocationDto(
    val id: String,
    val slug: String,
    val name: String,
    val floor: String? = null,
    val capacity: Int? = null,
)

@Serializable
data class SpeakerDto(
    val id: String,
    val slug: String,
    val name: String,
    val title: String? = null,
    val company: String? = null,
    val bio: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val links: Map<String, String> = emptyMap(),
)

@Serializable
data class EventSpeakerDto(
    val role: String = "speaker",
    @SerialName("sort_order") val sortOrder: Int = 0,
    val speaker: SpeakerDto,
)

/** Mirrors the nested PostgREST select in [com.conference.asmara.data.remote.SupabaseScheduleRemoteDataSource]. */
@Serializable
data class EventDto(
    val id: String,
    val slug: String,
    val title: String,
    val description: String? = null,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("event_type") val eventType: String = "talk",
    @SerialName("is_published") val isPublished: Boolean = false,
    val track: TrackDto? = null,
    val location: LocationDto? = null,
    @SerialName("event_speakers") val eventSpeakers: List<EventSpeakerDto> = emptyList(),
)

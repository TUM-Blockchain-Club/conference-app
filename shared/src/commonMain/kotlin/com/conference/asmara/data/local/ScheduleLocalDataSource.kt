package com.conference.asmara.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.conference.asmara.data.mapper.toEventType
import com.conference.asmara.data.mapper.toSpeakerRole
import com.conference.asmara.db.ScheduleDatabase
import com.conference.asmara.db.SelectSchedule
import com.conference.asmara.db.SelectSpeakersForEvents
import com.conference.asmara.domain.model.Event
import com.conference.asmara.domain.model.EventSpeaker
import com.conference.asmara.domain.model.Location
import com.conference.asmara.domain.model.Speaker
import com.conference.asmara.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** SQLDelight-backed cache. This is the single source of truth the repository serves from. */
class ScheduleLocalDataSource(private val database: ScheduleDatabase) {
    private val queries = database.scheduleQueries
    private val json = Json { ignoreUnknownKeys = true }

    fun observeSchedule(): Flow<List<Event>> =
        combine(
            queries.selectSchedule().asFlow().mapToList(Dispatchers.Default),
            queries.selectSpeakersForEvents().asFlow().mapToList(Dispatchers.Default),
        ) { scheduleRows, speakerRows ->
            buildEvents(scheduleRows, speakerRows)
        }

    suspend fun replaceAll(events: List<Event>, syncedAt: Instant) = withContext(Dispatchers.Default) {
        database.transaction {
            queries.deleteAllEventSpeakers()
            queries.deleteAllEvents()
            queries.deleteAllSpeakers()
            queries.deleteAllLocations()
            queries.deleteAllTracks()

            events.mapNotNull { it.track }.distinctBy { it.id }.forEach { track ->
                queries.insertTrack(track.id, track.slug, track.name, track.description, track.color, track.sortOrder.toLong())
            }
            events.mapNotNull { it.location }.distinctBy { it.id }.forEach { location ->
                queries.insertLocation(location.id, location.slug, location.name, location.floor, location.capacity?.toLong())
            }
            events.flatMap { it.speakers.map { eventSpeaker -> eventSpeaker.speaker } }.distinctBy { it.id }.forEach { speaker ->
                queries.insertSpeaker(
                    speaker.id, speaker.slug, speaker.name, speaker.title, speaker.company,
                    speaker.bio, speaker.photoUrl, json.encodeToString(speaker.links),
                )
            }
            events.forEach { event ->
                queries.insertEvent(
                    event.id, event.slug, event.title, event.description,
                    event.startTime.toEpochMilliseconds(), event.endTime.toEpochMilliseconds(),
                    event.track?.id, event.location?.id,
                    event.eventType.name.lowercase(), if (event.isPublished) 1L else 0L,
                )
                event.speakers.forEach { eventSpeaker ->
                    queries.insertEventSpeaker(
                        event.id, eventSpeaker.speaker.id,
                        eventSpeaker.role.name.lowercase(), eventSpeaker.sortOrder.toLong(),
                    )
                }
            }
            queries.upsertLastSyncedAt(syncedAt.toEpochMilliseconds())
        }
    }

    suspend fun lastSyncedAt(): Instant? = withContext(Dispatchers.Default) {
        queries.selectLastSyncedAt().executeAsOneOrNull()?.lastSyncedAt?.let(Instant::fromEpochMilliseconds)
    }

    suspend fun eventCount(): Long = withContext(Dispatchers.Default) {
        queries.countEvents().executeAsOne()
    }

    private fun buildEvents(
        scheduleRows: List<SelectSchedule>,
        speakerRows: List<SelectSpeakersForEvents>,
    ): List<Event> {
        val speakersByEvent = speakerRows.groupBy { it.eventId }
        return scheduleRows.map { row ->
            Event(
                id = row.eventId,
                slug = row.eventSlug,
                title = row.eventTitle,
                description = row.eventDescription,
                startTime = Instant.fromEpochMilliseconds(row.startTime),
                endTime = Instant.fromEpochMilliseconds(row.endTime),
                track = row.trackId?.let {
                    Track(it, row.trackSlug!!, row.trackName!!, row.trackDescription, row.trackColor, row.trackSortOrder!!.toInt())
                },
                location = row.locationId?.let {
                    Location(it, row.locationSlug!!, row.locationName!!, row.locationFloor, row.locationCapacity?.toInt())
                },
                eventType = row.eventType.toEventType(),
                isPublished = row.isPublished != 0L,
                speakers = (speakersByEvent[row.eventId] ?: emptyList()).map { speakerRow ->
                    EventSpeaker(
                        speaker = Speaker(
                            speakerRow.id, speakerRow.slug, speakerRow.name, speakerRow.title,
                            speakerRow.company, speakerRow.bio, speakerRow.photoUrl,
                            json.decodeFromString(speakerRow.linksJson),
                        ),
                        role = speakerRow.role.toSpeakerRole(),
                        sortOrder = speakerRow.sortOrder.toInt(),
                    )
                },
            )
        }
    }
}

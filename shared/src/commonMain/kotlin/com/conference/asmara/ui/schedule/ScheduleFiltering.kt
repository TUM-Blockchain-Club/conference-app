package com.conference.asmara.ui.schedule

import com.conference.asmara.domain.model.Event
import com.conference.asmara.domain.model.Track
import com.conference.asmara.ui.common.dayLabel
import com.conference.asmara.ui.common.hhmm
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Pure functions: no Compose, no Voyager. They run once per data/filter change
// in the state holder rather than on every recomposition, and they are the part
// of this feature worth unit-testing.

fun List<Event>.visibleEvents(): List<Event> = filter { it.isPublished }

fun List<Event>.applyScheduleFilters(filters: ScheduleFilters, now: Instant): List<Event> {
    val query = filters.query.trim()
    return filter { event ->
        (filters.trackIds.isEmpty() || event.track?.id in filters.trackIds) &&
            (!filters.upcomingOnly || event.endTime > now) &&
            (query.isEmpty() || event.matchesQuery(query))
    }
}

fun Event.matchesQuery(query: String): Boolean {
    if (title.contains(query, ignoreCase = true)) return true
    if (description?.contains(query, ignoreCase = true) == true) return true
    if (track?.name?.contains(query, ignoreCase = true) == true) return true
    if (location?.name?.contains(query, ignoreCase = true) == true) return true
    return speakers.any {
        it.speaker.name.contains(query, ignoreCase = true) ||
            it.speaker.company?.contains(query, ignoreCase = true) == true
    }
}

/**
 * Day header → time sub-header → parallel sessions. Sorting is explicit rather
 * than inherited from the repository's `ORDER BY start_time`, so the result is
 * deterministic for shuffled input and testable without the database.
 */
fun List<Event>.groupIntoDays(zone: TimeZone): List<ScheduleDay> =
    groupBy { it.startTime.toLocalDateTime(zone).date }
        .entries
        // Not toSortedMap(): that is JVM-only and this is commonMain.
        .sortedBy { it.key }
        .map { (date, dayEvents) ->
            ScheduleDay(
                date = date,
                label = date.dayLabel(),
                slots = dayEvents.groupBy { it.startTime }
                    .entries
                    .sortedBy { it.key }
                    .map { (start, slotEvents) ->
                        ScheduleSlot(
                            label = start.toLocalDateTime(zone).time.hhmm(),
                            events = slotEvents.sortedWith(
                                compareBy({ it.track?.sortOrder ?: Int.MAX_VALUE }, { it.title }),
                            ),
                        )
                    },
            )
        }

fun List<Event>.availableTracks(): List<Track> =
    mapNotNull { it.track }.distinctBy { it.id }.sortedBy { it.sortOrder }

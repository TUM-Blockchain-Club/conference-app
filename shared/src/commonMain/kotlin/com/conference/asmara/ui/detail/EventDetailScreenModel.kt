package com.conference.asmara.ui.detail

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.conference.asmara.domain.repository.ScheduleRepository
import com.conference.asmara.domain.repository.VenueMapRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Re-observes the cache by id rather than taking an [com.conference.asmara.domain.model.Event]
 * through the navigation argument: Voyager `Screen`s are serialized on Android,
 * and re-reading also gives live updates on refresh plus correct restoration
 * after process death, since the SQLDelight cache outlives the process.
 *
 * Re-mapping the whole schedule is free at conference scale; a dedicated
 * `selectEventById` query would be premature.
 *
 * It also watches the venue map, for one reason: to decide whether "Show on
 * map" is worth offering. Both flows come from the same local database, so this
 * is a second cheap read, not a second network call.
 */
class EventDetailScreenModel(
    private val eventId: String,
    private val scheduleRepository: ScheduleRepository,
    private val venueMapRepository: VenueMapRepository,
) : StateScreenModel<EventDetailUiState>(EventDetailUiState.Loading) {

    init {
        screenModelScope.launch {
            combine(
                scheduleRepository.observeSchedule(),
                venueMapRepository.observeVenueMap(),
            ) { events, venueMap ->
                val match = events.firstOrNull { it.id == eventId }
                when {
                    match != null -> {
                        val locationId = match.location?.id
                        EventDetailUiState.Content(
                            event = match,
                            mappedLocationId = locationId
                                ?.takeIf { venueMap?.featureForLocation(it) != null },
                        )
                    }
                    // A cold cache emits emptyList() before the first sync
                    // lands; that is loading, not "no such session".
                    events.isEmpty() -> EventDetailUiState.Loading
                    else -> EventDetailUiState.NotFound
                }
            }
                .distinctUntilChanged()
                .collect { mutableState.value = it }
        }
    }
}

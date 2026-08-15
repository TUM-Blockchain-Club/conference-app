package com.conference.asmara.ui.detail

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.conference.asmara.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Re-observes the cache by id rather than taking an [com.conference.asmara.domain.model.Event]
 * through the navigation argument: Voyager `Screen`s are serialized on Android,
 * and re-reading also gives live updates on refresh plus correct restoration
 * after process death, since the SQLDelight cache outlives the process.
 *
 * Re-mapping the whole schedule is free at conference scale; a dedicated
 * `selectEventById` query would be premature.
 */
class EventDetailScreenModel(
    private val eventId: String,
    private val repository: ScheduleRepository,
) : StateScreenModel<EventDetailUiState>(EventDetailUiState.Loading) {

    init {
        screenModelScope.launch {
            repository.observeSchedule()
                .map { events ->
                    val match = events.firstOrNull { it.id == eventId }
                    when {
                        match != null -> EventDetailUiState.Content(match)
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

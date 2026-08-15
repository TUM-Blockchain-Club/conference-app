package com.conference.asmara.ui.schedule

import com.conference.asmara.domain.model.Event
import com.conference.asmara.domain.model.Track
import kotlinx.datetime.LocalDate

data class ScheduleFilters(
    val query: String = "",
    val trackIds: Set<String> = emptySet(),
    val upcomingOnly: Boolean = false,
)

/** Parallel-track sessions sharing one start time. */
data class ScheduleSlot(val label: String, val events: List<Event>)

data class ScheduleDay(val date: LocalDate, val label: String, val slots: List<ScheduleSlot>)

enum class ScheduleBanner { OFFLINE_SHOWING_CACHE }

data class ScheduleUiState(
    /** Already filtered and grouped by the state holder, off the main thread. */
    val days: List<ScheduleDay> = emptyList(),
    /** Chip source — derived from unfiltered data so chips never disappear. */
    val tracks: List<Track> = emptyList(),
    val filters: ScheduleFilters = ScheduleFilters(),
    /** Unfiltered count; separates "no data" from "no matches". */
    val totalEventCount: Int = 0,
    val initialSyncDone: Boolean = false,
    val isRefreshing: Boolean = false,
    val banner: ScheduleBanner? = null,
    val lastSyncedLabel: String? = null,
    val errorMessage: String? = null,
) {
    // observeSchedule() emits emptyList() immediately on a cold cache, so
    // loading has to be tracked separately or first launch flashes "no sessions".
    val isLoading: Boolean get() = !initialSyncDone && totalEventCount == 0 && errorMessage == null
    val isEmptyResult: Boolean get() = days.isEmpty() && totalEventCount > 0
    val isScheduleEmpty: Boolean get() = initialSyncDone && totalEventCount == 0 && errorMessage == null
}

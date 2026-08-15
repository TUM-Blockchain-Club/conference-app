package com.conference.asmara.ui.schedule

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.conference.asmara.domain.repository.Outcome
import com.conference.asmara.domain.repository.ScheduleRepository
import com.conference.asmara.domain.repository.SyncResult
import com.conference.asmara.ui.common.ConferenceTimeZone
import com.conference.asmara.ui.common.syncedAtLabel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Deliberately free of Voyager types beyond the base class and
 * [screenModelScope], so swapping in a `ViewModel` later is a small change.
 * A [StateScreenModel] rather than a `remember`ed object because its scope is
 * cancelled when the screen leaves the back stack and it survives Android
 * configuration changes.
 */
class ScheduleScreenModel(
    private val repository: ScheduleRepository,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = ConferenceTimeZone,
    // Injected so tests can put filtering on the test scheduler; on a real
    // device this keeps grouping off the main thread.
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StateScreenModel<ScheduleUiState>(ScheduleUiState()) {

    private val filters = MutableStateFlow(ScheduleFilters())
    private var loadJob: Job? = null

    init {
        screenModelScope.launch {
            combine(repository.observeSchedule(), filters, ::Pair)
                .flowOn(defaultDispatcher)
                .collect { (events, activeFilters) ->
                    val visible = events.visibleEvents()
                    val days = visible
                        .applyScheduleFilters(activeFilters, clock.now())
                        .groupIntoDays(zone)
                    mutableState.update {
                        it.copy(
                            days = days,
                            tracks = visible.availableTracks(),
                            filters = activeFilters,
                            totalEventCount = visible.size,
                        )
                    }
                }
        }
        load(force = false, userInitiated = false)
    }

    fun onQueryChange(query: String) {
        filters.update { it.copy(query = query) }
    }

    fun onTrackToggle(trackId: String) {
        filters.update {
            val next = if (trackId in it.trackIds) it.trackIds - trackId else it.trackIds + trackId
            it.copy(trackIds = next)
        }
    }

    fun onUpcomingToggle() {
        filters.update { it.copy(upcomingOnly = !it.upcomingOnly) }
    }

    fun onClearFilters() {
        filters.value = ScheduleFilters()
    }

    fun onDismissBanner() {
        mutableState.update { it.copy(banner = null) }
    }

    fun onPullToRefresh() = load(force = true, userInitiated = true)

    fun onRetry() = load(force = true, userInitiated = false)

    private fun load(force: Boolean, userInitiated: Boolean) {
        if (loadJob?.isActive == true) return
        // isRefreshing only for user-initiated pulls: the first load should show
        // the loading state, not a pull-to-refresh spinner nobody pulled.
        if (userInitiated) mutableState.update { it.copy(isRefreshing = true) }
        loadJob = screenModelScope.launch {
            repository.refresh(force)
                .onSuccess { applySyncResult(it) }
                .onFailure { applySyncFailure() }
            mutableState.update { it.copy(isRefreshing = false, initialSyncDone = true) }
        }
    }

    private suspend fun applySyncResult(result: SyncResult) {
        when (result.outcome) {
            Outcome.REFRESHED, Outcome.SKIPPED_FRESH -> mutableState.update {
                it.copy(
                    banner = null,
                    errorMessage = null,
                    lastSyncedLabel = result.syncedAt?.syncedAtLabel(zone),
                )
            }
            Outcome.FAILED_SERVING_CACHE -> mutableState.update {
                // syncedAt is the *old* timestamp here — exactly what the banner needs.
                it.copy(
                    banner = ScheduleBanner.OFFLINE_SHOWING_CACHE,
                    errorMessage = null,
                    lastSyncedLabel = result.syncedAt?.syncedAtLabel(zone),
                )
            }
            // ScheduleRepositoryImpl never produces this; mapping it to the same
            // error state keeps the `when` exhaustive without an `else`.
            Outcome.FAILED_NO_CACHE -> applySyncFailure()
        }
    }

    // Reachable only with an empty cache — a failure with anything cached comes
    // back as success(FAILED_SERVING_CACHE). The exception message is a network
    // stack trace, so the user gets a fixed sentence instead.
    private suspend fun applySyncFailure() {
        val lastSynced = runCatching { repository.lastSyncedAt() }.getOrNull()
        mutableState.update {
            it.copy(
                banner = null,
                errorMessage = "Couldn't load the schedule. Check your connection and try again.",
                lastSyncedLabel = lastSynced?.syncedAtLabel(zone),
            )
        }
    }
}

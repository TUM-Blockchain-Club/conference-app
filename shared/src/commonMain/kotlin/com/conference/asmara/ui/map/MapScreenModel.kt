package com.conference.asmara.ui.map

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.conference.asmara.domain.model.Event
import com.conference.asmara.domain.model.MapFeature
import com.conference.asmara.domain.model.VenueLevel
import com.conference.asmara.domain.model.VenueMap
import com.conference.asmara.domain.repository.Outcome
import com.conference.asmara.domain.repository.ScheduleRepository
import com.conference.asmara.domain.repository.SyncResult
import com.conference.asmara.domain.repository.VenueMapRepository
import com.conference.asmara.ui.common.ConferenceTimeZone
import com.conference.asmara.ui.common.syncedAtLabel
import com.conference.asmara.ui.common.timeRangeLabel
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
import kotlin.time.Instant

/**
 * Combines the venue map with the schedule, because a room on its own is not
 * useful — "Workshop Room A" answers a question nobody asked; "Workshop Room A,
 * Building Trustless Bridges, on now" is the reason to look at the map at all.
 *
 * Free of Voyager types beyond the base class and [screenModelScope], for the
 * same reason as `ScheduleScreenModel`.
 */
class MapScreenModel(
    private val venueMapRepository: VenueMapRepository,
    private val scheduleRepository: ScheduleRepository,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = ConferenceTimeZone,
    // Injected so tests can put derivation on the test scheduler; on a real
    // device this keeps the search scan and the now/next lookup off the main
    // thread.
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StateScreenModel<MapUiState>(MapUiState()) {

    /**
     * Everything the user has done, kept separate from everything the data
     * layer has said. Keyed by *slug and id* rather than by index, so a refresh
     * that inserts a floor does not silently move the selection to a different
     * one.
     */
    private data class Interaction(
        val query: String = "",
        val levelSlug: String? = null,
        val selectedFeatureId: String? = null,
        /**
         * A "show on map" request that arrived before there was a map to apply
         * it to. Held here rather than dropped, because that is precisely the
         * cold-start case the cross-link has to survive.
         */
        val pendingFocusLocationId: String? = null,
    )

    private val interaction = MutableStateFlow(Interaction())
    private var loadJob: Job? = null

    init {
        screenModelScope.launch {
            combine(
                venueMapRepository.observeVenueMap(),
                scheduleRepository.observeSchedule(),
                interaction,
                ::Triple,
            )
                .flowOn(defaultDispatcher)
                .collect { (venueMap, events, current) -> render(venueMap, events, current) }
        }
        load(force = false)
    }

    fun onQueryChange(query: String) {
        interaction.update { it.copy(query = query) }
    }

    fun onClearQuery() {
        interaction.update { it.copy(query = "") }
    }

    fun onLevelSelected(index: Int) {
        val slug = state.value.levels.getOrNull(index)?.slug ?: return
        // Selection is not cleared: switching floors to look at a room and then
        // coming back should find it still highlighted.
        interaction.update { it.copy(levelSlug = slug) }
    }

    /** null clears the selection — that is what tapping bare floor means. */
    fun onFeatureSelected(featureId: String?) {
        interaction.update { it.copy(selectedFeatureId = featureId) }
    }

    /** A search hit: jump to its floor, highlight it, and drop the query. */
    fun onSearchResultSelected(featureId: String) {
        val levelSlug = state.value.levels.firstOrNull { level ->
            level.features.any { it.id == featureId }
        }?.slug
        interaction.update {
            it.copy(query = "", selectedFeatureId = featureId, levelSlug = levelSlug ?: it.levelSlug)
        }
    }

    /** The "Show on map" cross-link. Applied as soon as there is a map. */
    fun onFocusLocation(locationId: String) {
        interaction.update { it.copy(query = "", pendingFocusLocationId = locationId) }
    }

    fun onRetry() = load(force = true)

    private fun load(force: Boolean) {
        if (loadJob?.isActive == true) return
        loadJob = screenModelScope.launch {
            venueMapRepository.refresh(force)
                .onSuccess { applySyncResult(it) }
                .onFailure { applySyncFailure() }
            mutableState.update { it.copy(initialSyncDone = true) }
        }
    }

    private fun render(venueMap: VenueMap?, events: List<Event>, current: Interaction) {
        // A pending focus is resolved by *rewriting the interaction*, not by
        // producing state directly: that keeps one path into the UI state and
        // makes the focus indistinguishable from a tap once it has landed. The
        // update re-triggers this collector with the focus cleared.
        val pendingLocationId = current.pendingFocusLocationId
        if (pendingLocationId != null) {
            if (venueMap == null) return // Still cold. Try again when the map arrives.
            val feature = venueMap.featureForLocation(pendingLocationId)
            val level = feature?.let { venueMap.levelOfFeature(it.id) }
            interaction.update {
                it.copy(
                    pendingFocusLocationId = null,
                    // A location with no feature on the map leaves the map
                    // where it was rather than deselecting: the button that
                    // sent us here should not have been offered, and blanking
                    // the screen would be a worse way to say so.
                    selectedFeatureId = feature?.id ?: it.selectedFeatureId,
                    levelSlug = level?.slug ?: it.levelSlug,
                )
            }
            return
        }

        val levels = venueMap?.levels.orEmpty()
        val levelIndex = levels.indexOfFirst { it.slug == current.levelSlug }.takeIf { it >= 0 } ?: 0
        val selectedLevel = levels.getOrNull(levelIndex)

        val selected = current.selectedFeatureId?.let { id ->
            levels.firstNotNullOfOrNull { level ->
                level.features.firstOrNull { it.id == id }?.let { level to it }
            }
        }

        mutableState.update { previous ->
            previous.copy(
                venueName = venueMap?.name,
                levels = levels,
                selectedLevelIndex = levelIndex,
                query = current.query,
                searchResults = levels.search(current.query, selectedLevel?.slug),
                // Drops out on its own if the feature is gone after a refresh.
                selection = selected?.let { (level, feature) ->
                    feature.toSelection(level, events, clock.now())
                },
            )
        }
    }

    private fun MapFeature.toSelection(
        level: VenueLevel,
        events: List<Event>,
        now: Instant,
    ): MapSelection {
        val here = locationId
            ?.let { id -> events.filter { it.isPublished && it.location?.id == id } }
            ?.sortedBy { it.startTime }
            .orEmpty()
        return MapSelection(
            featureId = id,
            name = name,
            category = category,
            levelName = level.name,
            hostsSessions = locationId != null,
            // Half-open: a session is "now" up to but not including its end
            // time, so a talk that has just ended does not compete with the one
            // that has just started.
            nowSession = here.firstOrNull { it.startTime <= now && now < it.endTime }?.toMapSession(),
            nextSession = here.firstOrNull { it.startTime > now }?.toMapSession(),
        )
    }

    private fun Event.toMapSession() = MapSession(
        eventId = id,
        title = title,
        timeLabel = timeRangeLabel(startTime, endTime, zone),
    )

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
                    banner = MapBanner.OFFLINE_SHOWING_CACHE,
                    errorMessage = null,
                    lastSyncedLabel = result.syncedAt?.syncedAtLabel(zone),
                )
            }
            // VenueMapRepositoryImpl never produces this; mapping it to the
            // same error state keeps the `when` exhaustive without an `else`.
            Outcome.FAILED_NO_CACHE -> applySyncFailure()
        }
    }

    // Reachable only with an empty cache — a failure with anything cached comes
    // back as success(FAILED_SERVING_CACHE). The exception message is a network
    // stack trace, so the user gets a fixed sentence instead.
    private suspend fun applySyncFailure() {
        val lastSynced = runCatching { venueMapRepository.lastSyncedAt() }.getOrNull()
        mutableState.update {
            it.copy(
                banner = null,
                errorMessage = "Check your connection and try again.",
                lastSyncedLabel = lastSynced?.syncedAtLabel(zone),
            )
        }
    }
}

/**
 * Name search across every floor, not just the one on screen — the whole point
 * of searching a venue map is to find the room you cannot see.
 *
 * Results on another floor are flagged rather than filtered out or sorted
 * below: "Restrooms, 1st Floor" is a useful answer when you are standing on the
 * ground floor, and hiding it would make the search look broken.
 */
internal fun List<VenueLevel>.search(query: String, currentLevelSlug: String?): List<MapSearchResult> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    return flatMap { level ->
        level.features
            .filter { it.name.contains(trimmed, ignoreCase = true) }
            .map { feature ->
                MapSearchResult(
                    featureId = feature.id,
                    name = feature.name,
                    levelName = level.name,
                    onAnotherLevel = level.slug != currentLevelSlug,
                )
            }
    }
}

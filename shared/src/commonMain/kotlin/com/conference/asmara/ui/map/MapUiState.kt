package com.conference.asmara.ui.map

import com.conference.asmara.domain.model.MapCategory
import com.conference.asmara.domain.model.VenueLevel

enum class MapBanner { OFFLINE_SHOWING_CACHE }

/** A session in the selected room, flattened for the sheet. */
data class MapSession(
    val eventId: String,
    val title: String,
    val timeLabel: String,
)

/**
 * What the sheet shows about the highlighted feature.
 *
 * Resolved by the state holder rather than handed the raw [MapFeature]: the
 * now/next lookup needs the schedule and the clock, neither of which belongs in
 * a composable.
 */
data class MapSelection(
    val featureId: String,
    val name: String,
    val category: MapCategory,
    val levelName: String,
    /** True when the feature is a room that hosts sessions — i.e. has a `location_id`. */
    val hostsSessions: Boolean,
    val nowSession: MapSession? = null,
    val nextSession: MapSession? = null,
)

data class MapSearchResult(
    val featureId: String,
    val name: String,
    val levelName: String,
    /** True when the result is on a floor other than the one on screen. */
    val onAnotherLevel: Boolean,
)

data class MapUiState(
    val venueName: String? = null,
    val levels: List<VenueLevel> = emptyList(),
    val selectedLevelIndex: Int = 0,
    val query: String = "",
    val searchResults: List<MapSearchResult> = emptyList(),
    val selection: MapSelection? = null,
    val initialSyncDone: Boolean = false,
    val banner: MapBanner? = null,
    val lastSyncedLabel: String? = null,
    val errorMessage: String? = null,
) {
    val selectedLevel: VenueLevel? get() = levels.getOrNull(selectedLevelIndex)

    val hasMultipleLevels: Boolean get() = levels.size > 1

    // observeVenueMap() emits null immediately on a cold cache, so loading has
    // to be tracked separately or first launch flashes "no map yet".
    val isLoading: Boolean get() = !initialSyncDone && levels.isEmpty() && errorMessage == null

    /** Synced, and the venue genuinely has no map — distinct from still loading. */
    val isMapEmpty: Boolean get() = initialSyncDone && levels.isEmpty() && errorMessage == null

    val isSearching: Boolean get() = query.isNotBlank()
}

/**
 * The category as a person would say it, for the sheet's badge.
 *
 * Plural where the thing usually is ("Restrooms", "Booths"): a badge reading
 * "Restroom" over an area holding six of them is a small lie the eye notices.
 */
fun MapCategory.label(): String = when (this) {
    MapCategory.STAGE -> "Stage"
    MapCategory.ROOM -> "Room"
    MapCategory.FOOD -> "Food & drink"
    MapCategory.RESTROOM -> "Restrooms"
    MapCategory.BOOTH -> "Booths"
    MapCategory.ENTRANCE -> "Entrance"
    MapCategory.STAIRS -> "Stairs"
    MapCategory.ELEVATOR -> "Lift"
    MapCategory.CORRIDOR -> "Corridor"
    MapCategory.OTHER -> "Place"
}

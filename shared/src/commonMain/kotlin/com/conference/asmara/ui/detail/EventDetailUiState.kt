package com.conference.asmara.ui.detail

import com.conference.asmara.domain.model.Event

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState

    /**
     * @param mappedLocationId the `locations.id` to hand to the Map tab, set
     *   only when the venue map actually has a feature standing for this
     *   session's room. Resolved here rather than in the composable so that
     *   "Show on map" is never offered for a room the map cannot show — a
     *   button that navigates somewhere and then does nothing is worse than no
     *   button.
     */
    data class Content(
        val event: Event,
        val mappedLocationId: String? = null,
    ) : EventDetailUiState

    /** The id no longer resolves — e.g. the session was pulled from the schedule. */
    data object NotFound : EventDetailUiState
}

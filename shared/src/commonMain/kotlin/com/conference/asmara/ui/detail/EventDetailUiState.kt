package com.conference.asmara.ui.detail

import com.conference.asmara.domain.model.Event

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data class Content(val event: Event) : EventDetailUiState

    /** The id no longer resolves — e.g. the session was pulled from the schedule. */
    data object NotFound : EventDetailUiState
}

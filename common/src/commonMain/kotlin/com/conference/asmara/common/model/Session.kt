package com.conference.asmara.common.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val title: String,
    val description: String,
    val speakerIds: List<String>,
    val startTime: Instant,
    val endTime: Instant,
    val room: String,
)

@Serializable
data class Speaker(
    val id: String,
    val name: String,
    val bio: String,
    val photoUrl: String? = null,
)

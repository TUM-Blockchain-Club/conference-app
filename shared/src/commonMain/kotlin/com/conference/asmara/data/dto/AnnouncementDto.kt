package com.conference.asmara.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the PostgREST select in [com.conference.asmara.data.remote.SupabaseAnnouncementRemoteDataSource]. */
@Serializable
data class AnnouncementDto(
    val id: String,
    val slug: String,
    val title: String,
    val body: String,
    val category: String = "general",
    @SerialName("published_at") val publishedAt: String,
)

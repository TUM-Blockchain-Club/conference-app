package com.conference.asmara.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Mirrors the document assembled by the `get_venue_map()` RPC.
 *
 * Geometry stays as a raw [JsonElement] rather than a typed GeoJSON tree. It is
 * hand-authored data whose shape is a nested coordinate array — three levels
 * deep for a polygon with holes — and `@Serializable` classes over that would
 * throw on the first malformed ring. Decoding it defensively in
 * [com.conference.asmara.data.mapper] instead means a single bad room is
 * dropped rather than the whole map failing to parse.
 */
@Serializable
data class VenueMapDto(
    val id: String,
    val slug: String,
    val name: String,
    val levels: List<VenueLevelDto> = emptyList(),
)

@Serializable
data class VenueLevelDto(
    val id: String,
    val slug: String,
    val name: String,
    val ordinal: Int = 0,
    /** GeoJSON Polygon, or null for a level whose footprint has not been traced. */
    val outline: JsonElement? = null,
    val features: List<MapFeatureDto> = emptyList(),
)

@Serializable
data class MapFeatureDto(
    val id: String,
    val slug: String,
    val name: String,
    val category: String = "other",
    @SerialName("location_id") val locationId: String? = null,
    /** GeoJSON Polygon or Point. */
    val geometry: JsonElement? = null,
    @SerialName("label_anchor") val labelAnchor: JsonElement? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

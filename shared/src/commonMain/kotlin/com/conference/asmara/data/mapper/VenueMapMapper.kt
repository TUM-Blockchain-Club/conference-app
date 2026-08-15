package com.conference.asmara.data.mapper

import com.conference.asmara.data.dto.MapFeatureDto
import com.conference.asmara.data.dto.VenueLevelDto
import com.conference.asmara.data.dto.VenueMapDto
import com.conference.asmara.domain.model.FeatureShape
import com.conference.asmara.domain.model.MapCategory
import com.conference.asmara.domain.model.MapFeature
import com.conference.asmara.domain.model.Point
import com.conference.asmara.domain.model.Polygon
import com.conference.asmara.domain.model.VenueLevel
import com.conference.asmara.domain.model.VenueMap
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull

/**
 * GeoJSON → geometry, defensively.
 *
 * Same posture as `parseHexColor` in `TrackColor.kt`, and for the same reason:
 * this is hand-authored data. `venue.json` is traced in QGIS and then edited by
 * hand, and the seed script's validation only covers what goes through it —
 * anything written straight into the table bypasses it entirely.
 *
 * So nothing here throws. A feature whose geometry will not parse is **dropped**
 * and the rest of the floor still renders; a level whose outline will not parse
 * keeps its rooms and loses only its footprint. Failing the whole map because
 * one restroom has a three-deep coordinate array would be the worse trade: the
 * attendee standing in the building loses the map entirely over a room they were
 * not looking for.
 */

fun VenueMapDto.toDomain(): VenueMap = VenueMap(
    id = id,
    slug = slug,
    name = name,
    // Ordered here rather than trusted from the RPC, so a hand-run query or a
    // future caching layer cannot reorder the floor tabs.
    levels = levels.map { it.toDomain() }.sortedWith(compareBy({ it.ordinal }, { it.slug })),
)

fun VenueLevelDto.toDomain(): VenueLevel = VenueLevel(
    id = id,
    slug = slug,
    name = name,
    ordinal = ordinal,
    outline = outline?.let(::parseGeoJsonPolygon),
    features = features
        .mapNotNull { it.toDomainOrNull() }
        .sortedWith(compareBy({ it.sortOrder }, { it.name })),
)

/** Null when the geometry is missing or malformed — the caller drops the feature. */
fun MapFeatureDto.toDomainOrNull(): MapFeature? {
    val shape = geometry?.let(::parseGeoJsonShape) ?: return null
    return MapFeature(
        id = id,
        slug = slug,
        name = name,
        category = category.toMapCategory(),
        locationId = locationId,
        shape = shape,
        labelAnchor = labelAnchor?.let(::parseGeoJsonPoint),
        sortOrder = sortOrder,
    )
}

/** Unknown values (e.g. written by a future admin app) degrade to OTHER instead of throwing. */
fun String.toMapCategory(): MapCategory = when (this) {
    "stage" -> MapCategory.STAGE
    "room" -> MapCategory.ROOM
    "food" -> MapCategory.FOOD
    "restroom" -> MapCategory.RESTROOM
    "booth" -> MapCategory.BOOTH
    "entrance" -> MapCategory.ENTRANCE
    "stairs" -> MapCategory.STAIRS
    "elevator" -> MapCategory.ELEVATOR
    "corridor" -> MapCategory.CORRIDOR
    else -> MapCategory.OTHER
}

/** Dispatches on the GeoJSON `type`, so one column can hold rooms and pins. */
fun parseGeoJsonShape(element: JsonElement): FeatureShape? =
    when ((element as? JsonObject)?.get("type")?.asStringOrNull()) {
        "Polygon" -> parseGeoJsonPolygon(element)?.let(FeatureShape::Area)
        "Point" -> parseGeoJsonPoint(element)?.let(FeatureShape::Marker)
        else -> null
    }

fun parseGeoJsonPoint(element: JsonElement): Point? {
    val obj = element as? JsonObject ?: return null
    if (obj["type"]?.asStringOrNull() != "Point") return null
    return parsePosition(obj["coordinates"])
}

fun parseGeoJsonPolygon(element: JsonElement): Polygon? {
    val obj = element as? JsonObject ?: return null
    if (obj["type"]?.asStringOrNull() != "Polygon") return null
    val rings = (obj["coordinates"] as? JsonArray)
        ?.map { parseRing(it) }
        ?: return null
    // The exterior ring is the polygon. A hole that will not parse is dropped,
    // because a room drawn without its lift core is wrong in a way you can see
    // and work around; a room that vanishes is not.
    val exterior = rings.firstOrNull() ?: return null
    return Polygon(ring = exterior, holes = rings.drop(1).filterNotNull())
}

/** A GeoJSON linear ring: >= 3 positions once the repeated closing vertex is dropped. */
private fun parseRing(element: JsonElement): List<Point>? {
    val positions = (element as? JsonArray)?.map { parsePosition(it) ?: return null } ?: return null
    // GeoJSON requires the ring to close; the domain model does not store that
    // duplicate. Tolerated either way, so an unclosed ring is not an error.
    val open = if (positions.size > 1 && positions.first() == positions.last()) {
        positions.dropLast(1)
    } else {
        positions
    }
    return open.takeIf { it.size >= 3 }
}

/** `[x, y]`. Extra ordinates (a GeoJSON altitude) are ignored, not rejected. */
private fun parsePosition(element: JsonElement?): Point? {
    val array = element as? JsonArray ?: return null
    if (array.size < 2) return null
    val x = array[0].asFloatOrNull() ?: return null
    val y = array[1].asFloatOrNull() ?: return null
    return Point(x, y)
}

private fun JsonElement.asStringOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

/**
 * Numbers only. A quoted `"12.5"` is rejected rather than coerced: it means the
 * export wrote coordinates as text, which is a whole-layer problem worth
 * noticing, not a value worth silently repairing.
 */
private fun JsonElement.asFloatOrNull(): Float? =
    (this as? JsonPrimitive)?.takeIf { !it.isString }?.floatOrNull

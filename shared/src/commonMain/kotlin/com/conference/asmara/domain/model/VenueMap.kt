package com.conference.asmara.domain.model

/**
 * The venue's geometry, in **venue-local metres**: origin at the venue's
 * north-west corner, X increasing right, Y increasing *down*.
 *
 * Not longitude/latitude. Euclidean distance here is real distance, the render
 * transform is plain arithmetic, and Y-down already matches the screen. See
 * `docs/VENUE-MAP.md` for the authoring side of the same decision.
 *
 * `Float` rather than `Double` throughout: these values go straight into a
 * Compose `Canvas`, which is Float end to end, and a venue is tens of metres
 * across — Float carries roughly seven significant digits, so the precision
 * floor is well under a micrometre. Doubles would buy nothing but a conversion
 * on every draw.
 */
data class Point(val x: Float, val y: Float)

/**
 * A closed area.
 *
 * @param ring the exterior boundary, implicitly closed: the mapper drops
 *   GeoJSON's repeated final vertex, so the last point does not duplicate the
 *   first. Consumers tolerate a repeat anyway — it is a zero-length edge to
 *   both the renderer and [com.conference.asmara.ui.map.contains] — but every
 *   ring that comes through the data layer is in the stripped form.
 * @param holes interior boundaries: a courtyard, a lift core, an atrium.
 */
data class Polygon(
    val ring: List<Point>,
    val holes: List<List<Point>> = emptyList(),
)

/**
 * What a feature *is*, which is the only thing that decides how it is drawn and
 * coloured. Mirrors the `map_features.category` check constraint; unknown
 * values from a future admin app degrade to [OTHER] rather than throwing.
 */
enum class MapCategory {
    STAGE, ROOM, FOOD, RESTROOM, BOOTH, ENTRANCE, STAIRS, ELEVATOR, CORRIDOR, OTHER,
}

/**
 * A feature is either an area or a pin, never both, and the difference changes
 * everything about it — an area can be tapped inside, hit-tested and labelled
 * in place; a pin has a fixed on-screen size and is tapped within a radius.
 * A sealed interface rather than two nullable fields so the renderer cannot
 * forget one case.
 */
sealed interface FeatureShape {
    data class Area(val polygon: Polygon) : FeatureShape
    data class Marker(val point: Point) : FeatureShape
}

/**
 * One room, stall or point of interest.
 *
 * @param locationId the `locations` row this feature *is*, when it hosts
 *   sessions. This is the join that makes the map schedule-aware: it is how the
 *   map knows what is on in Main Stage right now, and what "Show on map" on a
 *   session resolves to. Null for a restroom or a staircase.
 * @param labelAnchor where to draw the name. Null means "use the centroid",
 *   which is right until a room is L-shaped and its centroid falls outside it.
 */
data class MapFeature(
    val id: String,
    val slug: String,
    val name: String,
    val category: MapCategory,
    val locationId: String?,
    val shape: FeatureShape,
    val labelAnchor: Point?,
    val sortOrder: Int,
)

/**
 * @param ordinal 0 is ground, 1 the first floor, -1 a basement. Display order
 *   for the floor tabs, and independent of the slug so floors can be inserted.
 */
data class VenueLevel(
    val id: String,
    val slug: String,
    val name: String,
    val ordinal: Int,
    val outline: Polygon?,
    val features: List<MapFeature>,
)

data class VenueMap(
    val id: String,
    val slug: String,
    val name: String,
    val levels: List<VenueLevel>,
) {
    /** The level a feature sits on, or null if the id is not in this map. */
    fun levelOfFeature(featureId: String): VenueLevel? =
        levels.firstOrNull { level -> level.features.any { it.id == featureId } }

    /** The feature standing for a schedule location, if the map has one. */
    fun featureForLocation(locationId: String): MapFeature? =
        levels.firstNotNullOfOrNull { level ->
            level.features.firstOrNull { it.locationId == locationId }
        }
}

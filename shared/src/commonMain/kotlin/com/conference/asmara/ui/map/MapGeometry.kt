package com.conference.asmara.ui.map

import com.conference.asmara.domain.model.FeatureShape
import com.conference.asmara.domain.model.MapFeature
import com.conference.asmara.domain.model.Point
import com.conference.asmara.domain.model.Polygon
import com.conference.asmara.domain.model.VenueLevel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The maths behind the map canvas. Pure Kotlin — no Compose types, no
 * composition locals — so it is testable the way `ScheduleFiltering` is.
 *
 * That is most of the argument for rendering with a `Canvas` instead of pulling
 * in a map library: the hard part of drawing a floor plan is a fit transform
 * and a point-in-polygon test, and both are twenty lines that can be pinned
 * down by unit tests rather than trusted.
 *
 * Two coordinate spaces throughout:
 *
 * - **venue** — metres, origin north-west, Y down. What the data layer holds.
 * - **canvas** — pixels, origin top-left, Y down. What the renderer draws in.
 *
 * [MapTransform] converts between them, and is the only thing that knows both.
 */

/** An axis-aligned box in venue metres. */
data class MapBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
}

/**
 * Venue metres → canvas pixels: uniform scale, then translate. Uniform because
 * a floor plan stretched on one axis is no longer a floor plan.
 */
data class MapTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
) {
    fun toCanvasX(x: Float): Float = x * scale + offsetX
    fun toCanvasY(y: Float): Float = y * scale + offsetY

    fun toVenueX(canvasX: Float): Float = (canvasX - offsetX) / scale
    fun toVenueY(canvasY: Float): Float = (canvasY - offsetY) / scale

    /** Inverse of [toCanvasX]/[toCanvasY] — a tap position back in metres. */
    fun toVenue(canvasX: Float, canvasY: Float): Point = Point(toVenueX(canvasX), toVenueY(canvasY))
}

/** The box containing every point of every polygon, or null if there are none. */
fun bounds(polygons: List<Polygon>, points: List<Point> = emptyList()): MapBounds? {
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    var any = false

    fun include(point: Point) {
        any = true
        minX = min(minX, point.x)
        minY = min(minY, point.y)
        maxX = max(maxX, point.x)
        maxY = max(maxY, point.y)
    }

    // Holes are inside the exterior ring by definition, so they cannot widen
    // the box and are skipped.
    polygons.forEach { polygon -> polygon.ring.forEach(::include) }
    points.forEach(::include)

    return if (any) MapBounds(minX, minY, maxX, maxY) else null
}

/** The box for a whole floor: its outline if it has one, otherwise its features. */
fun VenueLevel.contentBounds(): MapBounds? {
    val outlinePolygon = outline
    if (outlinePolygon != null && outlinePolygon.ring.isNotEmpty()) {
        return bounds(listOf(outlinePolygon))
    }
    val areas = features.mapNotNull { (it.shape as? FeatureShape.Area)?.polygon }
    val markers = features.mapNotNull { (it.shape as? FeatureShape.Marker)?.point }
    return bounds(areas, markers)
}

/**
 * The area-weighted centroid of a polygon's exterior ring — the label anchor
 * when the data does not supply one.
 *
 * Not the average of the vertices: that is pulled toward whichever wall was
 * traced with the most points, which for a room with a bay window puts the
 * label in the window. Falls back to the vertex mean only for a degenerate
 * ring, where the shoelace area is zero and the proper formula divides by it.
 */
fun centroid(polygon: Polygon): Point? {
    val ring = polygon.ring
    if (ring.isEmpty()) return null
    if (ring.size < 3) {
        return Point(ring.sumOf { it.x.toDouble() }.toFloat() / ring.size, ring.sumOf { it.y.toDouble() }.toFloat() / ring.size)
    }

    var twiceArea = 0.0
    var cx = 0.0
    var cy = 0.0
    var j = ring.size - 1
    for (i in ring.indices) {
        val a = ring[j]
        val b = ring[i]
        val cross = a.x.toDouble() * b.y - b.x.toDouble() * a.y
        twiceArea += cross
        cx += (a.x + b.x) * cross
        cy += (a.y + b.y) * cross
        j = i
    }

    if (abs(twiceArea) < 1e-9) {
        return Point(
            ring.sumOf { it.x.toDouble() }.toFloat() / ring.size,
            ring.sumOf { it.y.toDouble() }.toFloat() / ring.size,
        )
    }
    return Point((cx / (3.0 * twiceArea)).toFloat(), (cy / (3.0 * twiceArea)).toFloat())
}

/** Where a feature's name should be drawn, in venue metres. */
fun MapFeature.labelPoint(): Point? = labelAnchor ?: when (val s = shape) {
    is FeatureShape.Area -> centroid(s.polygon)
    is FeatureShape.Marker -> s.point
}

/**
 * The transform that fits [bounds] inside a canvas, centred, with [padding]
 * pixels clear on every side.
 *
 * A zero-width or zero-height box (one marker on an untraced floor) would make
 * the scale infinite, so it is treated as a [FALLBACK_SPAN]-metre span
 * instead — arbitrary, but it puts the pin in the middle of the screen at a
 * plausible zoom rather than dividing by zero.
 */
fun fitTransform(
    bounds: MapBounds,
    canvasWidth: Float,
    canvasHeight: Float,
    padding: Float = 0f,
): MapTransform {
    val availableWidth = max(canvasWidth - 2f * padding, 1f)
    val availableHeight = max(canvasHeight - 2f * padding, 1f)
    val spanX = if (bounds.width > EPSILON) bounds.width else FALLBACK_SPAN
    val spanY = if (bounds.height > EPSILON) bounds.height else FALLBACK_SPAN

    val scale = min(availableWidth / spanX, availableHeight / spanY)
    // Centre the *box*, not the canvas: offset places the box's midpoint on the
    // canvas's midpoint, which is why padding only has to bound the scale.
    val offsetX = canvasWidth / 2f - bounds.centerX * scale
    val offsetY = canvasHeight / 2f - bounds.centerY * scale
    return MapTransform(scale, offsetX, offsetY)
}

/**
 * Ray casting, honouring holes.
 *
 * A point exactly on an edge counts as inside. That is not the textbook answer
 * — ray casting is famously unstable there — but it is the right one for a tap
 * target: a finger landing on a wall means the room, and the alternative is a
 * tap that does nothing for reasons invisible to the person tapping. The same
 * rule applies to a hole's boundary, so the wall around a lift core still
 * belongs to the room.
 *
 * @param epsilon on-edge tolerance, in venue metres. The default is a
 *   millimetre: far below anything a floor plan traces, far above Float noise.
 */
fun contains(polygon: Polygon, point: Point, epsilon: Float = ON_EDGE_EPSILON): Boolean {
    if (polygon.ring.size < 3) return false
    if (onBoundary(polygon.ring, point, epsilon)) return true
    if (!insideRing(polygon.ring, point)) return false
    return polygon.holes.none { hole ->
        hole.size >= 3 && insideRing(hole, point) && !onBoundary(hole, point, epsilon)
    }
}

/** Strict interior test; [contains] adds the on-edge and hole rules. */
private fun insideRing(ring: List<Point>, point: Point): Boolean {
    var inside = false
    var j = ring.size - 1
    for (i in ring.indices) {
        val a = ring[i]
        val b = ring[j]
        // The (a.y > y) != (b.y > y) form is the half-open one: it counts each
        // crossing exactly once, so a ray passing through a vertex does not
        // flip the parity twice.
        if ((a.y > point.y) != (b.y > point.y)) {
            val t = (point.y - a.y) / (b.y - a.y)
            if (point.x < a.x + t * (b.x - a.x)) inside = !inside
        }
        j = i
    }
    return inside
}

private fun onBoundary(ring: List<Point>, point: Point, epsilon: Float): Boolean {
    var j = ring.size - 1
    for (i in ring.indices) {
        if (distanceToSegment(point, ring[j], ring[i]) <= epsilon) return true
        j = i
    }
    return false
}

private fun distanceToSegment(p: Point, a: Point, b: Point): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared < EPSILON) return hypotenuse(p.x - a.x, p.y - a.y)
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lengthSquared).coerceIn(0f, 1f)
    return hypotenuse(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
}

private fun hypotenuse(x: Float, y: Float): Float = kotlin.math.sqrt(x * x + y * y)

/**
 * The feature under a tap, or null.
 *
 * Markers win over areas, and later features win over earlier ones. Both follow
 * from draw order: a pin is drawn on top of the room it sits in, so a tap that
 * looks like it hit the pin has to select the pin.
 *
 * @param markerRadius how far from a pin still counts as hitting it, in venue
 *   metres — the caller converts the on-screen touch slop into metres, so the
 *   target stays finger-sized at every zoom level.
 */
fun hitTest(features: List<MapFeature>, point: Point, markerRadius: Float): MapFeature? {
    features.lastOrNull { feature ->
        val marker = (feature.shape as? FeatureShape.Marker)?.point ?: return@lastOrNull false
        hypotenuse(point.x - marker.x, point.y - marker.y) <= markerRadius
    }?.let { return it }

    return features.lastOrNull { feature ->
        val area = (feature.shape as? FeatureShape.Area)?.polygon ?: return@lastOrNull false
        contains(area, point)
    }
}

private const val EPSILON = 1e-6f
private const val ON_EDGE_EPSILON = 1e-3f

/** Metres of span assumed for a floor with no extent — see [fitTransform]. */
private const val FALLBACK_SPAN = 20f

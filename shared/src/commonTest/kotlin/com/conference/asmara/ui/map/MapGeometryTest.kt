package com.conference.asmara.ui.map

import com.conference.asmara.domain.model.FeatureShape
import com.conference.asmara.domain.model.MapCategory
import com.conference.asmara.domain.model.MapFeature
import com.conference.asmara.domain.model.Point
import com.conference.asmara.domain.model.Polygon
import com.conference.asmara.domain.model.VenueLevel
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private fun rect(x0: Float, y0: Float, x1: Float, y1: Float) = Polygon(
    listOf(Point(x0, y0), Point(x1, y0), Point(x1, y1), Point(x0, y1)),
)

private fun assertClose(expected: Float, actual: Float, tolerance: Float = 1e-3f) {
    assertTrue(abs(expected - actual) <= tolerance, "expected $expected but was $actual")
}

private fun feature(
    id: String,
    shape: FeatureShape,
    category: MapCategory = MapCategory.ROOM,
) = MapFeature(
    id = id,
    slug = id,
    name = id,
    category = category,
    locationId = null,
    shape = shape,
    labelAnchor = null,
    sortOrder = 0,
)

class MapGeometryTest {

    // -- bounds ------------------------------------------------------------

    @Test
    fun boundsOfNothingIsNull() {
        assertNull(bounds(emptyList()))
    }

    @Test
    fun boundsOfOnePolygonIsThatPolygon() {
        val box = bounds(listOf(rect(2f, 3f, 10f, 7f)))!!
        assertEquals(2f, box.minX)
        assertEquals(3f, box.minY)
        assertEquals(10f, box.maxX)
        assertEquals(7f, box.maxY)
        assertEquals(8f, box.width)
        assertEquals(4f, box.height)
    }

    @Test
    fun boundsSpanEveryPolygonAndPoint() {
        val box = bounds(
            polygons = listOf(rect(0f, 0f, 4f, 4f), rect(10f, -2f, 12f, 1f)),
            points = listOf(Point(-5f, 20f)),
        )!!
        assertEquals(-5f, box.minX)
        assertEquals(-2f, box.minY)
        assertEquals(12f, box.maxX)
        assertEquals(20f, box.maxY)
    }

    @Test
    fun holesCannotWidenTheBounds() {
        // A hole is inside its ring by definition, so including it would only
        // ever be a chance to get the answer wrong.
        val withHole = rect(0f, 0f, 10f, 10f).copy(holes = listOf(rect(2f, 2f, 4f, 4f).ring))
        val box = bounds(listOf(withHole))!!
        assertEquals(0f, box.minX)
        assertEquals(10f, box.maxX)
    }

    @Test
    fun levelBoundsPreferTheOutlineAndFallBackToFeatures() {
        val outlined = VenueLevel(
            id = "l1", slug = "l1", name = "L1", ordinal = 0,
            outline = rect(0f, 0f, 100f, 100f),
            features = listOf(feature("a", FeatureShape.Area(rect(1f, 1f, 2f, 2f)))),
        )
        assertEquals(100f, outlined.contentBounds()!!.maxX)

        val untraced = outlined.copy(outline = null)
        assertEquals(2f, untraced.contentBounds()!!.maxX)

        val markerOnly = outlined.copy(
            outline = null,
            features = listOf(feature("pin", FeatureShape.Marker(Point(7f, 9f)))),
        )
        val box = markerOnly.contentBounds()!!
        assertEquals(7f, box.minX)
        assertEquals(9f, box.maxY)

        assertNull(outlined.copy(outline = null, features = emptyList()).contentBounds())
    }

    // -- centroid ----------------------------------------------------------

    @Test
    fun centroidOfARectangleIsItsMiddle() {
        val c = centroid(rect(0f, 0f, 10f, 4f))!!
        assertClose(5f, c.x)
        assertClose(2f, c.y)
    }

    @Test
    fun centroidIsAreaWeightedNotVertexAveraged() {
        // One wall traced with extra vertices. The vertex mean would be pulled
        // toward it; the area centroid must not move.
        val denseWall = Polygon(
            listOf(
                Point(0f, 0f), Point(2f, 0f), Point(4f, 0f), Point(6f, 0f),
                Point(8f, 0f), Point(10f, 0f), Point(10f, 4f), Point(0f, 4f),
            ),
        )
        val c = centroid(denseWall)!!
        assertClose(5f, c.x)
        assertClose(2f, c.y)
    }

    @Test
    fun centroidOfADegenerateRingFallsBackToTheVertexMean() {
        // Collinear: zero area, so the proper formula would divide by zero.
        val line = Polygon(listOf(Point(0f, 0f), Point(2f, 0f), Point(4f, 0f)))
        val c = centroid(line)!!
        assertClose(2f, c.x)
        assertClose(0f, c.y)

        assertNull(centroid(Polygon(emptyList())))
    }

    @Test
    fun windingDirectionDoesNotChangeTheCentroid() {
        val clockwise = rect(0f, 0f, 10f, 4f)
        val counterClockwise = Polygon(clockwise.ring.reversed())
        assertClose(centroid(clockwise)!!.x, centroid(counterClockwise)!!.x)
        assertClose(centroid(clockwise)!!.y, centroid(counterClockwise)!!.y)
    }

    // -- fitTransform ------------------------------------------------------

    @Test
    fun fitScalesToTheTighterAxisAndCentresTheContent() {
        // 20x10 metres into a 200x200 canvas: width is the binding constraint,
        // so scale is 10 px/m and the content is vertically centred.
        val transform = fitTransform(MapBounds(0f, 0f, 20f, 10f), 200f, 200f)
        assertClose(10f, transform.scale)
        assertClose(0f, transform.toCanvasX(0f))
        assertClose(200f, transform.toCanvasX(20f))
        assertClose(50f, transform.toCanvasY(0f))
        assertClose(150f, transform.toCanvasY(10f))
    }

    @Test
    fun paddingShrinksTheScaleAndKeepsTheCentre() {
        val transform = fitTransform(MapBounds(0f, 0f, 20f, 20f), 200f, 200f, padding = 20f)
        assertClose(8f, transform.scale)
        assertClose(20f, transform.toCanvasX(0f))
        assertClose(180f, transform.toCanvasX(20f))
    }

    @Test
    fun aZeroSpanBoxDoesNotProduceAnInfiniteScale() {
        // One pin on an untraced floor. The transform must still be finite and
        // must put the pin in the middle of the canvas.
        val transform = fitTransform(MapBounds(5f, 5f, 5f, 5f), 200f, 100f)
        assertTrue(transform.scale.isFinite() && transform.scale > 0f)
        assertClose(100f, transform.toCanvasX(5f))
        assertClose(50f, transform.toCanvasY(5f))
    }

    @Test
    fun theInverseTransformRoundTrips() {
        val transform = fitTransform(MapBounds(-3f, 7f, 17f, 27f), 300f, 500f, padding = 12f)
        val venue = transform.toVenue(123f, 456f)
        assertClose(123f, transform.toCanvasX(venue.x))
        assertClose(456f, transform.toCanvasY(venue.y))
    }

    // -- contains ----------------------------------------------------------

    @Test
    fun containsIsTrueInsideAndFalseOutside() {
        val room = rect(0f, 0f, 10f, 10f)
        assertTrue(contains(room, Point(5f, 5f)))
        assertFalse(contains(room, Point(-1f, 5f)))
        assertFalse(contains(room, Point(11f, 5f)))
        assertFalse(contains(room, Point(5f, -0.5f)))
        assertFalse(contains(room, Point(5f, 10.5f)))
    }

    @Test
    fun aPointOnAnEdgeOrACornerCountsAsInside() {
        // Not the textbook answer, but the right one for a tap target: a finger
        // on the wall means the room.
        val room = rect(0f, 0f, 10f, 10f)
        assertTrue(contains(room, Point(0f, 5f)))
        assertTrue(contains(room, Point(10f, 5f)))
        assertTrue(contains(room, Point(5f, 0f)))
        assertTrue(contains(room, Point(0f, 0f)))
        assertTrue(contains(room, Point(10f, 10f)))
    }

    @Test
    fun aPointInAHoleIsOutsideButItsWallIsNot() {
        val courtyard = rect(0f, 0f, 10f, 10f).copy(holes = listOf(rect(4f, 4f, 6f, 6f).ring))
        assertFalse(contains(courtyard, Point(5f, 5f)))
        assertTrue(contains(courtyard, Point(2f, 2f)))
        // On the hole's boundary: that is the wall around the courtyard, and it
        // still belongs to the room.
        assertTrue(contains(courtyard, Point(4f, 5f)))
    }

    @Test
    fun aRayThroughAVertexDoesNotCountTheCrossingTwice() {
        // The classic ray-casting failure: y aligned exactly with a vertex.
        // Diamond with vertices at (5,0), (10,5), (5,10), (0,5).
        val diamond = Polygon(listOf(Point(5f, 0f), Point(10f, 5f), Point(5f, 10f), Point(0f, 5f)))
        assertTrue(contains(diamond, Point(5f, 5f)))
        assertFalse(contains(diamond, Point(1f, 1f)))
        assertFalse(contains(diamond, Point(9f, 9f)))
    }

    @Test
    fun anExplicitlyClosedRingBehavesLikeAnOpenOne() {
        // The mapper strips the repeated vertex, but hand-built data may not.
        val closed = Polygon(
            listOf(Point(0f, 0f), Point(10f, 0f), Point(10f, 10f), Point(0f, 10f), Point(0f, 0f)),
        )
        assertTrue(contains(closed, Point(5f, 5f)))
        assertFalse(contains(closed, Point(15f, 5f)))
    }

    @Test
    fun aRingWithTooFewPointsContainsNothing() {
        assertFalse(contains(Polygon(listOf(Point(0f, 0f), Point(1f, 1f))), Point(0.5f, 0.5f)))
        assertFalse(contains(Polygon(emptyList()), Point(0f, 0f)))
    }

    // -- hitTest -----------------------------------------------------------

    @Test
    fun hitTestPrefersTheMarkerDrawnOverTheRoom() {
        val room = feature("room", FeatureShape.Area(rect(0f, 0f, 10f, 10f)))
        val pin = feature("pin", FeatureShape.Marker(Point(5f, 5f)), MapCategory.STAIRS)
        assertSame(pin, hitTest(listOf(room, pin), Point(5.2f, 5f), markerRadius = 1f))
        // Outside the pin's radius, the room underneath still answers.
        assertSame(room, hitTest(listOf(room, pin), Point(9f, 9f), markerRadius = 1f))
    }

    @Test
    fun hitTestPicksTheLastOfTwoOverlappingRooms() {
        // Draw order decides: the one painted on top is the one you tapped.
        val under = feature("under", FeatureShape.Area(rect(0f, 0f, 10f, 10f)))
        val over = feature("over", FeatureShape.Area(rect(5f, 5f, 15f, 15f)))
        assertSame(over, hitTest(listOf(under, over), Point(7f, 7f), markerRadius = 0.5f))
        assertSame(under, hitTest(listOf(under, over), Point(2f, 2f), markerRadius = 0.5f))
    }

    @Test
    fun hitTestReturnsNullOnBareFloor() {
        val room = feature("room", FeatureShape.Area(rect(0f, 0f, 10f, 10f)))
        assertNull(hitTest(listOf(room), Point(50f, 50f), markerRadius = 0.5f))
        assertNull(hitTest(emptyList(), Point(0f, 0f), markerRadius = 0.5f))
    }

    // -- labelPoint --------------------------------------------------------

    @Test
    fun theLabelAnchorWinsOverTheCentroid() {
        // Which is the point of storing one: an L-shaped room's centroid can
        // fall outside the room.
        val room = feature("room", FeatureShape.Area(rect(0f, 0f, 10f, 10f)))
        assertEquals(Point(5f, 5f), room.labelPoint())
        assertEquals(Point(1f, 1f), room.copy(labelAnchor = Point(1f, 1f)).labelPoint())
    }

    @Test
    fun aMarkerLabelsItself() {
        val pin = feature("pin", FeatureShape.Marker(Point(3f, 4f)))
        assertEquals(Point(3f, 4f), pin.labelPoint())
    }
}

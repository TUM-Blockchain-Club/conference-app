package com.conference.asmara.data.mapper

import com.conference.asmara.data.dto.VenueMapDto
import com.conference.asmara.domain.model.FeatureShape
import com.conference.asmara.domain.model.MapCategory
import com.conference.asmara.domain.model.Point
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val json = Json { ignoreUnknownKeys = true }

private fun decode(document: String) = json.decodeFromString<VenueMapDto>(document).toDomain()

/** A one-level, one-feature document with the feature's JSON spliced in. */
private fun documentWith(featureJson: String, outlineJson: String = "null") = """
    {
      "id": "v1", "slug": "venue", "name": "Venue",
      "levels": [
        {
          "id": "l1", "slug": "ground", "name": "Ground", "ordinal": 0,
          "outline": $outlineJson,
          "features": [$featureJson]
        }
      ]
    }
""".trimIndent()

private const val SQUARE = """
    {"type":"Polygon","coordinates":[[[0,0],[10,0],[10,10],[0,10],[0,0]]]}
"""

/**
 * The seed file is traced in QGIS and then hand-edited, and anything written
 * straight into the table bypasses the seed script's validation entirely. So
 * every case here is about *degrading*: one bad room must never cost the
 * attendee the whole map.
 */
class VenueMapMapperTest {

    @Test
    fun aWellFormedDocumentMapsThrough() {
        val map = decode(
            documentWith(
                """
                {
                  "id":"f1","slug":"main-stage","name":"Main Stage","category":"stage",
                  "location_id":"loc-1","geometry":$SQUARE,
                  "label_anchor":{"type":"Point","coordinates":[5,6]},"sort_order":3
                }
                """.trimIndent(),
                outlineJson = SQUARE,
            ),
        )

        assertEquals("Venue", map.name)
        val level = map.levels.single()
        assertEquals(4, level.outline!!.ring.size)

        val feature = level.features.single()
        assertEquals(MapCategory.STAGE, feature.category)
        assertEquals("loc-1", feature.locationId)
        assertEquals(Point(5f, 6f), feature.labelAnchor)
        assertEquals(3, feature.sortOrder)
        val area = assertIs<FeatureShape.Area>(feature.shape)
        // GeoJSON's repeated closing vertex is dropped: five positions in, four
        // out. Every ring in the domain model is in this stripped form.
        assertEquals(4, area.polygon.ring.size)
        assertEquals(Point(0f, 0f), area.polygon.ring.first())
    }

    @Test
    fun aPointGeometryBecomesAMarker() {
        val map = decode(
            documentWith("""{"id":"f1","slug":"lift","name":"Lift","category":"elevator","geometry":{"type":"Point","coordinates":[3,4]}}"""),
        )
        val marker = assertIs<FeatureShape.Marker>(map.levels.single().features.single().shape)
        assertEquals(Point(3f, 4f), marker.point)
    }

    @Test
    fun holesSurviveAsHoles() {
        val withHole = """
            {"type":"Polygon","coordinates":[
              [[0,0],[10,0],[10,10],[0,10],[0,0]],
              [[4,4],[6,4],[6,6],[4,6],[4,4]]
            ]}
        """.trimIndent()
        val map = decode(documentWith("""{"id":"f1","slug":"r","name":"R","geometry":$withHole}"""))
        val area = assertIs<FeatureShape.Area>(map.levels.single().features.single().shape)
        assertEquals(1, area.polygon.holes.size)
        assertEquals(4, area.polygon.holes.single().size)
    }

    @Test
    fun anUnclosedRingIsAccepted() {
        // GeoJSON requires closure; a hand-edited file frequently is not
        // closed, and rejecting it would lose a room over a missing vertex the
        // renderer adds back anyway.
        val open = """{"type":"Polygon","coordinates":[[[0,0],[10,0],[10,10],[0,10]]]}"""
        val map = decode(documentWith("""{"id":"f1","slug":"r","name":"R","geometry":$open}"""))
        val area = assertIs<FeatureShape.Area>(map.levels.single().features.single().shape)
        assertEquals(4, area.polygon.ring.size)
    }

    @Test
    fun anUnknownCategoryDegradesToOther() {
        val map = decode(
            documentWith("""{"id":"f1","slug":"r","name":"R","category":"helipad","geometry":$SQUARE}"""),
        )
        assertEquals(MapCategory.OTHER, map.levels.single().features.single().category)
    }

    @Test
    fun aFeatureWithMalformedGeometryIsDroppedNotThrown() {
        val cases = listOf(
            // Wrong nesting: positions where rings belong.
            """{"type":"Polygon","coordinates":[[0,0],[10,0],[10,10]]}""",
            // Non-numeric coordinates: the export wrote them as text.
            """{"type":"Polygon","coordinates":[[["0","0"],["10","0"],["10","10"]]]}""",
            // Two vertices: not a ring.
            """{"type":"Polygon","coordinates":[[[0,0],[10,0]]]}""",
            // A position with one ordinate.
            """{"type":"Point","coordinates":[5]}""",
            // A type nothing knows.
            """{"type":"LineString","coordinates":[[0,0],[1,1]]}""",
            // Not an object at all.
            """[[0,0],[1,1]]""",
            "null",
        )
        cases.forEach { geometry ->
            val map = decode(documentWith("""{"id":"f1","slug":"r","name":"R","geometry":$geometry}"""))
            assertTrue(
                map.levels.single().features.isEmpty(),
                "expected geometry to be rejected: $geometry",
            )
        }
    }

    @Test
    fun oneBadFeatureDoesNotTakeTheFloorWithIt() {
        val map = decode(
            documentWith(
                """
                {"id":"bad","slug":"bad","name":"Bad","geometry":{"type":"Polygon","coordinates":[[0,0]]}},
                {"id":"good","slug":"good","name":"Good","geometry":$SQUARE}
                """.trimIndent(),
            ),
        )
        assertEquals(listOf("good"), map.levels.single().features.map { it.id })
    }

    @Test
    fun aMalformedOutlineLosesTheFootprintNotTheRooms() {
        val map = decode(
            documentWith(
                """{"id":"f1","slug":"r","name":"R","geometry":$SQUARE}""",
                outlineJson = """{"type":"Polygon","coordinates":"nonsense"}""",
            ),
        )
        assertNull(map.levels.single().outline)
        assertEquals(1, map.levels.single().features.size)
    }

    @Test
    fun aMalformedHoleIsDroppedAndTheRoomSurvives() {
        // A room drawn without its lift core is wrong in a way you can see and
        // work around. A room that vanishes is not.
        val badHole = """
            {"type":"Polygon","coordinates":[
              [[0,0],[10,0],[10,10],[0,10],[0,0]],
              [[4,4],[6,4]]
            ]}
        """.trimIndent()
        val map = decode(documentWith("""{"id":"f1","slug":"r","name":"R","geometry":$badHole}"""))
        val area = assertIs<FeatureShape.Area>(map.levels.single().features.single().shape)
        assertEquals(4, area.polygon.ring.size)
        assertTrue(area.polygon.holes.isEmpty())
    }

    @Test
    fun aMalformedLabelAnchorFallsBackToNull() {
        val map = decode(
            documentWith("""{"id":"f1","slug":"r","name":"R","geometry":$SQUARE,"label_anchor":{"type":"Point","coordinates":[]}}"""),
        )
        assertNull(map.levels.single().features.single().labelAnchor)
    }

    @Test
    fun levelsAndFeaturesAreOrderedHereNotTrustedFromTheServer() {
        val document = """
            {
              "id":"v1","slug":"venue","name":"Venue",
              "levels":[
                {"id":"l2","slug":"first","name":"1st","ordinal":1,"outline":null,"features":[
                  {"id":"b","slug":"b","name":"B","geometry":$SQUARE,"sort_order":5},
                  {"id":"a","slug":"a","name":"A","geometry":$SQUARE,"sort_order":1}
                ]},
                {"id":"l1","slug":"ground","name":"Ground","ordinal":0,"outline":null,"features":[]}
              ]
            }
        """.trimIndent()
        val map = decode(document)
        assertEquals(listOf("ground", "first"), map.levels.map { it.slug })
        assertEquals(listOf("a", "b"), map.levels[1].features.map { it.id })
    }

    @Test
    fun lookupsFindTheFeatureAndItsFloor() {
        val document = """
            {
              "id":"v1","slug":"venue","name":"Venue",
              "levels":[
                {"id":"l1","slug":"ground","name":"Ground","ordinal":0,"outline":null,"features":[]},
                {"id":"l2","slug":"first","name":"1st","ordinal":1,"outline":null,"features":[
                  {"id":"f9","slug":"room-a","name":"Room A","location_id":"loc-9","geometry":$SQUARE}
                ]}
              ]
            }
        """.trimIndent()
        val map = decode(document)
        assertEquals("f9", map.featureForLocation("loc-9")!!.id)
        assertEquals("first", map.levelOfFeature("f9")!!.slug)
        assertNull(map.featureForLocation("nope"))
        assertNull(map.levelOfFeature("nope"))
    }
}

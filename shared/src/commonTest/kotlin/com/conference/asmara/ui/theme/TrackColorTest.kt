package com.conference.asmara.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackColorTest {

    // -- parseHexColor ------------------------------------------------------

    @Test
    fun `parses six digit hex with hash`() {
        assertEquals(Color(0xFF4F46E5), parseHexColor("#4F46E5"))
    }

    @Test
    fun `parses six digit hex without hash`() {
        assertEquals(Color(0xFF4F46E5), parseHexColor("4F46E5"))
    }

    @Test
    fun `parses lowercase hex`() {
        assertEquals(Color(0xFF4F46E5), parseHexColor("#4f46e5"))
    }

    @Test
    fun `expands three digit shorthand`() {
        assertEquals(Color(0xFFAABBCC), parseHexColor("#abc"))
    }

    @Test
    fun `parses eight digit hex as RRGGBBAA`() {
        // CSS orders alpha last; Compose wants it first.
        assertEquals(Color(0x803B82F6), parseHexColor("#3B82F680"))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals(Color(0xFF4F46E5), parseHexColor("  #4F46E5 "))
    }

    @Test
    fun `returns null for null input`() {
        assertNull(parseHexColor(null))
    }

    @Test
    fun `returns null for empty and hash only`() {
        assertNull(parseHexColor(""))
        assertNull(parseHexColor("#"))
    }

    @Test
    fun `returns null for non hex characters`() {
        assertNull(parseHexColor("#GGGGGG"))
        assertNull(parseHexColor("rebeccapurple"))
        assertNull(parseHexColor("#12 34 56"))
    }

    @Test
    fun `returns null for wrong length`() {
        assertNull(parseHexColor("#12345"))
        assertNull(parseHexColor("#1234567"))
        assertNull(parseHexColor("#0123456789"))
    }

    // -- raiseForDark -------------------------------------------------------

    @Test
    fun `raiseForDark lightens every channel`() {
        val source = Color(0xFF4F46E5)
        val raised = raiseForDark(source)
        assertTrue(raised.red > source.red)
        assertTrue(raised.green > source.green)
        assertTrue(raised.blue > source.blue)
    }

    @Test
    fun `raiseForDark preserves alpha`() {
        assertEquals(0.5f, raiseForDark(Color(0x804F46E5)).alpha, absoluteTolerance = 0.01f)
    }

    @Test
    fun `raiseForDark cannot exceed white`() {
        val raised = raiseForDark(Color.White)
        assertEquals(1f, raised.red, absoluteTolerance = 0.001f)
        assertEquals(1f, raised.green, absoluteTolerance = 0.001f)
        assertEquals(1f, raised.blue, absoluteTolerance = 0.001f)
    }

    @Test
    fun `raiseForDark applies the documented formula`() {
        // black -> 0 + (1 - 0) * 0.22
        assertEquals(0.22f, raiseForDark(Color.Black).red, absoluteTolerance = 0.001f)
    }

    // -- trackColor ---------------------------------------------------------

    @Test
    fun `trackColor uplifts a parseable value`() {
        val parsed = assertNotNull(parseHexColor("#059669"))
        assertEquals(raiseForDark(parsed), trackColor("#059669", sortOrder = 0))
    }

    @Test
    fun `trackColor falls back by sort order when colour is missing`() {
        assertEquals(TrackFallback[0], trackColor(null, 0))
        assertEquals(TrackFallback[1], trackColor("", 1))
        assertEquals(TrackFallback[2], trackColor("not-a-colour", 2))
    }

    @Test
    fun `fallback wraps around the palette`() {
        assertEquals(TrackFallback[0], trackColor(null, TrackFallback.size))
        assertEquals(TrackFallback[1], trackColor(null, TrackFallback.size + 1))
    }

    @Test
    fun `fallback handles negative sort order`() {
        // mod, not rem: a negative sortOrder must not index out of bounds.
        assertEquals(TrackFallback[TrackFallback.size - 1], trackColor(null, -1))
    }
}

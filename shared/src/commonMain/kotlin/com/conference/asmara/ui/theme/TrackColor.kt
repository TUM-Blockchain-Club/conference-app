package com.conference.asmara.ui.theme

import androidx.compose.ui.graphics.Color
import com.conference.asmara.domain.model.EventType

/**
 * Colour for data that arrives from Supabase rather than from the design system.
 *
 * `Track.color` is a nullable free-text hex string owned by whoever edits the
 * schedule, so this is the one place in the theme that has to be defensive.
 * Everything here is a pure function — no `@Composable`, no locals — precisely
 * so it can be unit tested; see `TrackColorTest`.
 *
 * Three problems it solves:
 *
 * 1. **Format drift.** The column is a string. Seed data uses `#RRGGBB`, but
 *    `RRGGBB`, `#RGB` and `#RRGGBBAA` all show up in hand-edited rows.
 * 2. **Wrong tonal range.** The seeded values (`#4F46E5`, `#DC2626`, `#059669`)
 *    are Tailwind `*-600` mid-tones, authored for white backgrounds. On
 *    `#111111` they read muddy and sit near the 3:1 floor.
 * 3. **Absence.** `color` is nullable and frequently null for new tracks.
 */

/** How far an authored colour is pulled toward white before use on a dark canvas. */
private const val DARK_UPLIFT = 0.22f

/**
 * Parses a hex colour string, or returns `null` if it is not one.
 *
 * Accepts `#RGB`, `#RRGGBB` and `#RRGGBBAA`, with or without the leading `#`,
 * in either case. Anything else — empty, `"blue"`, `"#12345"`, a stray quote —
 * yields `null` so the caller falls back rather than throwing on user data.
 */
fun parseHexColor(raw: String?): Color? {
    val hex = raw?.trim()?.removePrefix("#") ?: return null
    if (hex.any { it !in "0123456789abcdefABCDEF" }) return null

    val rgba: Long = when (hex.length) {
        // #RGB -> expand each nibble: "abc" -> "aabbcc"
        3 -> {
            val r = hex[0]
            val g = hex[1]
            val b = hex[2]
            "$r$r$g$g$b$b".toLong(16) or 0xFF000000L
        }
        6 -> hex.toLong(16) or 0xFF000000L
        // #RRGGBBAA -> Compose wants AARRGGBB
        8 -> {
            val value = hex.toLong(16)
            val alpha = value and 0xFFL
            (alpha shl 24) or (value ushr 8)
        }
        else -> return null
    }
    return Color(rgba.toInt())
}

/**
 * Lifts a colour toward white so a mid-tone authored for a light background
 * stays legible on `#111111`.
 *
 * `c + (1 - c) * 0.22` per channel. Multiplying instead would darken; adding a
 * constant would clip the already-bright channels and skew the hue. This form
 * is a lerp toward white, so it preserves hue, cannot overflow, and leaves
 * already-light colours nearly untouched.
 */
fun raiseForDark(color: Color): Color = Color(
    red = color.red + (1f - color.red) * DARK_UPLIFT,
    green = color.green + (1f - color.green) * DARK_UPLIFT,
    blue = color.blue + (1f - color.blue) * DARK_UPLIFT,
    alpha = color.alpha,
)

/**
 * The colour to paint a track's dot, bar or badge.
 *
 * @param hex the raw `Track.color` value, however malformed.
 * @param sortOrder used to pick a stable fallback so two colourless tracks in
 *   the same list never collide. Floor-mod, so a negative or zero sort order is
 *   safe.
 */
fun trackColor(hex: String?, sortOrder: Int): Color =
    parseHexColor(hex)?.let(::raiseForDark) ?: TrackFallback[sortOrder.mod(TrackFallback.size)]

/**
 * Colour for an [EventType].
 *
 * Unlike track colours these are ours, not the database's, so they come
 * straight from the palette: keynote and talk carry the accent, workshops the
 * secondary, breaks stay neutral so they visually recede in a schedule list.
 */
fun eventTypeColor(type: EventType): Color = when (type) {
    EventType.KEYNOTE -> Violet400
    EventType.TALK -> Blue400
    EventType.PANEL -> Green400
    EventType.WORKSHOP -> Amber400
    EventType.BREAK -> Grey500
    EventType.OTHER -> Grey500
}

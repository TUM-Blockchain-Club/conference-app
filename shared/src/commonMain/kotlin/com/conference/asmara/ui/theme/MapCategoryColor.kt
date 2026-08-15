package com.conference.asmara.ui.theme

import androidx.compose.ui.graphics.Color
import com.conference.asmara.domain.model.MapCategory

/**
 * Colour for map features.
 *
 * Built exactly like [eventTypeColor]: a pure `when` over palette values, no
 * `Color(0x…)`, no new hexes. Categories are ours, not the database's, so
 * unlike [trackColor] there is nothing to parse defensively — an unknown
 * category has already degraded to [MapCategory.OTHER] in the mapper.
 *
 * Ten categories over five tones, deliberately. A ten-hue legend is not
 * something anyone reads, and colour is never the only signal here anyway:
 * every feature is labelled on the canvas, and the selected one names its
 * category in a badge. The grouping is by *what you are looking for* — a stage
 * and a workshop room are different errands, a staircase and a lift are the
 * same one.
 */
fun mapCategoryColor(category: MapCategory): Color = when (category) {
    MapCategory.STAGE -> Violet400
    MapCategory.ROOM -> Blue400
    MapCategory.BOOTH -> Green400
    MapCategory.ENTRANCE -> Green400
    MapCategory.FOOD -> Amber400
    // Circulation and plumbing: present so you can orient, never the thing you
    // came to find, so they recede into the neutral.
    MapCategory.RESTROOM -> Grey500
    MapCategory.STAIRS -> Grey500
    MapCategory.ELEVATOR -> Grey500
    MapCategory.CORRIDOR -> Grey500
    MapCategory.OTHER -> Grey500
}

/**
 * The **opaque** fill for a feature drawn on [surface].
 *
 * A room painted in [mapCategoryColor] at full strength is a colour field, not
 * a floor plan — twenty rooms of it and the labels stop being readable. The
 * design system's answer to "I need a dimmer version of this" is
 * `flatten(base, α)`, the same formula the stat-tile tints are baked from, and
 * that is what this is: the hue composited over the surface at [FILL_ALPHA] and
 * frozen.
 *
 * Computed rather than baked into `Palette.kt` because the result depends on
 * what is behind it, and the canvas draws features over the level outline
 * (`surfaceCard`) — a constant flattened against `surfaceBase` would be
 * visibly wrong there. Pure, so `MapCategoryColorTest` can pin it.
 */
fun mapCategoryFill(category: MapCategory, surface: Color): Color =
    flatten(mapCategoryColor(category), surface, FILL_ALPHA)

/**
 * `flatten(base, α)` — composite [base] over [over] at [alpha] and return the
 * opaque result. Documented in `docs/DESIGN.md`; the point is that the same
 * token cannot render as three different colours depending on what sits behind
 * it, which is what a translucent fill does.
 */
internal fun flatten(base: Color, over: Color, alpha: Float): Color = Color(
    red = over.red + (base.red - over.red) * alpha,
    green = over.green + (base.green - over.green) * alpha,
    blue = over.blue + (base.blue - over.blue) * alpha,
    alpha = 1f,
)

/**
 * 0.22 — the lowest value at which the four hues are still tellable apart on
 * `#1C1C1C`, and low enough that `#F2F2F2` labels stay well clear of 4.5:1 on
 * every one of them.
 */
private const val FILL_ALPHA = 0.22f

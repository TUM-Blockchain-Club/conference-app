package com.conference.asmara.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The decorative layer: the primary-button gradient, the hero text gradient,
 * and the grid + glow backdrop from the web platform's landing page.
 *
 * All of it is ornament. Nothing here encodes state or meaning, which is what
 * lets [Modifier.gridPattern] and [Modifier.glow] be dropped without
 * consequence when a screen needs to stay quiet.
 */

/**
 * `linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)` — the primary button.
 *
 * CSS `135deg` runs top-left to bottom-right, which is exactly what
 * [Brush.linearGradient] does with its default `Offset.Zero` -> `Offset.Infinite`
 * endpoints. The angle is therefore correct without arithmetic, though it does
 * mean the gradient's slope follows the element's aspect ratio rather than
 * staying pinned at 45 degrees. On a pill button that difference is invisible.
 */
internal fun primaryGradient(): Brush = Brush.linearGradient(listOf(Blue500, Violet500))

/**
 * `linear-gradient(300deg, cyan, violet, pink)` — the `.text-gradient` class,
 * used for hero wordmarks.
 *
 * 300deg is *not* a corner-to-corner run, so this one needs real endpoints:
 * CSS angles are measured clockwise from "up", giving a direction vector of
 * `(sin 300deg, -cos 300deg)` = `(-0.866, -0.5)` in screen coordinates —
 * bottom-right to top-left, shallow.
 */
internal fun heroTextGradient(size: Size): Brush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF00EBFF),
        Color(0xFF6F3DE2),
        Color(0xFFFFB6EC),
    ),
    start = Offset(size.width, size.height),
    end = Offset(size.width * 0.134f, size.height * 0.5f),
)

/**
 * Draws the faint 1px grid from `.grid-pattern`.
 *
 * The web uses a 50px cell that drops to 30px under `max-width: 640px`; a phone
 * is always in that branch, so 30dp is the only size implemented.
 *
 * Drawn with [drawWithCache] rather than a tiled image: the line count on a
 * phone is trivial and this avoids shipping an asset that would need two
 * densities.
 */
fun Modifier.gridPattern(
    color: Color,
    cell: Dp = 30.dp,
    lineWidth: Dp = 1.dp,
): Modifier = this.drawWithCache {
    val step = cell.toPx()
    val stroke = lineWidth.toPx()
    onDrawBehind {
        var x = 0f
        while (x <= size.width) {
            drawRect(color = color, topLeft = Offset(x, 0f), size = Size(stroke, size.height))
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawRect(color = color, topLeft = Offset(0f, y), size = Size(size.width, stroke))
            y += step
        }
    }
}

/**
 * `.grid-glow` — a soft radial wash that keeps a full-bleed `#111111` screen
 * from reading as a dead rectangle.
 *
 * **This is the one layer in the system that deliberately tints the canvas.**
 * It lifts `#111111` to roughly `#13171D` at its strongest, which is a visible
 * blue-violet cast and is faithful to the source: the web platform's hero page
 * measures `#151721` in the same region. Principle 1 ("no colour cast") governs
 * the neutral *tokens*, not this decorative overlay.
 *
 * The practical consequence: audit the surface ladder on an **undecorated**
 * screen. `GalleryScreen` renders with `decorated = false` for exactly this
 * reason.
 *
 * [centerBias] moves the glow's centre vertically as a fraction of height; the
 * web centres it, but on a tall phone screen an upper-third centre sits behind
 * the header where it actually reads.
 */
fun Modifier.glow(
    inner: Color,
    mid: Color,
    centerBias: Float = 0.35f,
    radiusFactor: Float = 0.9f,
): Modifier = this.drawBehind {
    val center = Offset(size.width / 2f, size.height * centerBias)
    val radius = maxOf(size.width, size.height) * radiusFactor
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to inner,
                0.3f to mid,
                0.7f to Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * The blue-to-violet fill used by the primary button.
 *
 * A `Modifier.background(brush)` rather than a `ButtonColors` because M3 button
 * colours are flat `Color`s with no brush slot.
 */
internal fun Modifier.primaryGradientBackground(shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.background(brush = primaryGradient(), shape = shape)

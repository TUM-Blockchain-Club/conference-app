package com.conference.asmara.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Mirrors the web platform's radius scale, which is derived from a single
 * `--radius: 0.5rem` knob:
 *
 * ```
 * --radius-sm: calc(var(--radius) - 4px)  ->  4px
 * --radius-md: calc(var(--radius) - 2px)  ->  6px
 * --radius-lg: var(--radius)              ->  8px
 * --radius-xl: calc(var(--radius) + 4px)  -> 12px
 * ```
 *
 * `medium` is the default: it is what a [com.conference.asmara.ui.components.TbcCard]
 * and a text field use, so an unstyled M3 component that reaches for
 * `shapes.medium` already lands on the house radius.
 *
 * Fully-rounded pills live in `TbcTheme.tokens.pill` — `Shapes` has no slot for
 * them.
 */
internal fun tbcShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

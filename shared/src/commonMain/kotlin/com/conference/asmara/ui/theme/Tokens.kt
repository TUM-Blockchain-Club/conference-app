package com.conference.asmara.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * The half of the design system that stock Material 3 never reads.
 *
 * Everything here would either have no M3 role at all (stat-tile tints, banner
 * tints, the decorative glow) or would be mangled if squeezed into one (the
 * three-step surface ladder collapses into `surfaceContainer*` slots that other
 * components also read).
 *
 * Provided through [LocalTbcTokens], a `staticCompositionLocalOf`: the app is
 * dark-only and these values never change at runtime, so paying for
 * read-tracking on every access would be pure overhead.
 */
@Immutable
data class TbcTokens(
    // -- Surfaces: the ladder. Each step is separated by [borderSubtle], never
    //    by a shadow.
    /** `#111111` — the app canvas. */
    val surfaceBase: Color,
    /** `#1C1C1C` — cards, list rows, sheets. */
    val surfaceCard: Color,
    /** `#242424` — inputs, inactive chips, progress-bar tracks. */
    val surfaceMuted: Color,
    /** `#252525` — popovers, menus, the highest neutral step. */
    val surfaceRaised: Color,

    // -- Text
    /** 17.4:1 on the canvas. Titles, values, body copy. */
    val textPrimary: Color,
    /** 5.47:1. Labels, helper text, secondary copy. Passes AA. */
    val textMuted: Color,
    /** 3.54:1 — decorative or >=18.66sp bold only. Never body text. */
    val textFaint: Color,
    /** Disabled foreground. */
    val textDisabled: Color,
    /**
     * Secondary text *on a stat tile*. [textMuted] measures only 3.0–3.8:1
     * against the lighter end of a tile gradient, so tiles get their own,
     * lighter secondary tone.
     */
    val textOnTile: Color,

    // -- Strokes. Alpha is deliberate: a hairline must adopt whatever surface
    //    sits behind it.
    val borderSubtle: Color,
    val borderStrong: Color,
    val borderFocus: Color,

    // -- State layers
    val statePressed: Color,

    // -- Accent
    val accent: Color,
    val accentPressed: Color,
    val accentSoft: Color,
    /** Gradient partner only. Never a standalone fill. */
    val accentSecondary: Color,
    val onAccent: Color,

    // -- Status
    val success: Color,
    val warning: Color,
    val danger: Color,

    // -- Tints for [com.conference.asmara.ui.components.Banner]
    val dangerTint: Color,
    val dangerBorder: Color,
    val warningTint: Color,
    val warningBorder: Color,
    val accentTint: Color,
    val accentBorder: Color,

    // -- Stat tiles
    val stats: StatTilePalette,

    // -- Decorative backdrop
    val gridLine: Color,
    val glowInner: Color,
    val glowMid: Color,

    /** M3's `Shapes` has no fully-rounded slot; buttons, tabs and badges need one. */
    val pill: Shape,
)

/** One stat tile's derived tint set. See the derivation table in `docs/DESIGN.md`. */
@Immutable
data class StatAccent(
    /** `flatten(base, 0.18)` — gradient start. */
    val start: Color,
    /** `flatten(base, 0.30)` — gradient end. */
    val end: Color,
    /** `flatten(base, 0.45)` — hairline. */
    val border: Color,
    /** The Tailwind `*-400` tone. Icons and accent marks. */
    val icon: Color,
    /** The undiluted brand colour, for dots and bar fills. */
    val base: Color,
)

@Immutable
data class StatTilePalette(
    val blue: StatAccent,
    val green: StatAccent,
    val violet: StatAccent,
    val amber: StatAccent,
) {
    /** Cycles the four accents for generated content (e.g. a stat row per track). */
    fun byIndex(index: Int): StatAccent =
        when (index.mod(4)) {
            0 -> blue
            1 -> green
            2 -> violet
            else -> amber
        }
}

internal fun tbcTokens(): TbcTokens = TbcTokens(
    surfaceBase = Ink900,
    surfaceCard = Ink800,
    surfaceMuted = Ink700,
    surfaceRaised = Ink600,
    textPrimary = Snow100,
    textMuted = Grey500,
    textFaint = Grey600,
    textDisabled = Grey700,
    textOnTile = Grey300,
    borderSubtle = BorderSubtle,
    borderStrong = BorderStrong,
    borderFocus = BorderFocus,
    statePressed = StateLayerPressed,
    accent = Blue500,
    accentPressed = Blue600,
    accentSoft = Blue400,
    accentSecondary = Violet500,
    onAccent = White,
    success = Green500,
    warning = Amber500,
    danger = Red500,
    dangerTint = DangerTint,
    dangerBorder = DangerBorder,
    warningTint = WarningTint,
    warningBorder = WarningBorder,
    accentTint = AccentTint,
    accentBorder = AccentBorder,
    stats = StatTilePalette(
        blue = StatAccent(TileBlueStart, TileBlueEnd, TileBlueBorder, Blue400, Blue500),
        green = StatAccent(TileGreenStart, TileGreenEnd, TileGreenBorder, Green400, Green500),
        violet = StatAccent(TileVioletStart, TileVioletEnd, TileVioletBorder, Violet400, Violet500),
        amber = StatAccent(TileAmberStart, TileAmberEnd, TileAmberBorder, Amber400, Amber500),
    ),
    gridLine = GridLine,
    glowInner = GlowVioletInner,
    glowMid = GlowBlueMid,
    pill = RoundedCornerShape(percent = 50),
)

/**
 * No sensible default: falling back to a stub would let a screen render
 * *almost* correctly outside [TbcTheme] and hide the missing wrapper until a
 * designer noticed. Crash loudly instead.
 */
val LocalTbcTokens = staticCompositionLocalOf<TbcTokens> {
    error("No TbcTokens found. Wrap your content in TbcTheme { }.")
}

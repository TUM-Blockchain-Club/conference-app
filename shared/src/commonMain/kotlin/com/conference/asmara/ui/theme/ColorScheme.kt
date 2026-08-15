package com.conference.asmara.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme

/**
 * The M3 [ColorScheme] half of the token layer.
 *
 * Split rule: *does a stock Material 3 component read this?* If yes it belongs
 * here — including roles the web platform has no equivalent for, because an
 * unfilled role falls back to baseline M3 purple the moment anyone drops a
 * stock `TextField` or `Snackbar` on screen.
 *
 * If no, it belongs in [TbcTokens]. See `docs/DESIGN.md` for the full mapping
 * table with the reasoning per role.
 */
internal fun tbcDarkColorScheme(): ColorScheme = darkColorScheme(
    // --primary / --accent. White-on-blue is 3.68:1, which clears the 3:1 bar
    // for UI components but not the 4.5:1 body-text bar — hence the rule that
    // filled blue carries icons and short labels only.
    primary = Blue500,
    onPrimary = White,
    primaryContainer = TileBlueEnd,
    onPrimaryContainer = Blue400,
    inversePrimary = Blue600,

    // Violet is a gradient partner, not a second accent. It is mapped so stock
    // components that reach for `secondary` land somewhere sane rather than on
    // baseline purple, but no TBC component uses it as a fill.
    secondary = Violet500,
    onSecondary = White,
    secondaryContainer = TileVioletEnd,
    onSecondaryContainer = Violet400,

    tertiary = Green500,
    onTertiary = Ink900,
    tertiaryContainer = TileGreenEnd,
    onTertiaryContainer = Green400,

    // `#111111`, NOT `#1C1C1C`. Scaffold's containerColor is `surface`, not
    // `background` — map surface to the card colour and every screen silently
    // gets a `#1C1C1C` canvas. Cards read `surfaceContainer` instead.
    background = Ink900,
    onBackground = Snow100,
    surface = Ink900,
    onSurface = Snow100,

    // The flattened `rgba(255,255,255,0.06)` from `--muted` / `--input`.
    surfaceVariant = Ink700,
    onSurfaceVariant = Grey500,

    surfaceContainerLowest = Ink900,
    surfaceContainerLow = Ink800,
    surfaceContainer = Ink800, // the card colour
    surfaceContainerHigh = Ink700,
    surfaceContainerHighest = Ink600, // --popover
    surfaceBright = Ink600,
    surfaceDim = Ink900,

    // M3 computes elevated surfaces as
    //   surfaceTint.copy(alpha = f(elevation)).compositeOver(surface)
    // Left at its default (`primary`), every elevated surface picks up a blue
    // cast — a direct violation of "pure neutral dark, no colour cast". Pinning
    // the tint to the base colour makes the formula a no-op. Belt and braces:
    // every TBC component also passes `tonalElevation = 0.dp`.
    surfaceTint = Ink900,

    // --border at 8% and the stronger 20% stroke.
    outline = BorderStrong,
    outlineVariant = BorderSubtle,

    error = Red500,
    onError = White,
    errorContainer = DangerTint,
    onErrorContainer = Red400,

    // Intentional deviation from M3 semantics: `inverseSurface` is supposed to
    // be the *light* counterpart, which is exactly what makes a stock Snackbar
    // flash as a white chip on a `#111111` app. Forced dark.
    inverseSurface = Ink500,
    inverseOnSurface = Snow100,

    scrim = Ink900,
)

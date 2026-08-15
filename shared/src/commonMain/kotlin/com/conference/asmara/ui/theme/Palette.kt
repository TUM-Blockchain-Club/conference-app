package com.conference.asmara.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The **only** file in the codebase that may contain hex colour literals.
 *
 * Everything else reads colours through [MaterialTheme.colorScheme] or
 * [TbcTheme.tokens]. Values are transcribed from the TBC web platform's
 * Tailwind v4 `global.css` `:root` block; derived tints follow the
 * `flatten(base, alpha) = 17 + (channel - 17) * alpha` formula documented in
 * `docs/DESIGN.md` (composite over `#111111`).
 */

// ---------------------------------------------------------------------------
// Neutral ladder — depth is expressed as a surface *value* step plus a
// hairline border, never as a shadow. Pure greys: R == G == B, no colour cast.
// ---------------------------------------------------------------------------

/** `--background`. App canvas. */
internal val Ink900 = Color(0xFF111111)

/** `--card`. One step up from the canvas. */
internal val Ink800 = Color(0xFF1C1C1C)

/**
 * The web's `--muted` / `--secondary`, flattened to an opaque value so the same
 * token cannot render as three different greys depending on what sits behind
 * it. See the alpha-vs-opaque policy in `docs/DESIGN.md`.
 *
 * This is `flatten(#FFFFFF, 0.08)`, not the literal `rgba(255,255,255,0.06)`,
 * which would give `#1F1F1F` — only fourteen 8-bit steps above the canvas, and
 * on an OLED panel that reads as a smudge rather than a distinct surface.
 * `#242424` is the smallest step that reads as deliberate.
 */
internal val Ink700 = Color(0xFF242424)

/** `--popover`. Highest neutral surface: menus, sheets, elevated chips. */
internal val Ink600 = Color(0xFF252525)

/**
 * Forced-dark `inverseSurface`, so M3's `Snackbar` renders as a dark chip
 * instead of the light one baseline M3 would paint.
 */
internal val Ink500 = Color(0xFF2E2E2E)

// ---------------------------------------------------------------------------
// Text
// ---------------------------------------------------------------------------

/** `--foreground`. 16.87:1 on `#111111`. */
internal val Snow100 = Color(0xFFF2F2F2)

/**
 * Deliberate uplift from the web's `rgba(242,242,242,0.45)`, which flattens to
 * `#767676` — 4.16:1 on the canvas and 3.75:1 on a card, both short of WCAG AA
 * for the uppercase field labels and helper text it is used for. `#8A8A8A` is
 * 5.47:1 on `#111111`, 4.94:1 on a card and 4.50:1 on `#242424`, so it clears
 * the bar on every surface in the ladder.
 */
internal val Grey500 = Color(0xFF8A8A8A)

/** Decorative or large text only — 3.54:1 on the canvas fails AA for body copy. */
internal val Grey600 = Color(0xFF6B6B6B)

/** Disabled foreground. 2.57:1, which is the point: disabled should read as off. */
internal val Grey700 = Color(0xFF565656)

/**
 * Label text on a stat tile.
 *
 * [Grey500] is the obvious choice and it fails: against the *lighter* end of a
 * tile gradient it measures 3.0–3.8:1. This is the dimmest neutral that clears
 * 4.5:1 on all four tiles (worst case 5.02:1 on amber) while still reading as
 * secondary next to the `#F2F2F2` figure.
 */
internal val Grey300 = Color(0xFFB4B4B4)

// ---------------------------------------------------------------------------
// Accent — one blue. Violet exists only as a gradient partner.
// ---------------------------------------------------------------------------

internal val Blue500 = Color(0xFF3B82F6) // --primary / --accent
internal val Blue600 = Color(0xFF2563EB) // --accent-hover, pressed state
internal val Blue400 = Color(0xFF60A5FA) // icon tone on tinted surfaces

internal val Violet500 = Color(0xFF8B5CF6)
internal val Violet400 = Color(0xFFA78BFA)

internal val Green500 = Color(0xFF10B981)
internal val Green400 = Color(0xFF34D399)

internal val Amber500 = Color(0xFFF59E0B)
internal val Amber400 = Color(0xFFFBBF24)

internal val Red500 = Color(0xFFEF4444) // --destructive
internal val Red400 = Color(0xFFF87171)

internal val White = Color(0xFFFFFFFF)

// ---------------------------------------------------------------------------
// Strokes and state layers — alpha is *correct* here: a hairline must pick up
// whatever surface it sits on, and state layers are by definition overlays.
// ---------------------------------------------------------------------------

/** `--border`: `rgba(255,255,255,0.08)`. The hairline that separates every step. */
internal val BorderSubtle = Color(0x14FFFFFF)

/** Secondary-button stroke: `rgba(255,255,255,0.20)`. */
internal val BorderStrong = Color(0x33FFFFFF)

/** `--ring`: `rgba(59,130,246,0.40)`. Focus only. */
internal val BorderFocus = Color(0x663B82F6)

/** Hover-equivalent (pressed on touch): `rgba(255,255,255,0.10)`. */
internal val StateLayerPressed = Color(0x1AFFFFFF)

// ---------------------------------------------------------------------------
// Stat-tile tints. Every value below is *generated*, not picked:
//
//   start  = flatten(base, 0.18)
//   end    = flatten(base, 0.30)
//   border = flatten(base, 0.45)
//   icon   = the Tailwind *-400 tone
//
// where flatten(base, a) = 17 + (channel - 17) * a, i.e. compositing the base
// colour at opacity `a` over #111111 and freezing the result.
//
// These land within a few 8-bit steps of a direct pixel sample of the web
// dashboard, but they do not match it exactly, and cannot: the web tiles stack
// two different Tailwind opacity utilities over a card, so no single alpha
// reproduces them. Generated values are the better trade — a new accent can be
// added by running the formula instead of by eye, and the whole set stays
// internally consistent.
// ---------------------------------------------------------------------------

internal val TileBlueStart = Color(0xFF19253A)
internal val TileBlueEnd = Color(0xFF1E3356)
internal val TileBlueBorder = Color(0xFF244478)

internal val TileGreenStart = Color(0xFF112F25)
internal val TileGreenEnd = Color(0xFF114333)
internal val TileGreenBorder = Color(0xFF115D43)

internal val TileVioletStart = Color(0xFF271F3A)
internal val TileVioletEnd = Color(0xFF362856)
internal val TileVioletBorder = Color(0xFF483378)

internal val TileAmberStart = Color(0xFF3A2A10)
internal val TileAmberEnd = Color(0xFF553B0F)
internal val TileAmberBorder = Color(0xFF78500E)

// ---------------------------------------------------------------------------
// Banner tints — translucent by design: a banner is an overlay on whatever
// surface hosts it, and its border must match.
// ---------------------------------------------------------------------------

internal val DangerTint = Color(0x1FEF4444)
internal val DangerBorder = Color(0x4DEF4444)
internal val WarningTint = Color(0x1FF59E0B)
internal val WarningBorder = Color(0x4DF59E0B)
internal val AccentTint = Color(0x1F3B82F6)
internal val AccentBorder = Color(0x4D3B82F6)

// ---------------------------------------------------------------------------
// Decorative — the grid + glow backdrop from the web platform's hero.
// ---------------------------------------------------------------------------

/**
 * Web is `rgba(99,102,241,0.03)`. Raised to 0.05 for mobile: at 0.03 the line
 * lands inside one 8-bit step of `#111111` and disappears entirely on OLED.
 * Documented divergence.
 */
internal val GridLine = Color(0x0D6366F1)

internal val GlowVioletInner = Color(0x148B5CF6) // rgba(139,92,246,0.08)
internal val GlowBlueMid = Color(0x0D3B82F6) // rgba(59,130,246,0.05)

// ---------------------------------------------------------------------------
// Fallback track palette. Used when `Track.color` is null or unparseable;
// indexed by `sortOrder`. Chosen to stay legible on `#111111`.
// ---------------------------------------------------------------------------

internal val TrackFallback = listOf(
    Blue400,
    Green400,
    Violet400,
    Amber400,
    Color(0xFF22D3EE), // cyan-400
    Color(0xFFF472B6), // pink-400
)

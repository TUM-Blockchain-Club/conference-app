# Token reference

Exhaustive values, the derivation formula, and the measured contrast audit.
Rationale for any decision here is in `docs/DESIGN.md`.

`ui/theme/Palette.kt` is the only file in the codebase permitted to hold a hex
literal. Everything else reads `MaterialTheme.colorScheme.*` or
`TbcTheme.tokens.*`.

---

## Derivation formula

```
flatten(base, alpha) = 17 + (channel - 17) * alpha
```

Composites `base` at opacity `alpha` over `#111111` (decimal 17) and freezes the
result to an opaque value. This is how every derived tint in the system was
produced. Use it rather than picking by eye — a colour that cannot be
regenerated cannot be reviewed.

```
raiseForDark(channel) = channel + (1 - channel) * 0.22
```

Lifts a mid-tone authored for a light background so it stays legible on
`#111111`. A lerp toward white: preserves hue, cannot overflow, and leaves
already-light colours nearly untouched. Applied to `Track.color` values out of
Supabase.

---

## Neutral ladder

| Name | Hex | M3 role | Use for |
|---|---|---|---|
| `Ink900` / `surfaceBase` | `#111111` | `background`, `surface`, `surfaceTint`, `scrim` | Screen canvas |
| `Ink800` / `surfaceCard` | `#1C1C1C` | `surfaceContainer`, `surfaceContainerLow` | Cards, rows, sheets |
| `Ink700` / `surfaceMuted` | `#242424` | `surfaceVariant`, `surfaceContainerHigh` | Inputs, inactive chips, bar tracks |
| `Ink600` / `surfaceRaised` | `#252525` | `surfaceContainerHighest`, `surfaceBright` | Popovers, menus |
| `Ink500` | `#2E2E2E` | `inverseSurface` | Snackbar (forced dark) |

`surfaceMuted` is `flatten(#FFFFFF, 0.08)`, not the web's literal 0.06 — that
gives `#1F1F1F`, only fourteen 8-bit steps above the canvas, which reads as a
smudge on OLED rather than a distinct surface.

---

## Text

| Name | Hex | Token | On `#111111` | On `#1C1C1C` | On `#242424` |
|---|---|---|---|---|---|
| `Snow100` | `#F2F2F2` | `textPrimary` | 16.87 | 15.22 | 13.87 |
| `Grey500` | `#8A8A8A` | `textMuted` | 5.47 | 4.94 | 4.50 |
| `Grey300` | `#B4B4B4` | `textOnTile` | — | — | — |
| `Grey600` | `#6B6B6B` | `textFaint` | **3.54** | **3.20** | — |
| `Grey700` | `#565656` | `textDisabled` | 2.57 | — | — |

`textFaint` fails AA for body text at every size the app uses it. It is
decorative-and-large-only, and it exists solely because it is the literal web
value; prefer `textMuted`.

`textOnTile` exists because `textMuted` measures only 3.0–3.8:1 against the
lighter end of a stat-tile gradient. See the tile table below.

---

## Accent and status

| Name | Hex | Token | On `#111111` |
|---|---|---|---|
| `Blue500` | `#3B82F6` | `accent`, `primary` | 5.13 |
| `Blue600` | `#2563EB` | `accentPressed`, `inversePrimary` | — |
| `Blue400` | `#60A5FA` | `accentSoft`, tile icon | — |
| `Violet500` | `#8B5CF6` | `accentSecondary`, `secondary` | 4.46 |
| `Violet400` | `#A78BFA` | tile icon | — |
| `Green500` | `#10B981` | `success`, `tertiary` | 7.44 |
| `Green400` | `#34D399` | tile icon | — |
| `Amber500` | `#F59E0B` | `warning` | 8.79 |
| `Amber400` | `#FBBF24` | tile icon | — |
| `Red500` | `#EF4444` | `danger`, `error` | 5.02 |
| `Red400` | `#F87171` | `onErrorContainer` | 6.83 |
| `White` | `#FFFFFF` | `onAccent` | on `#3B82F6`: **3.68** |

White on blue is below the 4.5:1 body-text bar. It clears the 3:1 UI-component
bar, so it is kept for brand parity under one rule: **filled blue carries icons
and short button labels only.** Badges use the outlined treatment — blue text on
`#111111` is 5.13:1.

---

## Strokes and state layers

Alpha is correct for these: a hairline has to adopt whatever surface sits behind
it, and a state layer is by definition an overlay.

| Name | Value | Token | From |
|---|---|---|---|
| `BorderSubtle` | `rgba(255,255,255,0.08)` | `borderSubtle`, `outlineVariant` | `--border` |
| `BorderStrong` | `rgba(255,255,255,0.20)` | `borderStrong`, `outline` | `.btn-secondary` |
| `BorderFocus` | `rgba(59,130,246,0.40)` | `borderFocus` | `--ring` |
| `StateLayerPressed` | `rgba(255,255,255,0.10)` | `statePressed` | `.btn-secondary:hover` |

---

## Stat tiles

Generated: `start = flatten(base, 0.18)`, `end = flatten(base, 0.30)`,
`border = flatten(base, 0.45)`, `icon` = the Tailwind `*-400` tone.

| Tile | base | start | end | border | icon | icon on end | `#F2F2F2` on end | `#B4B4B4` on end |
|---|---|---|---|---|---|---|---|---|
| blue | `#3B82F6` | `#19253A` | `#1E3356` | `#244478` | `#60A5FA` | 4.97 | 11.28 | 6.09 |
| green | `#10B981` | `#112F25` | `#114333` | `#115D43` | `#34D399` | 5.83 | 10.01 | 5.40 |
| violet | `#8B5CF6` | `#271F3A` | `#362856` | `#483378` | `#A78BFA` | 4.84 | 11.76 | 6.35 |
| amber | `#F59E0B` | `#3A2A10` | `#553B0F` | `#78500E` | `#FBBF24` | 6.23 | 9.29 | 5.02 |

For comparison, `textMuted` (`#8A8A8A`) on those same ends measures 3.66 / 3.25
/ 3.81 / 3.01 — which is why `textOnTile` exists.

These land within a few 8-bit steps of a pixel sample of the web dashboard but
do not match it exactly, and cannot: the web tiles stack two Tailwind opacity
utilities over a card, so no single alpha reproduces them.

---

## Banner tints

Translucent on purpose — a banner overlays cards, lists and the bare canvas, and
a tint flattened for one would be wrong on the other two.

| Token | Value |
|---|---|
| `accentTint` / `accentBorder` | `rgba(59,130,246,0.12)` / `0.30` |
| `warningTint` / `warningBorder` | `rgba(245,158,11,0.12)` / `0.30` |
| `dangerTint` / `dangerBorder` | `rgba(239,68,68,0.12)` / `0.30` |

---

## Decorative

| Token | Value | From |
|---|---|---|
| `gridLine` | `rgba(99,102,241,0.05)` | `.grid-pattern` (web: 0.03 — raised, see below) |
| `glowInner` | `rgba(139,92,246,0.08)` | `.grid-glow` |
| `glowMid` | `rgba(59,130,246,0.05)` | `.grid-glow` |

The grid line is raised from the web's 0.03: at that value the line falls within
one 8-bit step of `#111111` and vanishes entirely on OLED.

Grid cell is 30dp — the web's own `max-width: 640px` value, which a phone is
always inside.

---

## Track fallback palette

Used when `Track.color` is null or unparseable, indexed by `sortOrder.mod(6)`
(floor-mod, so zero and negative are safe).

`#60A5FA` · `#34D399` · `#A78BFA` · `#FBBF24` · `#22D3EE` · `#F472B6`

All Tailwind `*-400` tones, chosen to stay legible on `#111111` without needing
the dark uplift.

---

## Shape

Mirrors the web's single `--radius: 0.5rem` knob.

| Slot | Radius | Use for |
|---|---|---|
| `extraSmall` | 4dp | Swatches, tiny chips |
| `small` | 6dp | Inline controls |
| `medium` | **8dp** | Cards, inputs, tiles — the house default |
| `large` | 12dp | Sheets, large containers |
| `extraLarge` | 16dp | Full-bleed panels |
| `tokens.pill` | 50% | Buttons, tabs, badges, dots, bar tracks |

---

## Spacing

| Token | Value | Use for |
|---|---|---|
| `xxs` | 2dp | Optical nudges |
| `xs` | 4dp | Icon-to-label, dot-to-text |
| `sm` | 8dp | Inside a badge, tight row gaps |
| `md` | 12dp | Between list items |
| `lg` | 16dp | Default gap, card padding |
| `xl` | 20dp | Screen margin |
| `xxl` | 24dp | Between cards |
| `x3l` | 32dp | Between sections |
| `x4l` | 48dp | Above a footer, below a hero |
| `screenH` | 20dp | Screen horizontal margin |
| `cardPadding` | 16dp | Card interior |
| `touchTarget` | 48dp | Minimum tappable size |

---

## Motion

| Token | Duration | Use for |
|---|---|---|
| `fast` | 120ms | State layers, colour swaps |
| `standard` | 220ms | Default: expand, tab indicator, content swap |
| `slow` | 320ms | Screen transitions, bar fills |
| `entrance` | 800ms | The web's `fadeIn` / `scaleIn` |

Easings: `easeOut` = `cubic-bezier(0, 0, 0.2, 1)`, `easeStandard` =
`FastOutSlowIn`.

**Always** route through `motion.d(duration)`, which returns 0 when the platform
has requested reduced motion.

---

## Full contrast audit

WCAG 2.1 relative luminance. AA needs 4.5:1 for normal text, 3:1 for large text
(≥18.66sp bold or ≥24sp) and UI components.

| Foreground | Background | Ratio | Verdict |
|---|---|---|---|
| `#F2F2F2` | `#111111` | 16.87 | Pass |
| `#F2F2F2` | `#1C1C1C` | 15.22 | Pass |
| `#F2F2F2` | `#242424` | 13.87 | Pass |
| `#F2F2F2` | `#252525` | 13.69 | Pass |
| `#8A8A8A` | `#111111` | 5.47 | Pass |
| `#8A8A8A` | `#1C1C1C` | 4.94 | Pass |
| `#8A8A8A` | `#242424` | 4.50 | Pass (exactly at the line) |
| `#B4B4B4` | tile ends | 5.02–6.35 | Pass |
| `#3B82F6` | `#111111` | 5.13 | Pass |
| `#3B82F6` | `#1C1C1C` | 4.63 | Pass |
| `#10B981` | `#111111` | 7.44 | Pass |
| `#F59E0B` | `#111111` | 8.79 | Pass |
| `#EF4444` | `#111111` | 5.02 | Pass |
| `#F87171` | `#111111` | 6.83 | Pass |
| `#8B5CF6` | `#111111` | 4.46 | Pass for large/UI; not body text |
| `#60A5FA` | `#1E3356` | 4.97 | Pass |
| `#34D399` | `#114333` | 5.83 | Pass |
| `#A78BFA` | `#362856` | 4.84 | Pass |
| `#FBBF24` | `#553B0F` | 6.23 | Pass |
| `#6B6B6B` | `#111111` | 3.54 | **Fails AA** — decorative / large only |
| `#767676` (web value, not used) | `#111111` | 4.16 | **Fails AA** — why it was uplifted |
| `#FFFFFF` | `#3B82F6` | 3.68 | **Fails AA** — icons and short labels only |
| `#8A8A8A` | tile ends | 3.01–3.81 | **Fails AA** — use `textOnTile` instead |

Two failures ship deliberately (`textFaint`, white-on-blue); both are fenced by
an API rule rather than by good intentions. The other two rows are recorded so
nobody reintroduces them.

Recompute before changing any colour:

```js
const lum = h => {
  const c = [1,3,5].map(i => parseInt(h.slice(i,i+2),16)/255)
    .map(v => v <= 0.03928 ? v/12.92 : Math.pow((v+0.055)/1.055, 2.4));
  return 0.2126*c[0] + 0.7152*c[1] + 0.0722*c[2];
};
const contrast = (a,b) => {
  const [x,y] = [lum(a), lum(b)].sort((p,q) => q-p);
  return ((x+0.05)/(y+0.05)).toFixed(2);
};
```

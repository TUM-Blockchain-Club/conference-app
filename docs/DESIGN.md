# TBC Design System

The visual language of the TUM Blockchain Club conference app, and the reasoning
behind it.

This document is the *why*. The *what* — every token value, every contrast
ratio — lives in
[`.claude/skills/design-system/references/tokens.md`](../.claude/skills/design-system/references/tokens.md),
so there is one table with two readers rather than two tables that drift apart.
The *how* — what to reach for when writing a screen — lives in
[`.claude/skills/design-system/SKILL.md`](../.claude/skills/design-system/SKILL.md).

---

## Scope

Covers the shared Compose Multiplatform UI in
`shared/src/commonMain/kotlin/com/conference/asmara/ui/`, plus the two host
shells (`androidApp`, `iosApp`) insofar as they paint pixels before Compose
does.

The system is **dark-only**, and that is a decision rather than an omission. The
source visual language is a pure-neutral dark dashboard; a light variant is not
an inversion of it but a second design with its own surface ladder and its own
contrast audit. Building one is a project, not a flag.

The Kotlin package path stays `com.conference.asmara` — `asmara` is the existing
Gradle module and package name. The design system itself is TBC-branded
throughout: `TbcTheme`, `TbcTokens`, `TbcCard`.

---

## Provenance

Everything here derives from the TBC **web membership platform** — the club's
internal dashboard — as it stood on **2026-08-15**.

Two sources:

1. The platform's Tailwind v4 `global.css`, whose `:root` block is the
   authoritative token list:

   ```css
   :root {
     /* Pure neutral dark — no colour cast */
     --background: #111111;
     --foreground: #f2f2f2;
     --card: #1c1c1c;
     --card-foreground: #f2f2f2;
     --popover: #252525;
     --popover-foreground: #f2f2f2;
     --primary: #3b82f6;
     --primary-foreground: #ffffff;
     --secondary: rgba(255, 255, 255, 0.06);
     --secondary-foreground: #f2f2f2;
     --muted: rgba(255, 255, 255, 0.06);
     --muted-foreground: rgba(242, 242, 242, 0.45);
     --accent: #3b82f6;
     --accent-foreground: #ffffff;
     --accent-hover: #2563eb;
     --destructive: #ef4444;
     --destructive-foreground: #ffffff;
     --border: rgba(255, 255, 255, 0.08);
     --input: rgba(255, 255, 255, 0.07);
     --ring: rgba(59, 130, 246, 0.4);
     --radius: 0.5rem;
   }
   ```

   Plus the `.btn-primary` / `.btn-secondary` / `.grid-pattern` / `.grid-glow`
   / `.text-gradient` rules and the `fadeIn` / `scaleIn` / `gridMove` keyframes.

2. Six screenshots of the running platform, dated 2026-08-15: the profile page,
   the events grid, the statistics dashboard, and three others. Referenced
   per-component in the catalogue below.

Anyone diffing app against web in six months should start here: if the web
`:root` has moved, this document is stale, and the token table is the thing to
re-derive.

---

## Principles

**1. Pure neutral dark, no colour cast.** `#111111` is the canvas. The neutral
*tokens* have identical R, G and B channels, and the CSS says so in a comment.
Any blue creeping into a grey is a bug, and it has a specific usual cause — see
`surfaceTint` below.

The one sanctioned exception is the optional decorative backdrop
(`TbcScaffold(decorated = true)`), whose radial glow lifts the canvas to about
`#13171D` at its strongest. That is faithful to the source — the web hero page
measures `#151721` in the same region — and it is why the surface ladder must be
audited on an *undecorated* screen.

**2. Depth is a surface step plus a hairline, never a shadow.** `#111111` →
`#1C1C1C` → `#242424` → `#252525`, each separated by a `rgba(255,255,255,0.08)`
stroke. Beyond taste, this is a Compose constraint: shadows draw *behind* the
shape, so anything translucent shows its own shadow through it as a smudge —
noticeably worse on iOS/Skia. And on `#111111` a black shadow has nowhere to go
tonally; it reads as dirt.

**3. One accent.** Blue `#3B82F6`. Violet `#8B5CF6` exists only as the far end
of a gradient, never as a standalone fill. Green and amber are status, not
brand.

**4. Motion is decorative.** No animation in this system carries information
that layout and colour do not already carry. That is what makes honouring
reduce-motion free rather than a trade-off.

---

## Colour

### The raw palette

See [`references/tokens.md`](../.claude/skills/design-system/references/tokens.md)
for the full table with hex values, contrast ratios and usage notes.
`ui/theme/Palette.kt` is the only file in the codebase permitted to contain a
hex literal.

### The token split

One question decides where a colour lives: **does a stock Material 3 component
read this role?**

- **Yes → `ColorScheme`.** Even roles the web has no equivalent for must be
  filled. Leave `surfaceVariant` or `inverseSurface` at their defaults and the
  first stock `TextField` or `Snackbar` anyone drops on a screen renders in
  baseline M3 purple.
- **No → `TbcTokens`,** provided via `staticCompositionLocalOf`. Static rather
  than dynamic because the app is dark-only and these values never change at
  runtime, so read-tracking would be pure overhead. Its default is
  `error("Wrap your content in TbcTheme")`: a stub default would let a screen
  render *almost* right outside the theme and hide the missing wrapper until
  someone noticed by eye.
- **Spacing and motion → their own locals,** so changing a spacing value does
  not invalidate every colour reader in the tree.

### The web → Material 3 mapping

| Web token | M3 role | Value | Why |
|---|---|---|---|
| `--background` | `background`, `surface` | `#111111` | **See note 1.** |
| `--foreground` | `onBackground`, `onSurface` | `#F2F2F2` | 16.87:1. |
| `--card` | `surfaceContainer`, `surfaceContainerLow` | `#1C1C1C` | Cards read this, not `surface`. |
| `--muted` / `--secondary` / `--input` | `surfaceVariant` | `#242424` | Flattened. **See note 4.** |
| `--muted-foreground` | `onSurfaceVariant` | `#8A8A8A` | Uplifted. **See note 5.** |
| `--popover` | `surfaceContainerHighest`, `surfaceBright` | `#252525` | Menus, sheets. |
| `--primary` / `--accent` | `primary` | `#3B82F6` | |
| `--primary-foreground` | `onPrimary` | `#FFFFFF` | 3.68:1. **See note 6.** |
| `--accent-hover` | `inversePrimary` | `#2563EB` | Pressed state on touch. |
| `--destructive` | `error` | `#EF4444` | |
| `--border` | `outlineVariant` | `rgba(255,255,255,0.08)` | The hairline. |
| (20% stroke, from `.btn-secondary`) | `outline` | `rgba(255,255,255,0.20)` | |
| `--ring` | (via `TbcTokens.borderFocus`) | `rgba(59,130,246,0.40)` | |
| — | `surfaceTint` | `#111111` | **See note 2.** |
| — | `inverseSurface` | `#2E2E2E` | **See note 3.** |
| `#8B5CF6` (gradient partner) | `secondary` | `#8B5CF6` | Mapped so stock components land somewhere sane; no TBC component fills with it. |

#### Note 1 — `surface` is `#111111`, not `#1C1C1C`

The intuitive mapping is `--card` → `surface`. It is wrong, and the failure is
silent. Material 3's `Scaffold` takes its `containerColor` from
`colorScheme.surface`, not `colorScheme.background`. Map `surface` to the card
colour and every screen background quietly becomes `#1C1C1C` — a whole app one
step off, with nothing to point at. Cards read `surfaceContainer` instead.

#### Note 2 — `surfaceTint` is `#111111`

M3 renders an elevated surface as:

```
surfaceTint.copy(alpha = f(elevation)).compositeOver(containerColor)
```

`surfaceTint` defaults to `primary`. Left alone, **every elevated surface in the
app picks up a blue cast** — a direct violation of principle 1, and one that
looks like a rendering artefact rather than a configuration mistake. Pinning the
tint to the base colour makes the formula a no-op. Belt and braces: every TBC
component also passes `tonalElevation = 0.dp`.

#### Note 3 — `inverseSurface` is forced dark

An intentional deviation from M3 semantics. `inverseSurface` is *supposed* to be
the light counterpart of `surface`, which is precisely what makes a stock
`Snackbar` appear as a bright white chip over a `#111111` app. Forced to
`#2E2E2E`.

#### Note 4 — alpha for strokes, opaque for fills

The web expresses its muted surfaces as `rgba(255,255,255,0.06)`. Transcribing
that literally is the single highest-yield mistake available here, for three
Compose-specific reasons:

1. **The same token yields different colours.** `rgba(255,255,255,0.06)` over
   `#111111` is `#1F1F1F`; over a `#1C1C1C` card it is `#2A2A2A`. One token,
   three greys in one screenshot. Flattened to one opaque `#242424`.
2. **Shadows show through.** Compose draws shadows behind the shape. A
   translucent container reveals its own shadow as a grey smudge — worse on
   iOS/Skia.
3. **Adjacent translucent pills seam.** Two overlapping antialiased edges
   composite twice and leave a visibly brighter line. This shows up immediately
   in dense stat rows.

Alpha **stays** for: `borderSubtle` / `borderStrong` / `borderFocus`, pressed
state layers, banner tints (a banner genuinely overlays three different
surfaces), and runtime track tints, where no flatten can be precomputed for an
arbitrary hex out of the database.

#### Note 5 — the `--muted-foreground` uplift

`rgba(242,242,242,0.45)` flattens to `#767676`: **4.16:1 on the canvas, 3.75:1
on a card.** Both fail WCAG AA, and this token is used for the uppercase field
labels and all helper text — 11–14sp, nowhere near large enough for the relaxed
3:1 bar.

The app uses `#8A8A8A` instead: 5.47:1 on `#111111`, 4.94:1 on a card, 4.50:1 on
`#242424`. Passes everywhere in the ladder. `#6B6B6B` is retained as `textFaint`
for decorative and large-bold text only.

#### Note 6 — white on blue, and what follows from it

White on `#3B82F6` is **3.68:1**. That clears the 3:1 bar for UI components but
not the 4.5:1 bar for normal text.

The colour is kept for brand parity, and the consequence is written into the
component API rather than left to judgement:

- **Filled blue carries icons and short button labels only.** `TbcButton`'s
  primary style and the selected `PillTabRow` pill qualify — both are short, and
  both signal state through fill and position as well as text.
- **Badges use the outlined treatment.** Blue text on `#111111` is 5.13:1. This
  is what the platform's own "Core Member" badge does, so parity and
  accessibility agree here. There is deliberately no filled-blue badge in the
  system.

### Derivation formula

New tints are generated, not eyeballed:

```
flatten(base, alpha) = 17 + (channel - 17) * alpha        // composite over #111111
```

Stat tiles evaluate it at `0.18` (gradient start), `0.30` (gradient end) and
`0.45` (border); the icon takes the Tailwind `*-400` tone, the only step light
enough to read as an accent rather than as more background.

| Tile | base | start | end | border | icon |
|---|---|---|---|---|---|
| blue | `#3B82F6` | `#19253A` | `#1E3356` | `#244478` | `#60A5FA` |
| green | `#10B981` | `#112F25` | `#114333` | `#115D43` | `#34D399` |
| violet | `#8B5CF6` | `#271F3A` | `#362856` | `#483378` | `#A78BFA` |
| amber | `#F59E0B` | `#3A2A10` | `#553B0F` | `#78500E` | `#FBBF24` |

These land within a few 8-bit steps of a direct pixel sample of the web
dashboard, but they do not match it exactly — and cannot. The web tiles stack
two different Tailwind opacity utilities over a card, so no single alpha
reproduces them. Generated values are the better trade: a fifth accent can be
added by running the formula rather than by eye, and the set stays internally
consistent.

### Contrast audit

Computed with the WCAG 2.1 relative-luminance formula. Full table in
[`references/tokens.md`](../.claude/skills/design-system/references/tokens.md).

| Foreground | Background | Ratio | Verdict |
|---|---|---|---|
| `#F2F2F2` | `#111111` | 16.87:1 | Pass |
| `#F2F2F2` | `#1C1C1C` | 15.22:1 | Pass |
| `#8A8A8A` | `#111111` | 5.47:1 | Pass |
| `#8A8A8A` | `#1C1C1C` | 4.94:1 | Pass |
| `#8A8A8A` | `#242424` | 4.50:1 | Pass (at the line) |
| `#3B82F6` | `#111111` | 5.13:1 | Pass |
| `#10B981` | `#111111` | 7.44:1 | Pass |
| `#F59E0B` | `#111111` | 8.79:1 | Pass |
| `#EF4444` | `#111111` | 5.02:1 | Pass |
| `#B4B4B4` | tile ends | 5.02–6.35:1 | Pass |
| `#60A5FA` | `#1E3356` | 4.97:1 | Pass |
| `#6B6B6B` | `#111111` | 3.54:1 | **Documented failure** — decorative / large only |
| `#FFFFFF` | `#3B82F6` | 3.68:1 | **Documented failure** — short labels and icons only |

Two failures, both deliberate and both fenced by an API rule rather than by good
intentions. Everything else clears AA.

One near-miss worth recording, because it was found by measuring rather than by
looking: `textMuted` on a stat tile measures **3.0–3.8:1** against the lighter
end of the gradient. Stat tiles therefore have their own secondary tone,
`textOnTile` = `#B4B4B4` (worst case 5.02:1, on amber).

---

## Typography

**Geist**, bundled as static TTFs — regular, medium, semibold, bold, plus Geist
Mono regular (~510 KB). SIL OFL 1.1; license at `third_party/geist/OFL.txt`.

Three constraints shaped this:

- **Static, not variable.** Skia's `FontVariation` support is uneven across
  targets and `minSdk 26` sits right at the Android boundary where it stops
  being dependable.
- **Filenames must be `[a-z0-9_]`.** They become Kotlin identifiers;
  `Geist-Regular.ttf` fails codegen outright.
- **The license file must not live in `composeResources/font/`.** The generator
  maps *every* file in that directory to a `FontResource`, and a stray `.txt`
  breaks the build.

### The `@Composable Font()` constraint

`org.jetbrains.compose.resources.Font(...)` is `@Composable`. That cascades: a
`FontFamily` cannot be a top-level `val`, so neither can a `Typography`, and
neither can be built inside `remember { }` (no composable calls in a remember
lambda). The standard two-layer shape:

```kotlin
@Composable
internal fun rememberTbcTypography(): Typography {
    val sans = TbcFonts.sans()                    // composable call, hoisted
    return remember(sans) { tbcTypography(sans) } // plain fun: cacheable, testable
}
```

`Typography` holds exactly one family, so the mono face cannot live in it. It
ships as `TbcTextStyles` (`monoSmall`, `monoLabel`) alongside the tokens.

### The scale

Compressed for mobile. The source is a dashboard at 1440px+; transcribing its
sizes to a 390pt phone would leave a 36px page title eating a fifth of the
viewport. Headlines come down hardest; body text barely moves.

| Slot | Size / weight | Used for |
|---|---|---|
| `displaySmall` | 30 Bold | Stat-tile figures |
| `headlineLarge` | 28 Bold | Screen title (web: 36px) |
| `headlineSmall` | 20 SemiBold | Section headers |
| `titleLarge` | 18 SemiBold | Card headers |
| `titleSmall` | 14 Medium | Names, inline emphasis |
| `bodyLarge` | 16 Regular | Default body |
| `bodyMedium` | 14 Regular | Dense body, list rows |
| `bodySmall` | 12 Regular | Helper text, footer |
| `labelLarge` | **15** Medium | Buttons |
| `labelMedium` | 13 Medium | Tabs, badges |
| `labelSmall` | 11 Medium, 0.9sp tracking | Uppercase field labels |

Two departures from stock M3: `labelLarge` is 15sp rather than 14, because it is
the button label — the loudest control on the screen, set at 16px/500 on the web
— and M3's 14 reads cheap next to a 48dp pill. `labelSmall` carries 0.9sp
tracking because it is only ever rendered uppercase, where default tracking lets
letters collide.

There is deliberately **no global `ProvideTextStyle`**. It would silently
reshape every stock M3 component that reads `LocalTextStyle`, which is a far
wider blast radius than it appears.

**Known caveat:** on iOS/Skia the first frame may paint `FontFamily.Default` and
swap to Geist. Accepted — one frame, cold start only. `preloadFont` plus a
splash gate is the fix if it ever becomes objectionable. `TbcTheme` must never
block on font loading.

---

## Shape, spacing, layout

**Shape** mirrors the web's single `--radius: 0.5rem` knob: 4 / 6 / 8 / 12dp
mapped to `extraSmall` … `large`, with `medium` (8dp) as the house default so a
stray stock component lands on the right radius by accident. Fully-rounded pills
live in `TbcTheme.tokens.pill`; `Shapes` has no slot for them.

**Spacing** is a 4dp ramp with two half-steps at the small end where mobile
density needs them, plus three named constants that let a screen read as intent
rather than as arithmetic: `screenH` (20dp), `cardPadding` (16dp), `touchTarget`
(48dp).

**Layout**: one screen title, sections separated by `x3l` (32dp), cards by `xxl`
(24dp), list rows by `md` (12dp).

---

## Motion

Durations: `fast` 120ms (state layers), `standard` 220ms (the default),
`slow` 320ms (screen transitions, bar fills), `entrance` 800ms (the web's
`fadeIn`).

Every duration goes through `motion.d(...)`, which returns zero when the
platform has asked for reduced motion:

```kotlin
val motion = TbcTheme.motion
animateFloatAsState(target, tween(motion.d(motion.standard)))
```

The signal differs per platform. iOS has a real switch
(`UIAccessibilityIsReduceMotionEnabled`). Android has no single "reduce motion"
setting; the closest available is the global animator duration scale, which is
what both Developer Options and the accessibility "Remove animations" toggle
write to. `reduceMotionEnabled()` is `@Composable` because the Android
implementation needs `LocalContext` — which is why `TbcTheme` must sit *inside*
whatever provides that context.

---

## Component catalogue

Source lives in `ui/components/`. Copy-pasteable implementations are in
[`references/components.md`](../.claude/skills/design-system/references/components.md);
this section is anatomy and provenance only.

| Component | Anatomy | From |
|---|---|---|
| `TbcScaffold` | `#111111` canvas + optional grid/glow + `safeDrawing` insets | Every screenshot |
| `TbcCard` | `#1C1C1C`, 8dp, hairline, `tonalElevation = 0` | Profile, statistics cards |
| `TbcButton` | Primary pill (135° blue→violet + accent glow), Secondary (transparent + 20% stroke), Tonal (`#242424`) | `.btn-primary` / `.btn-secondary` |
| `TbcBadge` | Outlined; accent, neutral, success, warning, danger | "Core Member", "IT & Development" |
| `CountPill` | Muted text on `#242424`, pill | "7 departments", "132 total" |
| `PillTabRow` | Scrollable pills; selected = solid blue + icon; edge fade | Dashboard nav |
| `FieldLabel` | Uppercase, 0.9sp tracking, muted, optional padlock | Profile form |
| `TbcTextField` | Label above, opaque `#242424` container, 8dp | Profile form |
| `StatTile` | Derived gradient + hairline + `*-400` icon + figure | Statistics tiles |
| `StatBarRow` | Dot + label + fixed-width rounded track + count/pct | Department Distribution |
| `Banner` | Tinted overlay + hue-matched hairline + distinct glyph per severity | Error states |
| `SectionHeader` / `ScreenTitle` / `ScreenFooter` | Title + subtitle + optional count pill | Dashboard headers, footer |

**The rule:** if no component fits, build one *here*, from tokens. Never inline a
bespoke styled `Box` in a screen file. A one-off in a screen is how a design
system dies — nobody ever finds it again to fix.

### Icons

The set is **Lucide geometry, transcribed as `ImageVector`s** in
`ui/icons/TbcIcons.kt`. 24×24 outlines, 2px stroke, round caps and joins.

This started as a plan to use `Icons.Filled.*` from `material-icons-core`, on
the assumption that `compose.material3` supplies it transitively. **It does
not** — not in Compose Multiplatform 1.11.1 — and the artifact is frozen at
**1.7.3**: JetBrains stopped publishing `material-icons-core` and `-extended`
after that release. Adding it would pin a 1.7.3 icon artifact against a 1.11.1
Compose stack, which is exactly the version skew that makes
`compose.materialIconsExtended` a trap in this repo.

Transcribing Lucide costs one file, no dependency and no skew — and it *removes*
a divergence instead of adding one, since Lucide is what the web platform draws.
Material's filled glyphs would have been a visible mismatch beside the reference
screenshots.

To add an icon: copy the `d` attributes from the Lucide SVG (lucide.dev, ISC) and
pass them to the file's `lucide()` helper. Do not redraw by hand, and do not mix
in a filled glyph from another set — the uniform stroke weight is what holds the
set together.

---

## Mobile adaptations of desktop patterns

The source is a desktop dashboard. These are the translations, and the reasoning
matters more than the rules, because new screens will need new ones.

| Desktop | Mobile | Why |
|---|---|---|
| 4-across stat grid | 2×2 | Four across gives each tile ~90dp — not enough for a two-word label beside a three-digit figure. |
| Full-width tab bar, 6 tabs | Horizontally scrolling `PillTabRow` with edge fade | Two or three pills fit. A cropped pill reads as a bug; a fade reads as an affordance. |
| Two-column form | Single column | Two 170dp fields are unusable, and label + input + helper text needs the width. |
| Wide table rows | Stacked cards | Horizontal scrolling to read a row is worse than any vertical alternative. |
| Hover states | Pressed / ripple | Hover does not exist. `TbcButtonStyle.Tonal` exists because the desktop uses hover for the same job. |
| Pinned footer | Last item in the scroll | A docked footer costs vertical space on every screen for information nobody reads twice. |
| 32px button padding | 24dp horizontal, vertical retained | 32dp insets make two side-by-side pills impossible at 390dp; vertical stays because the 48dp touch target depends on it. |
| 50px grid cell | 30dp | The web itself does this under `max-width: 640px`. |

---

## Data-driven colour

`Track.color` is a nullable free-text string owned by whoever edits the schedule
in Supabase, which makes `ui/theme/TrackColor.kt` the one place in the theme that
has to be defensive. It solves three problems:

1. **Format drift.** `#RGB`, `#RRGGBB`, `#RRGGBBAA`, with or without `#`, either
   case. Anything else returns `null` rather than throwing on user data.
2. **Wrong tonal range.** The seeded values (`#4F46E5`, `#DC2626`, `#059669`)
   are Tailwind `*-600` mid-tones authored for white backgrounds; on `#111111`
   they read muddy. `raiseForDark(c) = c + (1 - c) * 0.22` per channel lifts
   them. It is a lerp toward white, so it preserves hue, cannot overflow, and
   leaves already-light colours nearly untouched — where a multiply would darken
   and an additive constant would clip and skew.
3. **Absence.** Falls back to a six-colour palette indexed by
   `sortOrder.mod(6)` — floor-mod, so a zero or negative sort order is safe.

All of it is pure and non-`@Composable`, which is what makes it testable:
`commonTest/.../TrackColorTest.kt` covers null, empty, both hex forms, the alpha
form, invalid characters, wrong lengths, the uplift formula, and fallback
wraparound including negative indices.

`EventType` colours are ours rather than the database's, so they come straight
from the palette. Breaks stay neutral so they recede in a schedule list.

---

## Consolidated list of deliberate divergences

Everything below departs from either the web source or Material 3 defaults, on
purpose:

1. **`--muted-foreground` uplifted** `#767676` → `#8A8A8A`. The web value fails
   WCAG AA (4.16:1) at the sizes it is used.
2. **Translucent fills flattened to opaque.** `rgba(255,255,255,0.06)` →
   `#242424`. Alpha retained for strokes, state layers, banner tints and runtime
   track colours.
3. **`surfaceMuted` is `flatten(white, 0.08)`, not the literal 0.06.** At 0.06
   the step is `#1F1F1F` — fourteen 8-bit steps above the canvas, which reads as
   a smudge on OLED rather than as a surface.
4. **`inverseSurface` forced dark** (`#2E2E2E`), deviating from M3 semantics, so
   `Snackbar` is not a white chip.
5. **`surfaceTint` pinned to `#111111`** rather than `primary`, to stop M3
   tinting elevated surfaces blue.
6. **`labelLarge` is 15sp**, not M3's 14 and not the web's 16px.
7. **Type scale compressed** across the board for a phone viewport.
8. **Grid lines raised** from `rgba(99,102,241,0.03)` to `0.05`. At 0.03 the line
   falls within one 8-bit step of `#111111` and disappears on OLED.
9. **Stat-tile tints generated by formula**, so they differ by a few steps from a
   pixel sample of the web.
10. **`textOnTile` (`#B4B4B4`) added** — a token with no web equivalent, because
    the muted tone fails AA on tile gradients.
11. **`TbcButtonStyle.Tonal` added** — no web equivalent; it replaces hover
    affordances that do not exist on touch.
12. **Icons are Lucide-transcribed vectors**, not Material and not the `lucide`
    npm package.
13. **Footer scrolls** rather than being pinned.

---

## Host shells

Both platforms paint pixels before Compose runs, and both default to something
wrong for a dark-only app. These are the least visible and most reported-as-bugs
parts of the system.

**Android.**
- **`androidResources.enable = true` on the shared module's `android { }` target
  is what makes the fonts ship.** Without it the APK contains *zero*
  `composeResources` assets and `Font()` falls back to Roboto — silently, with
  no warning and no crash. The cause is a gap between Compose Multiplatform
  1.11.1 and AGP's newer `com.android.kotlin.multiplatform.library` plugin: the
  Compose plugin registers a
  `copyAndroidMainComposeResourcesToAndroidAssets` task but never configures its
  `outputDirectory`, because the variant hook it expects is not there. Enabling
  Android resources on the target restores the hook.
  Verify with `unzip -l androidApp/build/outputs/apk/debug/androidApp-debug.apk | grep geist`
  — five TTFs, or something is wrong.
- `res/values/colors.xml` duplicates `#111111` as `tbc_background`. It has to
  exist twice — the framework cannot read a Kotlin constant. A mismatch shows up
  as a one-frame flash of the old colour.
- `themes.xml` uses `android:Theme.Material.NoActionBar`, not
  `Theme.AppCompat.DayNight.NoActionBar`. `DayNight` follows the *system* theme,
  so on a phone in light mode the pre-Compose window background is **white** — a
  full-screen flash at every cold start, and invisible unless you test with the
  system theme set to light. It also dropped the AppCompat dependency, which
  `MainActivity` never needed: it extends `ComponentActivity`.
- `MainActivity` forces `SystemBarStyle.dark(TRANSPARENT)` on both bars. Bare
  `enableEdgeToEdge()` uses `SystemBarStyle.auto`, which follows the system theme
  and yields dark, invisible status icons on a light-mode phone.
- `androidApp` no longer declares `androidx-compose-bom` + `ui` + `material3`.
  Those publish to the same `androidx.compose.*` coordinates that Compose
  Multiplatform's Android artifacts do, so alongside `project(":shared")` they
  put material3 on the classpath at two pinned versions — the classic "works on
  iOS, subtly wrong on Android".

**iOS.**
- `ContentView.swift` sets `.preferredColorScheme(.dark)`. Compose does not
  control the iOS status bar; without this, a light-mode phone gets dark status
  text over `#111111`.
- `Info.plist` names a `LaunchBackground` colorset (`#111111` in *both*
  appearances) under `UILaunchScreen`. An empty dict means "system default",
  which is a white launch flash.
- `ContentView.swift` must apply `.ignoresSafeArea()` as well. Without it
  SwiftUI insets the Compose view to the safe area and the uncovered strip under
  the status bar renders black rather than `#111111`.
- `TbcScaffold` applies `WindowInsets.safeDrawing`, without which content slides
  under the notch and the home indicator.
- `OTHER_LDFLAGS` carries `-lsqlite3`. The SQLDelight native driver links
  against the system SQLite, and without the flag the app fails to link with a
  wall of undefined `_sqlite3_*` symbols. This was a pre-existing break — the
  iOS target had not linked since SQLDelight was introduced — fixed here so the
  design system could actually be verified on device.

---

## How to extend

1. **Need a colour?** Check the tokens first. If it genuinely does not exist,
   derive it with `flatten(base, alpha)`, add it to `Palette.kt` *and*
   `Tokens.kt`, and add a row to `references/tokens.md` with its measured
   contrast. Never inline `Color(0xFF…)` in a component or screen.
2. **Need a component?** Build it in `ui/components/` from tokens, add it to the
   catalogue above and to `references/components.md`, and render it in
   `GalleryScreen`.
3. **Need an icon?** Copy the path data from lucide.dev into `TbcIcons.kt`.
4. **Changed a token?** Re-run the contrast numbers before committing. The two
   documented failures are the only two allowed.

### Verifying

```bash
./gradlew :shared:generateComposeResClass      # catches font filename errors
./gradlew :shared:compileAndroidMain
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :shared:testAndroidHostTest          # includes TrackColorTest
make android-run   # then set the system theme to LIGHT and relaunch
make ios-run
```

Note the task names: this project uses AGP's KMP-library plugin, so it is
`:shared:compileAndroidMain`, **not** `:shared:compileDebugKotlinAndroid`.

Then open Home → Design System on both and check, in rough order of how often
each actually breaks:

1. **Android with the system theme set to LIGHT.** The number-one regression.
   Status-bar icons stay light, no white cold-start flash, nothing turns white.
   A failure points at `themes.xml` or the `enableEdgeToEdge` style.
2. **Font parity.** The gallery puts a Geist sample directly above a
   `FontFamily.Default` sample. They must look *different*. If they look
   identical on one platform and different on the other, the identical one is
   not packaging fonts — and this fails silently via fallback, so nothing else
   will tell you.
3. **iOS cold start.** No white launch flash, light status bar.
4. **Surface ladder.** Colour-pick `#111111` / `#1C1C1C` / `#242424` / `#252525`
   from a gallery screenshot and confirm R == G == B. Any blue means
   `surfaceTint` has come loose. Read it off the gallery, which renders
   undecorated on purpose — on a `decorated = true` screen the glow tints the
   canvas toward `#13171D` by design and the measurement is meaningless.
5. **Safe areas.** Clear of the notch and the home indicator.
6. **Reduced motion.** Android: Developer Options → Animator duration scale off.
   iOS: Accessibility → Motion → Reduce Motion. Animations should be instant,
   not janky.
7. **Text scaling.** Largest system font size on both. Pill buttons and
   uppercase field labels break first.

---

## Attribution

- **Geist** and **Geist Mono** — Vercel, in collaboration with basement.studio.
  SIL Open Font License 1.1. `third_party/geist/OFL.txt`.
- **Lucide** icon geometry — ISC License. Transcribed, not vendored.
- Colour values and layout patterns derive from the TBC web membership platform.

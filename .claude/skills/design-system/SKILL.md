---
name: design-system
description: >-
  The TBC design system — tokens, components and hard rules for the conference
  app's Compose UI. Load this BEFORE writing or editing any Composable under
  `ui/` or `navigation/`; before adding a card, button, badge, tab, input,
  banner or stat tile; before choosing any colour, font size, radius or spacing
  value; before writing a `Color(0x…)`, `.sp` or `.dp` literal; for any layout,
  screen-scaffolding or animation work; and for any request mentioning design,
  theme, style, dark mode, branding, or "match the web platform".
---

# TBC Design System

Dark-only. Pure neutral `#111111`. One blue accent. Depth by surface step and
hairline, never shadow.

Everything below is executable — the tokens and components exist in
`shared/src/commonMain/kotlin/com/conference/asmara/ui/`. Use them; do not
re-derive them.

- Rationale for any rule here: `docs/DESIGN.md`
- Exhaustive token table + contrast audit: `references/tokens.md`
- Copy-pasteable component source: **load `references/components.md` when you
  need an exact implementation** — do not guess a component's internals

---

## Hard rules

| Never | Always |
|---|---|
| `Color(0xFF…)` in a component or screen | `MaterialTheme.colorScheme.*` / `TbcTheme.tokens.*` |
| `16.dp` layout literals | `TbcTheme.spacing.lg` |
| `fontSize = 14.sp` inline | `MaterialTheme.typography.bodyMedium` |
| `RoundedCornerShape(8.dp)` | `MaterialTheme.shapes.medium` / `tokens.pill` |
| `tween(300)` | `tween(motion.d(motion.standard))` |
| `Modifier.shadow` for depth | surface step + `borderSubtle` hairline |
| `tonalElevation > 0` | `tonalElevation = 0.dp`, always |
| Stock `Card` / `Button` / `Chip` / `TabRow` / `Scaffold` | the `Tbc*` equivalent |
| Translucent **fills** | opaque `surfaceCard` / `surfaceMuted`; alpha only for strokes and state layers |
| Ad-hoc new colours | derive with `flatten(base, α)`, add to `Palette.kt` **and** `Tokens.kt` **and** the docs |
| Long text on filled blue | outlined blue-on-dark badge |
| `Icons.Filled.*` / `material-icons-*` | `TbcIcons.*` (Lucide; the Material artifact is frozen at 1.7.3) |
| A bespoke styled `Box` in a screen file | a new component in `ui/components/` |

The last one matters most. A one-off inside a screen is invisible to everyone
who comes later, which is how a design system quietly stops being one.

---

## Token cheat-sheet

Read these off `TbcTheme.tokens` — you should never need to open `Palette.kt`.

**Surfaces** (the ladder; each step separated by a hairline)

| Token | Hex | Use for |
|---|---|---|
| `surfaceBase` | `#111111` | Screen canvas |
| `surfaceCard` | `#1C1C1C` | Cards, list rows, sheets |
| `surfaceMuted` | `#242424` | Inputs, inactive chips, bar tracks |
| `surfaceRaised` | `#252525` | Popovers, menus |

**Text**

| Token | Hex | Use for |
|---|---|---|
| `textPrimary` | `#F2F2F2` | Titles, values, body |
| `textMuted` | `#8A8A8A` | Labels, helper text, secondary copy |
| `textFaint` | `#6B6B6B` | Decorative / large only — **3.54:1, fails AA for body** |
| `textDisabled` | `#565656` | Disabled controls |
| `textOnTile` | `#B4B4B4` | Secondary text **on a stat tile** (muted fails there) |

**Strokes** (alpha is correct here — a hairline must adopt its background)

| Token | Value | Use for |
|---|---|---|
| `borderSubtle` | 8% white | Every surface-step separation |
| `borderStrong` | 20% white | Secondary button outline |
| `borderFocus` | 40% blue | Focus rings |

**Accent and status**

| Token | Hex | Notes |
|---|---|---|
| `accent` | `#3B82F6` | The one accent |
| `accentPressed` | `#2563EB` | Pressed |
| `accentSecondary` | `#8B5CF6` | **Gradient partner only** — never a standalone fill |
| `success` / `warning` / `danger` | `#10B981` / `#F59E0B` / `#EF4444` | |
| `stats.blue/green/violet/amber` | — | `StatAccent(start, end, border, icon, base)` |

**Other:** `pill` (fully rounded shape), `gridLine` / `glowInner` / `glowMid`
(decorative backdrop), `dangerTint` / `warningTint` / `accentTint` (+ matching
`*Border`) for banners.

---

## Type cheat-sheet

| Style | Size | Use for |
|---|---|---|
| `displaySmall` | 30 Bold | Stat-tile figures |
| `headlineLarge` | 28 Bold | Screen title (one per screen) |
| `headlineSmall` | 20 SemiBold | Section headers |
| `titleLarge` | 18 SemiBold | Card headers |
| `titleSmall` | 14 Medium | Names, inline emphasis |
| `bodyLarge` | 16 | Default body |
| `bodyMedium` | 14 | Dense body, list rows |
| `bodySmall` | 12 | Helper text, footer |
| `labelLarge` | 15 Medium | Buttons |
| `labelMedium` | 13 Medium | Tabs, badges |
| `labelSmall` | 11 Medium | Uppercase field labels (the component uppercases) |

Mono lives outside `Typography`: `TbcTheme.text.monoSmall` / `.monoLabel`.

Never call `ProvideTextStyle` globally — it reshapes every stock M3 component.

---

## Spacing cheat-sheet

`xxs` 2 · `xs` 4 · `sm` 8 · `md` 12 · `lg` 16 · `xl` 20 · `xxl` 24 · `x3l` 32 ·
`x4l` 48

Named: `screenH` 20 (screen margin) · `cardPadding` 16 · `touchTarget` 48.

Sections separated by `x3l`, cards by `xxl`, list rows by `md`.

---

## Component picker

| Need | Use |
|---|---|
| Screen root | `TbcScaffold(decorated = …)` |
| Grouped content | `TbcCard` / `TbcRaisedCard` |
| Main action | `TbcButton(style = Primary)` — **one per screen** |
| Secondary action | `TbcButton(style = Secondary)` |
| Dense / inline action | `TbcButton(style = Tonal)` |
| Icon-only action | `TbcIconButton` (`contentDescription` required) |
| Row of actions | `TbcButtonRow` (wraps at large text sizes) |
| Status / category label | `TbcBadge(style = …)` |
| Count next to a header | `CountPill` |
| Legend / category marker | `ColorDot` |
| Top-level navigation | `PillTabRow` (single-select) |
| Narrowing a list | `TbcFilterChip` in a `FilterChipRow` (multi-select) |
| Form input | `TbcTextField` |
| Search | `TbcSearchField` |
| Nothing to show | `EmptyState` — action only if the user can fix it |
| Field label | `FieldLabel` (uppercases for you) |
| Headline figure | `StatTile` / `StatTileGrid` |
| Proportion comparison | `StatBarRow` |
| Inline message / error | `Banner(style = …)` |
| Screen / section heading | `ScreenTitle` / `SectionHeader` |
| Footer | `ScreenFooter` (last scroll item, not pinned) |

Nothing fits? Build it in `ui/components/` from tokens, add it to
`GalleryScreen`, and document it. Do not inline it.

---

## Canonical screen

```kotlin
class ScheduleScreen : Screen {
    @Composable
    override fun Content() {
        val spacing = TbcTheme.spacing
        TbcScaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenH, vertical = spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(spacing.x3l),
            ) {
                ScreenTitle(title = "Schedule", subtitle = "Day 1")
                SectionHeader(title = "Morning", count = "6 sessions")
                TbcCard { /* … */ }
                ScreenFooter(text = "TUM Blockchain Club")
            }
        }
    }
}
```

---

## Mobile adaptations

The reference design is a desktop dashboard. When translating:

- **4-across stat grid → 2×2.** `StatTileGrid` defaults to this.
- **Desktop tab bar → `PillTabRow`,** scrollable with an edge fade. Never crop a
  pill.
- **Two-column form → single column.**
- **Wide table rows → stacked cards.** Never scroll horizontally to read a row.
- **Hover → pressed / ripple.** Hover does not exist; `Tonal` covers what hover
  used to.
- **Pinned footer → last item in the scroll.**

---

## Accessibility floor

- **4.5:1** for body text, **3:1** for UI components and large text.
- `textFaint` and white-on-blue are the two known sub-AA values. `textFaint` is
  decorative/large only; filled blue carries icons and short labels only, and
  badges use the outlined treatment instead.
- **48dp minimum** touch target — `spacing.touchTarget`.
- `contentDescription` is **required** on icon-only controls, and `null` on an
  icon that sits beside a label that already says the same thing.
- Colour is never the only signal. `Banner` gives each severity its own glyph.
- Mark headings with `Modifier.semantics { heading() }` — `ScreenTitle` and
  `SectionHeader` already do.
- Composite rows get one combined `contentDescription` rather than three
  fragments — see `StatBarRow`.

---

## Finish checklist

Before calling UI work done:

- [ ] No `Color(0x…)`, no bare `.sp`, no layout `.dp` literals outside the theme package
- [ ] No stock `Card` / `Button` / `TabRow` / `Scaffold`
- [ ] `tonalElevation = 0.dp` anywhere elevation is passed
- [ ] Every duration routed through `motion.d(...)`
- [ ] Icon-only controls have a `contentDescription`
- [ ] Tappable targets ≥ 48dp
- [ ] New colour? → derived by formula, added to `Palette.kt` + `Tokens.kt` + `references/tokens.md` with its contrast ratio
- [ ] New component? → in `ui/components/`, rendered in `GalleryScreen`, documented
- [ ] Compiles on both: `./gradlew :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64`
- [ ] Checked at the largest system font size (pills and uppercase labels break first)

## Gotchas that fail silently

- **Fonts.** If text renders in Roboto/SF instead of Geist, nothing errors. The
  shared module needs `androidResources.enable = true` on its `android { }`
  target or the APK ships no `composeResources` at all. Check with
  `unzip -l androidApp/build/outputs/apk/debug/androidApp-debug.apk | grep geist`
  (expect five TTFs), or open the gallery's Font parity block — the two rows
  must look *different*.
- **Task names.** This project uses AGP's KMP-library plugin:
  `:shared:compileAndroidMain`, not `:shared:compileDebugKotlinAndroid`.
- **Android light mode.** Always test with the system theme set to LIGHT
  (`adb shell cmd uimode night no`). Dark-mode-only testing hides white flashes
  and invisible status-bar icons.

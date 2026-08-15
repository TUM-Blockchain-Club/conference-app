# Component reference

Signatures, usage and the non-obvious parts of each component's construction.
Source lives in `shared/src/commonMain/kotlin/com/conference/asmara/ui/components/`
— read the file when you need the full body; this page is for picking the right
component and calling it correctly.

Every component here renders in `ui/gallery/GalleryScreen.kt`. Add new ones
there too.

---

## `TbcScaffold`

```kotlin
TbcScaffold(
    modifier: Modifier = Modifier,
    decorated: Boolean = false,   // faint grid + radial glow
    applyInsets: Boolean = true,  // WindowInsets.safeDrawing
    content: @Composable () -> Unit,
)
```

The root of every screen. Deliberately **not** M3's `Scaffold`: this app has no
app bar, no FAB and no bottom bar, so all `Scaffold` would add is a container
colour (taken from `colorScheme.surface`) and a `PaddingValues` most screens
then fight.

`applyInsets = false` only when the screen bleeds an image to the edges and
takes over inset handling itself. Skipping insets on iOS is not cosmetic —
content slides under the notch and the home indicator.

Use `decorated = true` on landing and hero screens; leave it off for dense
lists, where the grid competes with content.

```kotlin
TbcScaffold(decorated = true) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenH, vertical = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.x3l),
    ) { /* … */ }
}
```

---

## `TbcCard` / `TbcRaisedCard`

```kotlin
TbcCard(
    modifier: Modifier = Modifier,
    padding: Dp = TbcTheme.spacing.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
)
```

`#1C1C1C`, 8dp radius, one 8%-white hairline, `tonalElevation = 0.dp`. Passing
`onClick` switches to the clickable `Card` overload, which brings the ripple and
the correct semantics role.

`TbcRaisedCard` is the same at `#252525`, for content that overlays other
content: menus, sheets, popovers.

Never add `Modifier.shadow` to either. Depth in this system is the surface step
plus the hairline; a shadow on `#111111` reads as dirt, and Compose draws it
*behind* the shape so any translucency shows it through as a smudge.

---

## `TbcButton`

```kotlin
TbcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TbcButtonStyle = TbcButtonStyle.Primary,  // Primary | Secondary | Tonal
    icon: ImageVector? = null,
    enabled: Boolean = true,
)
```

- **Primary** — the `.btn-primary` pill: 135° `#3B82F6` → `#8B5CF6` gradient
  with an accent-tinted glow. **One per screen.** Because white on blue is
  3.68:1, it carries short labels only.
- **Secondary** — transparent with a 20% white stroke.
- **Tonal** — opaque `#242424`. No web equivalent; it replaces hover
  affordances, which do not exist on touch.

The gradient is painted by a `Modifier`, not by `ButtonColors`, because M3
button colours are flat `Color`s with no brush slot; the container is set
transparent underneath.

The glow is the system's one sanctioned shadow, and it is a coloured halo rather
than a depth cue. Coloured shadows need API 28+; on 26–27 it degrades to a black
shadow that is invisible on `#111111`, which is a harmless no-op.

### `TbcIconButton`

```kotlin
TbcIconButton(icon, contentDescription: String, onClick, modifier, tint)
```

`contentDescription` is non-null **by type**. An icon with no adjacent label is
the one case where a screen reader has nothing to fall back on, so the API
refuses to let a caller forget.

### `TbcButtonRow`

Wraps to a new line when buttons no longer fit. A plain `Row` of pills is the
first thing to break at the largest system font size.

---

## `TbcBadge` / `CountPill` / `ColorDot`

```kotlin
TbcBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: TbcBadgeStyle = Neutral,  // Accent | Neutral | Success | Warning | Danger
    icon: ImageVector? = null,
)
```

**Outlined, never filled.** `Accent` is blue text on a faint blue tint with a
blue hairline — the "Core Member" badge. This is not a stylistic preference:
white on solid `#3B82F6` is 3.68:1, while blue text on `#111111` is 5.13:1.
There is no filled-blue badge in the system, and adding one would be a bug.

`CountPill("7 departments")` is the trailing chip on a section header — pass the
formatted string including its noun, not a number.

`ColorDot(color)` takes an explicit `Color` rather than a style, because its
usual source is `trackColor(...)` — a runtime value from Supabase, not a token.

---

## `PillTabRow`

```kotlin
PillTabRow(
    tabs: List<PillTab>,          // PillTab(label, icon)
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
)
```

Not M3's `TabRow` / `ScrollableTabRow` — both are built around an indicator line
under an equal-weight row, a different component with a different visual
language.

Scrolls horizontally with a fade at whichever edge has more content. The fade is
drawn *over* the content in the canvas colour rather than as a translucent
overlay, so it cannot double-composite against a pill beneath it.

The selected pill is one of the two places filled blue may carry text: labels
are short, and selection is also encoded by fill, position and icon, so the
3.68:1 text is never the only signal.

---

## `TbcFilterChip` / `FilterChipRow`

```kotlin
TbcFilterChip(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    leadingDot: Color? = null,     // usually trackColor(...)
    icon: ImageVector? = null,
)

FilterChipRow { /* chips */ }
```

Multi-select, which is the whole reason it is not `PillTabRow`. Tabs answer
"where am I" and exactly one is always on; filters answer "what have I narrowed
to" and any number can be. A tab row that let two tabs light up would be a bug,
so the two cannot share a component.

Selection is carried by three signals — accent fill, a check glyph, and
`Role.Checkbox` semantics. The tab row can lean on fill alone because position
and the always-one-selected invariant back it up; a filter row has neither, so
fill-only selection would be invisible to anyone who cannot separate blue from
`#242424`.

`leadingDot` is swapped out for the check while selected, so a chip barely
changes width as it toggles and the row does not reflow under the thumb.

`FilterChipRow` scrolls horizontally and shares `edgeFade` with `PillTabRow`. It
scrolls rather than wraps because filters sit directly above the list they act
on, and wrapping would push the first result off-screen as soon as a conference
grew a fourth track.

---

## `EmptyState`

```kotlin
EmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,   // typically one TbcButton
)
```

Fills its parent: a haloed glyph, a headline, an explanation, and at most one
way out.

Omit `action` when the user has nothing to act on. "No results for your filters"
and "nothing published yet" look alike and are not alike — the first is theirs to
fix and gets a **Clear filters** button, the second is not and gets none. A
button that cannot help is worse than no button.

Not a [`Banner`]: a banner explains content that is still on screen, this
replaces it.

---

## `FieldLabel` / `FieldHelperText`

```kotlin
FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    lockedDescription: String = "Locked",
    trailingIcon: ImageVector? = null,
)
```

The component uppercases the text itself. Pass `"Full name"`, not
`"FULL NAME"` — the source string stays readable, translatable, and correct for
a screen reader that would otherwise spell an all-caps word letter by letter.

`locked = true` draws the padlock used for admin-only fields, and announces it
rather than leaving it as decoration.

---

## `TbcTextField` / `TbcSearchField`

```kotlin
TbcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    locked: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
)

TbcSearchField(value, onValueChange, modifier, placeholder, onClear: (() -> Unit)? = null)
```

`TbcSearchField`'s `onClear` adds a trailing clear button once there is
something to clear. It is a `TbcIconButton`, not a bare `Icon` wired to the
trailing slot, so it reaches the 48dp target and announces itself.

Two structural departures from M3:

1. **The label sits above the field, not inside it.** M3's floating label
   animates from placeholder position into the outline — a different visual
   language, and it makes attaching a padlock or helper text impossible to do
   cleanly.
2. **The container is opaque `#242424`.** The web's
   `--input: rgba(255,255,255,0.07)` renders as a different grey on a card than
   on the canvas.

Still built on `OutlinedTextField` rather than `BasicTextField`, because it
brings cursor, selection handles, IME wiring and accessibility semantics that
would otherwise need rebuilding for two platforms.

---

## `StatTile` / `StatTileGrid`

```kotlin
StatTile(label: String, value: String, accent: StatAccent, modifier, icon)

StatTileGrid(tiles: List<StatTileData>, modifier, columns: Int = 2)
```

Accents come from `TbcTheme.tokens.stats.blue` / `.green` / `.violet` / `.amber`,
or `stats.byIndex(i)` to cycle them over generated content.

The label uses `textOnTile`, **not** `textMuted` — the muted tone drops to about
3:1 against the lighter end of a tile gradient.

`StatTileGrid` defaults to 2 columns: the web's 4-across leaves each tile ~90dp
on a phone, not enough for a two-word label beside a three-digit figure. Built
from `Row`s rather than a `LazyVerticalGrid`, because nesting a lazy grid inside
a scrolling column forces a fixed height.

---

## `StatBarRow`

```kotlin
StatBarRow(
    label: String,
    value: String,
    fraction: Float,              // 0..1, clamped
    modifier: Modifier = Modifier,
    barColor: Color? = null,
    dotColor: Color? = null,
    secondaryValue: String? = null,   // e.g. "(40.9%)"
)
```

Not `LinearProgressIndicator`: that announces itself as *progress* to
accessibility services, and these bars are a static comparison, not a task
completing. The whole row carries one combined `contentDescription`, so a screen
reader reads "Research, 15" instead of three fragments plus an unlabelled
progress bar.

The track is a fixed width, not weighted. Weighting it would let the bar stretch
with the label beside it, so two rows with different label lengths would draw
the same value at different widths — destroying the comparison the component
exists for.

---

## `Banner`

```kotlin
Banner(
    text: String,
    modifier: Modifier = Modifier,
    style: BannerStyle = Info,   // Info | Success | Warning | Error
    title: String? = null,
)
```

The system's one intentional exception to "opaque fills". A banner genuinely
overlays cards, lists and the bare canvas, so a tint flattened for one surface
would be visibly wrong on the other two.

Not `Snackbar`: a banner stays put and belongs to the content it explains, where
a snackbar floats over everything and dismisses itself.

Each severity gets a distinct glyph, not just a distinct hue — amber and red are
the pair most likely to be indistinguishable. With a `title` present the body
text drops to `textPrimary`, because coloured 14sp copy sits near the 4.5:1 floor
for amber and red alike.

---

## `ScreenTitle` / `SectionHeader` / `ScreenFooter`

```kotlin
ScreenTitle(title: String, modifier, subtitle: String? = null)
SectionHeader(title: String, modifier, subtitle: String? = null, count: String? = null)
ScreenFooter(text: String, modifier, trailing: String? = null)
```

`ScreenTitle` is one per screen and is the accessibility entry point;
`SectionHeader` repeats. Both mark their title with
`Modifier.semantics { heading() }` so a screen reader can jump between sections
instead of walking every row.

`ScreenFooter` is the **last item in the scrolling content**, never pinned: a
docked footer costs vertical space on every screen for information nobody reads
twice.

---

## `TbcIcons`

```kotlin
Icon(TbcIcons.Calendar, contentDescription = null, tint = tokens.textMuted)
```

Available: `User` `Users` `Calendar` `BarChart` `CheckCircle` `Hexagon`
`LogOut` `Building` `Search` `MapPin` `Star` `Clock` `Lock` `Info`
`AlertTriangle` `ErrorCircle` `Close` `ChevronRight` `ChevronDown` `ArrowLeft`.

Lucide geometry transcribed as `ImageVector`s — 24×24 outlines, 2px stroke,
round caps and joins. **Do not add `material-icons-core`**: it is frozen at
1.7.3 against this project's 1.11.1 Compose stack.

To add one, copy the `d` attributes from the Lucide SVG (lucide.dev, ISC) into
`ui/icons/TbcIcons.kt` and pass them to the `lucide()` helper. Do not redraw by
hand and do not mix in a filled glyph — the uniform stroke weight is what holds
the set together.

---

## `trackColor` / `eventTypeColor`

```kotlin
trackColor(hex: String?, sortOrder: Int): Color
eventTypeColor(type: EventType): Color
parseHexColor(raw: String?): Color?
raiseForDark(color: Color): Color
```

For colours that come from Supabase rather than the palette. `trackColor`
parses (`#RGB`, `#RRGGBB`, `#RRGGBBAA`, with or without `#`), applies the
dark uplift, and falls back to a six-colour palette indexed by
`sortOrder.mod(6)`.

All pure and non-`@Composable`, which is what makes them unit-testable — see
`commonTest/.../TrackColorTest.kt`.

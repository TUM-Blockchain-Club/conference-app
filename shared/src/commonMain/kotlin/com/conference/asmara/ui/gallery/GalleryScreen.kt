package com.conference.asmara.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.conference.asmara.domain.model.EventType
import com.conference.asmara.ui.components.Banner
import com.conference.asmara.ui.components.BannerStyle
import com.conference.asmara.ui.components.ColorDot
import com.conference.asmara.ui.components.CountPill
import com.conference.asmara.ui.components.EmptyState
import com.conference.asmara.ui.components.FieldLabel
import com.conference.asmara.ui.components.FilterChipRow
import com.conference.asmara.ui.components.PillTab
import com.conference.asmara.ui.components.PillTabRow
import com.conference.asmara.ui.components.ScreenFooter
import com.conference.asmara.ui.components.ScreenTitle
import com.conference.asmara.ui.components.SectionHeader
import com.conference.asmara.ui.components.StatBarRow
import com.conference.asmara.ui.components.StatTileData
import com.conference.asmara.ui.components.StatTileGrid
import com.conference.asmara.ui.components.TbcBadge
import com.conference.asmara.ui.components.TbcBadgeStyle
import com.conference.asmara.ui.components.TbcButton
import com.conference.asmara.ui.components.TbcButtonRow
import com.conference.asmara.ui.components.TbcButtonStyle
import com.conference.asmara.ui.components.TbcCard
import com.conference.asmara.ui.components.TbcFilterChip
import com.conference.asmara.ui.components.TbcScaffold
import com.conference.asmara.ui.components.TbcSearchField
import com.conference.asmara.ui.components.TbcTextField
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme
import com.conference.asmara.ui.theme.eventTypeColor
import com.conference.asmara.ui.theme.trackColor

/**
 * Every token and component in one scroll.
 *
 * This is the artifact to screenshot beside the web references — it exists so a
 * regression in the surface ladder, the type ramp or the font pipeline is
 * visible in one place rather than discovered screen by screen.
 *
 * The font-parity block near the top is the important one: Geist failing to
 * package is a *silent* failure that falls back to the platform sans, and the
 * only reliable way to catch it is to put the two adjacent and look.
 */
class GalleryScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val spacing = TbcTheme.spacing
        val tokens = TbcTheme.tokens

        // Deliberately undecorated. The grid-and-glow backdrop tints the canvas
        // toward blue-violet by design, which is fine on a hero screen and
        // actively unhelpful here: this is the screen you colour-pick the
        // surface ladder from, and a decorative wash would contaminate the
        // reading.
        TbcScaffold(decorated = false) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenH, vertical = spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(spacing.x3l),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    ScreenTitle(
                        title = "Design System",
                        subtitle = "TBC tokens and components",
                    )
                    TbcButton(
                        text = "Back",
                        onClick = { navigator.pop() },
                        style = TbcButtonStyle.Secondary,
                        icon = TbcIcons.ArrowLeft,
                    )
                }

                FontParitySection()
                ColorSection()
                TypeSection()
                ButtonSection()
                BadgeSection()
                TabSection()
                FilterChipSection()
                StatTileSection()
                StatBarSection()
                BannerSection()
                FieldSection()
                EmptyStateSection()
                TrackColorSection()

                Spacer(Modifier.height(spacing.lg))
                ScreenFooter(
                    text = "TUM Blockchain Club",
                    trailing = "Gallery",
                )
            }
        }
    }
}

/**
 * The font-packaging check.
 *
 * `Font()` falls back to the platform default silently when a resource fails to
 * load, so "the text renders" proves nothing. Two samples of the same string
 * side by side do: if the rows look identical on one platform and different on
 * the other, the identical one is not loading Geist.
 */
@Composable
private fun FontParitySection() {
    val spacing = TbcTheme.spacing
    val tokens = TbcTheme.tokens

    GallerySection(
        title = "Font parity",
        subtitle = "These two rows must look different",
    ) {
        TbcCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                FieldLabel("Geist (bundled)")
                Text(
                    text = "Handgloves 0123 — Il1 O0",
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.textPrimary,
                )
                FieldLabel("Platform default")
                Text(
                    text = "Handgloves 0123 — Il1 O0",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Default,
                    ),
                    color = tokens.textPrimary,
                )
                FieldLabel("Geist Mono")
                Text(
                    text = "Handgloves 0123 — Il1 O0",
                    style = TbcTheme.text.monoSmall,
                    color = tokens.textMuted,
                )
            }
        }
    }
}

/**
 * The surface ladder and the accents, each labelled with the hex it should be.
 *
 * Colour-pick these from a screenshot to confirm nothing has drifted: the
 * neutrals must have identical R, G and B channels. Any blue creeping into
 * them means `surfaceTint` has come loose.
 */
@Composable
private fun ColorSection() {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    GallerySection(title = "Colour", subtitle = "Neutrals must be pure grey") {
        TbcCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                FieldLabel("Surface ladder")
                Swatch("surfaceBase", tokens.surfaceBase)
                Swatch("surfaceCard", tokens.surfaceCard)
                Swatch("surfaceMuted", tokens.surfaceMuted)
                Swatch("surfaceRaised", tokens.surfaceRaised)

                Spacer(Modifier.height(spacing.sm))
                FieldLabel("Text")
                Swatch("textPrimary", tokens.textPrimary)
                Swatch("textMuted", tokens.textMuted)
                Swatch("textFaint", tokens.textFaint)

                Spacer(Modifier.height(spacing.sm))
                FieldLabel("Accent")
                Swatch("accent", tokens.accent)
                Swatch("accentPressed", tokens.accentPressed)
                Swatch("accentSecondary", tokens.accentSecondary)

                Spacer(Modifier.height(spacing.sm))
                FieldLabel("Status")
                Swatch("success", tokens.success)
                Swatch("warning", tokens.warning)
                Swatch("danger", tokens.danger)
            }
        }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    val tokens = TbcTheme.tokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TbcTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color)
                .border(1.dp, tokens.borderSubtle, MaterialTheme.shapes.extraSmall)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = color.toHexString(),
            style = TbcTheme.text.monoLabel,
            color = tokens.textMuted,
        )
    }
}

/** Renders a colour as `#RRGGBB`, or `#RRGGBBAA` when it carries alpha. */
private fun Color.toHexString(): String {
    fun channel(value: Float): String {
        val byte = (value * 255f + 0.5f).toInt().coerceIn(0, 255)
        return byte.toString(16).uppercase().padStart(2, '0')
    }
    val rgb = "#${channel(red)}${channel(green)}${channel(blue)}"
    return if (alpha >= 1f) rgb else rgb + channel(alpha)
}

@Composable
private fun TypeSection() {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    val type = MaterialTheme.typography

    GallerySection(title = "Type", subtitle = "Geist, compressed for mobile") {
        TbcCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                TypeRow("displaySmall", "132", type.displaySmall)
                TypeRow("headlineLarge", "Dashboard", type.headlineLarge)
                TypeRow("headlineSmall", "Organization Statistics", type.headlineSmall)
                TypeRow("titleLarge", "Department Distribution", type.titleLarge)
                TypeRow("titleSmall", "Kirill Inozemtsev", type.titleSmall)
                TypeRow("bodyLarge", "The quick brown fox", type.bodyLarge)
                TypeRow("bodyMedium", "The quick brown fox", type.bodyMedium)
                TypeRow("bodySmall", "Only admins can change this field.", type.bodySmall)
                TypeRow("labelLarge", "I'm Interested", type.labelLarge)
                TypeRow("labelMedium", "Statistics", type.labelMedium)
                TypeRow("labelSmall", "FULL NAME", type.labelSmall)
            }
        }
    }
}

@Composable
private fun TypeRow(name: String, sample: String, style: TextStyle) {
    val tokens = TbcTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(TbcTheme.spacing.xxs)) {
        Text(text = name, style = TbcTheme.text.monoLabel, color = tokens.textFaint)
        Text(text = sample, style = style, color = tokens.textPrimary)
    }
}

@Composable
private fun ButtonSection() {
    val spacing = TbcTheme.spacing
    GallerySection(title = "Buttons", subtitle = "One primary per screen") {
        TbcCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                TbcButtonRow {
                    TbcButton("Primary", onClick = {})
                    TbcButton("Secondary", onClick = {}, style = TbcButtonStyle.Secondary)
                }
                TbcButtonRow {
                    TbcButton("Tonal", onClick = {}, style = TbcButtonStyle.Tonal)
                    TbcButton("With icon", onClick = {}, icon = TbcIcons.Star)
                }
                FieldLabel("Disabled")
                TbcButtonRow {
                    TbcButton("Primary", onClick = {}, enabled = false)
                    TbcButton("Secondary", onClick = {}, style = TbcButtonStyle.Secondary, enabled = false)
                    TbcButton("Tonal", onClick = {}, style = TbcButtonStyle.Tonal, enabled = false)
                }
            }
        }
    }
}

@Composable
private fun BadgeSection() {
    val spacing = TbcTheme.spacing
    GallerySection(title = "Badges", subtitle = "Outlined, never filled blue") {
        TbcCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    TbcBadge("Core Member", style = TbcBadgeStyle.Accent)
                    TbcBadge("IT & Development", icon = TbcIcons.Building)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    TbcBadge("Active", style = TbcBadgeStyle.Success)
                    TbcBadge("Pending", style = TbcBadgeStyle.Warning)
                    TbcBadge("Expired", style = TbcBadgeStyle.Danger)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    CountPill("7 departments")
                    CountPill("132 total")
                }
            }
        }
    }
}

@Composable
private fun TabSection() {
    var selected by remember { mutableIntStateOf(3) }
    val tabs = remember {
        listOf(
            PillTab("My Profile", TbcIcons.User),
            PillTab("All Members", TbcIcons.Users),
            PillTab("Events", TbcIcons.Calendar),
            PillTab("Statistics", TbcIcons.BarChart),
            PillTab("Attendance", TbcIcons.CheckCircle),
            PillTab("NFT Status", TbcIcons.Hexagon),
        )
    }
    GallerySection(title = "Tabs", subtitle = "Scrolls horizontally, fades at the edge") {
        PillTabRow(
            tabs = tabs,
            selectedIndex = selected,
            onSelect = { selected = it },
        )
    }
}

/**
 * Multi-select filters, next to the single-select tabs above so the difference
 * is visible: several chips can be on at once, and each carries a check glyph
 * as well as the fill so selection is not signalled by colour alone.
 */
@Composable
private fun FilterChipSection() {
    var tracks by remember { mutableStateOf(setOf("DeFi")) }
    var upcoming by remember { mutableStateOf(false) }
    val seeds = remember {
        listOf("DeFi" to "#4F46E5", "Security" to "#DC2626", "AI x Crypto" to "#059669")
    }

    GallerySection(title = "Filter chips", subtitle = "Multi-select, unlike tabs") {
        FilterChipRow {
            seeds.forEachIndexed { index, (name, hex) ->
                TbcFilterChip(
                    label = name,
                    selected = name in tracks,
                    onToggle = { tracks = if (name in tracks) tracks - name else tracks + name },
                    leadingDot = trackColor(hex, index),
                )
            }
            TbcFilterChip(
                label = "Upcoming",
                selected = upcoming,
                onToggle = { upcoming = !upcoming },
                icon = TbcIcons.Clock,
            )
        }
    }
}

/**
 * The two empty states that are most often collapsed into one: "your filters
 * matched nothing", which the user can fix, and "there is no data yet", which
 * they cannot. Only the first gets an action.
 */
@Composable
private fun EmptyStateSection() {
    val spacing = TbcTheme.spacing
    GallerySection(title = "Empty states", subtitle = "Only actionable ones get a button") {
        TbcCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.x3l)) {
                EmptyState(
                    title = "No matching sessions",
                    description = "Nothing matches your search and filters.",
                    icon = TbcIcons.Search,
                    modifier = Modifier.height(EmptyStateSampleHeight),
                ) {
                    TbcButton(
                        text = "Clear filters",
                        onClick = {},
                        style = TbcButtonStyle.Secondary,
                    )
                }
                EmptyState(
                    title = "Schedule coming soon",
                    description = "The programme hasn't been published yet.",
                    icon = TbcIcons.Calendar,
                    modifier = Modifier.height(EmptyStateSampleHeight),
                )
            }
        }
    }
}

/** The real component fills its parent; the gallery has to bound it. */
private val EmptyStateSampleHeight = 260.dp

@Composable
private fun StatTileSection() {
    val tokens = TbcTheme.tokens
    GallerySection(title = "Stat tiles", subtitle = "4-across on desktop becomes 2x2") {
        StatTileGrid(
            tiles = listOf(
                StatTileData("Total Members", "132", tokens.stats.blue, TbcIcons.Users),
                StatTileData("Active Members", "54", tokens.stats.green, TbcIcons.CheckCircle),
                StatTileData("Departments", "7", tokens.stats.violet, TbcIcons.Building),
                StatTileData("Ex-Core", "67", tokens.stats.amber, TbcIcons.User),
            ),
        )
    }
}

@Composable
private fun StatBarSection() {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    GallerySection(title = "Stat bars") {
        TbcCard(Modifier.fillMaxWidth()) {
            SectionHeader(title = "Member Status", count = "132 total")
            Spacer(Modifier.height(spacing.lg))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                StatBarRow("Active", "54", 0.409f, barColor = tokens.success, dotColor = tokens.success, secondaryValue = "(40.9%)")
                StatBarRow("Honorary", "10", 0.076f, barColor = tokens.warning, dotColor = tokens.warning, secondaryValue = "(7.6%)")
                StatBarRow("Alumni", "35", 0.265f, barColor = tokens.success, dotColor = tokens.success, secondaryValue = "(26.5%)")
                StatBarRow("Advisor", "7", 0.053f, barColor = tokens.accentSecondary, dotColor = tokens.accentSecondary, secondaryValue = "(5.3%)")
                StatBarRow("Passive", "15", 0.114f, barColor = tokens.textMuted, dotColor = tokens.textMuted, secondaryValue = "(11.4%)")
            }
        }
    }
}

@Composable
private fun BannerSection() {
    val spacing = TbcTheme.spacing
    GallerySection(title = "Banners") {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Banner("Schedule synced 2 minutes ago.", style = BannerStyle.Info)
            Banner("Your attendance was recorded.", style = BannerStyle.Success)
            Banner("This session is nearly full.", style = BannerStyle.Warning)
            Banner(
                text = "Could not reach the server. Showing cached data.",
                style = BannerStyle.Error,
                title = "Offline",
            )
        }
    }
}

@Composable
private fun FieldSection() {
    var name by remember { mutableStateOf("Kirill Inozemtsev") }
    var query by remember { mutableStateOf("") }
    val spacing = TbcTheme.spacing

    GallerySection(title = "Fields", subtitle = "Label above, opaque container") {
        TbcCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                TbcTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full name",
                )
                TbcTextField(
                    value = "IT & Development",
                    onValueChange = {},
                    label = "Department",
                    locked = true,
                    helperText = "Only admins can change this field.",
                )
                TbcTextField(
                    value = "not-an-email",
                    onValueChange = {},
                    label = "Email",
                    isError = true,
                    helperText = "Enter a valid address.",
                )
                TbcSearchField(value = query, onValueChange = { query = it })
            }
        }
    }
}

/**
 * Colours that come from the database rather than the palette.
 *
 * The left column is what Supabase stores; the right is what
 * [trackColor] renders after the dark uplift. The seeded values are Tailwind
 * `*-600` tones authored for white backgrounds, which is exactly why the raw
 * column reads muddy here.
 */
@Composable
private fun TrackColorSection() {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    GallerySection(title = "Data-driven colour", subtitle = "Supabase values, lifted for dark") {
        TbcCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                FieldLabel("Track colours")
                TrackRow("Protocol Research", "#4F46E5", 0)
                TrackRow("Applications", "#DC2626", 1)
                TrackRow("Regulation", "#059669", 2)
                TrackRow("No colour set", null, 3)
                TrackRow("Invalid value", "rebeccapurple", 4)

                Spacer(Modifier.height(spacing.sm))
                FieldLabel("Event types")
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    EventType.entries.forEach { type ->
                        ColorDot(color = eventTypeColor(type))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(name: String, raw: String?, sortOrder: Int) {
    val tokens = TbcTheme.tokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TbcTheme.spacing.md),
    ) {
        ColorDot(color = trackColor(raw, sortOrder))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = raw ?: "null",
            style = TbcTheme.text.monoLabel,
            color = tokens.textFaint,
        )
    }
}

@Composable
private fun GallerySection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TbcTheme.spacing.lg)) {
        SectionHeader(title = title, subtitle = subtitle)
        content()
    }
}

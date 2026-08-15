package com.conference.asmara.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.theme.StatAccent
import com.conference.asmara.ui.theme.TbcTheme

/**
 * A headline figure on a tinted card — "Total Members / 132", "Departments / 7".
 *
 * The tints are not eyeballed. Each [StatAccent] is derived from its base brand
 * colour by the flatten formula in `docs/DESIGN.md`:
 * `flatten(base, a) = 17 + (channel - 17) * a`, evaluated at 0.18 for the
 * gradient start, 0.30 for the end and 0.45 for the border. The results are
 * opaque, so a tile looks identical on the canvas and inside a card. The icon
 * uses the Tailwind `*-400` tone, the only step light enough to read as accent
 * rather than as more background.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    accent: StatAccent,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val spacing = TbcTheme.spacing
    val tokens = TbcTheme.tokens
    val shape = MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(accent.start, accent.end)))
            .border(HairlineWidth, accent.border, shape)
            .padding(spacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    // Not textMuted: against the lighter end of a tile gradient
                    // it drops to ~3:1. See TbcTokens.textOnTile.
                    color = tokens.textOnTile,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    color = tokens.textPrimary,
                )
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    // The label already carries the meaning; announcing the
                    // icon separately would only repeat it.
                    contentDescription = null,
                    tint = accent.icon,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/** One entry in a [StatTileGrid]. */
data class StatTileData(
    val label: String,
    val value: String,
    val accent: StatAccent,
    val icon: ImageVector? = null,
)

/**
 * Stat tiles in a fixed-column grid.
 *
 * **Mobile adaptation.** The web puts four tiles across in one row. At phone
 * width that leaves each tile about 90dp — not enough for a two-word label
 * beside a three-digit figure. Two across is the honest translation.
 *
 * Built from `Row`s rather than a `LazyVerticalGrid` deliberately: these rows
 * are always short and fully known, and nesting a lazy grid inside a scrolling
 * column forces a fixed height, which is exactly the constraint a stat row
 * should not have.
 */
@Composable
fun StatTileGrid(
    tiles: List<StatTileData>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    val spacing = TbcTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        tiles.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                row.forEach { tile ->
                    StatTile(
                        label = tile.label,
                        value = tile.value,
                        accent = tile.accent,
                        icon = tile.icon,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keeps a short final row aligned with the ones above it
                // instead of stretching its tiles to full width.
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f).height(0.dp))
                }
            }
        }
    }
}

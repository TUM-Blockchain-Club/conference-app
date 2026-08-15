package com.conference.asmara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.theme.TbcTheme

/**
 * Badge treatments, matching the two the web platform uses on the profile card.
 *
 * - [Accent] — the "Core Member" badge: blue text on a faint blue tint with a
 *   blue hairline. **Outlined, not filled.** White on solid `#3B82F6` is
 *   3.68:1, below the 4.5:1 body-text bar; blue text on `#111111` is 5.13:1 and
 *   passes. This is why the system has no filled-blue badge.
 * - [Neutral] — the "IT & Development" badge: muted text on `#242424`.
 * - [Success] / [Warning] / [Danger] — status, same outlined construction.
 */
enum class TbcBadgeStyle { Accent, Neutral, Success, Warning, Danger }

@Composable
fun TbcBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: TbcBadgeStyle = TbcBadgeStyle.Neutral,
    icon: ImageVector? = null,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    val (content, container, border) = when (style) {
        TbcBadgeStyle.Accent -> Triple(tokens.accent, tokens.accentTint, tokens.accentBorder)
        TbcBadgeStyle.Neutral -> Triple(tokens.textMuted, tokens.surfaceMuted, tokens.borderSubtle)
        TbcBadgeStyle.Success -> Triple(tokens.success, Color.Transparent, tokens.success.copy(alpha = 0.3f))
        TbcBadgeStyle.Warning -> Triple(tokens.warning, tokens.warningTint, tokens.warningBorder)
        TbcBadgeStyle.Danger -> Triple(tokens.danger, tokens.dangerTint, tokens.dangerBorder)
    }

    Row(
        modifier = modifier
            .clip(tokens.pill)
            .background(container)
            .border(HairlineWidth, border, tokens.pill)
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

/**
 * The small count chip that trails a section header — the web's "7 departments"
 * and "132 total" pills.
 */
@Composable
fun CountPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    val tokens = TbcTheme.tokens
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = tokens.textMuted,
        // A pill that wraps stops being a pill. Squeezed for width it should
        // truncate on one line, not stack its letters vertically.
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(tokens.pill)
            .background(tokens.surfaceMuted)
            .border(HairlineWidth, tokens.borderSubtle, tokens.pill)
            .padding(horizontal = TbcTheme.spacing.sm, vertical = TbcTheme.spacing.xxs),
    )
}

/**
 * The coloured dot that precedes a label in a legend or a stat bar row.
 *
 * Takes an explicit colour rather than a style because its most common source
 * is [com.conference.asmara.ui.theme.trackColor] — a runtime value from the
 * database, not a design token.
 */
@Composable
fun ColorDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(TbcTheme.tokens.pill)
            .background(color)
    )
}

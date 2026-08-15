package com.conference.asmara.ui.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme

/**
 * One session in the map's selection sheet: a "NOW"/"NEXT" marker, the title,
 * the time, and a chevron into the detail screen.
 *
 * Not `EventRow`. That is a card, and this already sits inside one — a card in
 * a card puts two hairlines a few pixels apart, which reads as a rendering
 * fault rather than as structure. This is the flat, inline form of the same
 * thing: `surfaceMuted` on the card's `surfaceCard`, one step up the ladder,
 * separated by value rather than by border.
 *
 * The whole row is one accessibility node with one description, rather than
 * three fragments a screen reader would read as three unrelated strings.
 */
@Composable
fun MapSessionRow(
    label: String,
    title: String,
    timeLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(tokens.surfaceMuted)
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = spacing.touchTarget)
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .clearAndSetSemantics { contentDescription = "$label: $title, $timeLabel" },
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.accent,
                )
                // Mono, so "NOW 14:00 – 14:45" and "NEXT 15:00 – 15:45" line up
                // on the colon in the two-row case.
                Text(
                    text = timeLabel,
                    style = TbcTheme.text.monoLabel,
                    color = tokens.textMuted,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = tokens.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = TbcIcons.ChevronRight,
            contentDescription = null,
            tint = tokens.textFaint,
            modifier = Modifier.size(ChevronSize),
        )
    }
}

private val ChevronSize = 20.dp

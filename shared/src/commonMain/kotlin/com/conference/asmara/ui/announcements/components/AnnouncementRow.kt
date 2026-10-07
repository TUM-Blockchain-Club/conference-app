package com.conference.asmara.ui.announcements.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.conference.asmara.domain.model.AnnouncementCategory
import com.conference.asmara.ui.announcements.AnnouncementItem
import com.conference.asmara.ui.announcements.label
import com.conference.asmara.ui.components.TbcBadge
import com.conference.asmara.ui.components.TbcBadgeStyle
import com.conference.asmara.ui.components.TbcCard
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme

/**
 * One announcement. Not clickable: the whole message is already on the card,
 * and a detail screen repeating it would be a tap that buys nothing.
 *
 * The category is a badge with a word in it, not just a colour — emergency's
 * red is the case where being colour-blind must not mean missing it, so it
 * also carries the warning glyph.
 */
@Composable
fun AnnouncementRow(item: AnnouncementItem, modifier: Modifier = Modifier) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    // Merged, so a screen reader reads one announcement as one thing rather
    // than four fragments.
    TbcCard(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                TbcBadge(
                    text = item.category.label(),
                    style = item.category.badgeStyle(),
                    icon = if (item.category == AnnouncementCategory.EMERGENCY) TbcIcons.AlertTriangle else null,
                )
                if (item.isNew) {
                    TbcBadge(text = "New", style = TbcBadgeStyle.Accent)
                }
                Spacer(Modifier.weight(1f))
                // Mono, as on the schedule: times that line up on the colon
                // scan as a column.
                Text(
                    text = item.timeLabel,
                    style = TbcTheme.text.monoSmall,
                    color = tokens.textMuted,
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = tokens.textPrimary,
            )
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textPrimary,
            )
        }
    }
}

/**
 * No new colours: emergency is the existing `danger`, schedule changes the
 * accent (they are the ones you act on), everything else neutral.
 */
private fun AnnouncementCategory.badgeStyle(): TbcBadgeStyle = when (this) {
    AnnouncementCategory.EMERGENCY -> TbcBadgeStyle.Danger
    AnnouncementCategory.SCHEDULE -> TbcBadgeStyle.Accent
    AnnouncementCategory.GENERAL, AnnouncementCategory.KEYNOTE -> TbcBadgeStyle.Neutral
}

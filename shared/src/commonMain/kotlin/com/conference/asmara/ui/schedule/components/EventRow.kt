package com.conference.asmara.ui.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.conference.asmara.domain.model.Event
import com.conference.asmara.ui.common.ConferenceTimeZone
import com.conference.asmara.ui.common.timeRangeLabel
import com.conference.asmara.ui.components.ColorDot
import com.conference.asmara.ui.components.TbcCard
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme
import com.conference.asmara.ui.theme.trackColor

/**
 * One session in the schedule list.
 *
 * **Mobile adaptation.** The reference design lays a session out as a wide
 * table row — time, title, speaker, track and room in five columns. That would
 * force horizontal scrolling to read a single row on a phone, so it stacks into
 * a card, with the metadata that fits on one line kept on one line.
 */
@Composable
fun EventRow(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    TbcCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                // Mono, because a column of times that do not align on the
                // colon reads as noise rather than as a schedule.
                Text(
                    text = timeRangeLabel(event.startTime, event.endTime, ConferenceTimeZone),
                    style = TbcTheme.text.monoSmall,
                    color = tokens.textMuted,
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.textPrimary,
                )
                val speakerNames = event.speakers
                    .sortedBy { it.sortOrder }
                    .joinToString { it.speaker.name }
                if (speakerNames.isNotEmpty()) {
                    Text(
                        text = speakerNames,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                EventMetaRow(event)
            }
            Icon(
                imageVector = TbcIcons.ChevronRight,
                // The card itself is the control and it is labelled by its title.
                contentDescription = null,
                tint = tokens.textFaint,
                modifier = Modifier.size(ChevronSize),
            )
        }
    }
}

/** Track marker and room, on one line under the session. */
@Composable
private fun EventMetaRow(event: Event, modifier: Modifier = Modifier) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    val track = event.track
    val location = event.location
    if (track == null && location == null) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (track != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorDot(trackColor(track.color, track.sortOrder))
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.textMuted,
                )
            }
        }
        if (location != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = TbcIcons.MapPin,
                    contentDescription = null,
                    tint = tokens.textFaint,
                    modifier = Modifier.size(MetaIconSize),
                )
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val ChevronSize = 20.dp
private val MetaIconSize = 14.dp

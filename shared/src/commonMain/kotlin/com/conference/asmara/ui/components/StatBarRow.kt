package com.conference.asmara.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.theme.TbcTheme

/**
 * A labelled proportion bar — the "Research ▬▬ 15" and "Active ▬▬ 54 (40.9%)"
 * rows on the statistics screen.
 *
 * Not `LinearProgressIndicator`: that component announces itself as progress to
 * accessibility services, and these bars are a static comparison, not a task
 * completing. The whole row instead carries one combined description, so a
 * screen reader reads "Research, 15" rather than three disconnected fragments
 * and an unlabelled progress bar.
 *
 * @param fraction 0..1, clamped. Values outside the range are a data bug, not a
 *   reason to draw outside the track.
 * @param dotColor optional leading dot. Supply the track's colour from
 *   [com.conference.asmara.ui.theme.trackColor] for schedule data; omit it when
 *   the label alone is enough.
 */
@Composable
fun StatBarRow(
    label: String,
    value: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    barColor: Color? = null,
    dotColor: Color? = null,
    secondaryValue: String? = null,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    val motion = TbcTheme.motion
    val safeFraction = fraction.coerceIn(0f, 1f)

    val animated by animateFloatAsState(
        targetValue = safeFraction,
        animationSpec = tween(motion.d(motion.slow)),
        label = "statBar",
    )

    val description = buildString {
        append(label)
        append(", ")
        append(value)
        if (secondaryValue != null) {
            append(" ")
            append(secondaryValue)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        if (dotColor != null) {
            ColorDot(color = dotColor)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .width(BarWidth)
                .height(BarHeight)
                .clip(tokens.pill)
                .background(tokens.surfaceMuted)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(tokens.pill)
                    .background(barColor ?: tokens.accent)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = tokens.textPrimary,
        )
        if (secondaryValue != null) {
            Text(
                text = secondaryValue,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
        }
    }
}

/**
 * A fixed-width track, not a weighted one.
 *
 * Weighting the bar would let it stretch or collapse with the label beside it,
 * so two rows with different label lengths would draw the same value at
 * different widths — the exact comparison the component exists to support.
 */
private val BarWidth = 96.dp
private val BarHeight = 6.dp

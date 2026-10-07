package com.conference.asmara.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.theme.TbcTheme

/**
 * An on/off switch: a pill track with a round thumb.
 *
 * Not M3's `Switch`, which is on the "never" list with the rest of the stock
 * controls: its track and thumb colours come from `colorScheme` roles that this
 * theme deliberately neutralises, and its checked state draws a filled tonal
 * track this system has no token for.
 *
 * State is carried by more than colour. On is an accent track with the thumb
 * at the end; off is a muted track with a hairline and the thumb at the start —
 * position alone reads correctly to anyone who cannot separate blue from grey.
 *
 * ### Two ways to use it
 *
 * - **Standalone**: pass [onCheckedChange] and a [contentDescription]. The
 *   switch is the control, gets `Role.Switch` semantics and a 48dp target.
 * - **Inside a row**: pass `onCheckedChange = null` and make the *row*
 *   `toggleable(role = Role.Switch)`. The whole row is then the target and its
 *   label is what a screen reader announces, which is what users expect of a
 *   settings row. The switch becomes a pure indicator and [contentDescription]
 *   is ignored.
 *
 * @param enabled false greys the control out. A disabled switch that is
 *   [checked] reads as "on, and not yours to change" — used for categories a
 *   user cannot opt out of.
 */
@Composable
fun TbcSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    val tokens = TbcTheme.tokens
    val motion = TbcTheme.motion

    val track by animateColorAsState(
        targetValue = when {
            checked && enabled -> tokens.accent
            else -> tokens.surfaceMuted
        },
        animationSpec = tween(motion.d(motion.fast)),
        label = "switchTrack",
    )
    val thumb by animateColorAsState(
        targetValue = when {
            !enabled -> tokens.textDisabled
            checked -> tokens.onAccent
            else -> tokens.textMuted
        },
        animationSpec = tween(motion.d(motion.fast)),
        label = "switchThumb",
    )
    val stroke by animateColorAsState(
        targetValue = if (checked && enabled) tokens.accent else tokens.borderStrong,
        animationSpec = tween(motion.d(motion.fast)),
        label = "switchStroke",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - ThumbSize - ThumbInset * 2 else 0.dp,
        animationSpec = tween(motion.d(motion.fast)),
        label = "switchThumbOffset",
    )

    val interaction = if (onCheckedChange != null) {
        Modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            )
    } else {
        Modifier
    }

    // The visible track is 44x24; the box around it is what makes the target
    // 48dp in both directions.
    Box(
        modifier = modifier
            .then(interaction)
            .defaultMinSize(minWidth = TbcTheme.spacing.touchTarget, minHeight = TbcTheme.spacing.touchTarget),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = TrackWidth, height = TrackHeight)
                .clip(tokens.pill)
                .background(track)
                .border(HairlineWidth, stroke, tokens.pill)
                .padding(ThumbInset),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(ThumbSize)
                    .clip(tokens.pill)
                    .background(thumb),
            )
        }
    }
}

private val TrackWidth = 44.dp
private val TrackHeight = 24.dp
private val ThumbSize = 18.dp
private val ThumbInset = 3.dp

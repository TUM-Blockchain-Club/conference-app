package com.conference.asmara.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme

/**
 * A multi-select filter pill.
 *
 * Distinct from [PillTabRow], which is single-select navigation: tabs answer
 * "where am I", filters answer "what have I narrowed to", and several can be on
 * at once. That difference is not stylistic — a tab row that let two tabs light
 * up would be a bug, so the two cannot share a component.
 *
 * Selection is deliberately carried by three signals at once — the accent fill,
 * the check glyph and the `Role.Checkbox` semantics. Fill alone would make the
 * state invisible to anyone who cannot separate blue from `#242424`, and unlike
 * a tab row there is no "one of these is always on" invariant to fall back on.
 *
 * @param leadingDot a category colour, usually from
 *   [com.conference.asmara.ui.theme.trackColor]. Replaced by the check glyph
 *   while selected, so the chip's width barely moves as it toggles.
 */
@Composable
fun TbcFilterChip(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    leadingDot: Color? = null,
    icon: ImageVector? = null,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    val motion = TbcTheme.motion

    val container by animateColorAsState(
        targetValue = if (selected) tokens.accent else tokens.surfaceMuted,
        animationSpec = tween(motion.d(motion.fast)),
        label = "chipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) tokens.onAccent else tokens.textMuted,
        animationSpec = tween(motion.d(motion.fast)),
        label = "chipContent",
    )
    val stroke by animateColorAsState(
        targetValue = if (selected) Color.Transparent else tokens.borderSubtle,
        animationSpec = tween(motion.d(motion.fast)),
        label = "chipStroke",
    )

    Row(
        modifier = modifier
            .clip(tokens.pill)
            .background(container)
            .border(HairlineWidth, stroke, tokens.pill)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .defaultMinSize(minHeight = spacing.touchTarget)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        when {
            selected -> Icon(
                imageVector = TbcIcons.CheckCircle,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(MarkerSize),
            )
            leadingDot != null -> ColorDot(color = leadingDot, size = MarkerSize)
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(MarkerSize),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

/**
 * A horizontally scrolling row of [TbcFilterChip]s.
 *
 * Scrolls rather than wraps: filters sit directly above the list they act on,
 * and a wrapping row would push the first result off-screen as soon as a
 * conference grew a fourth track.
 */
@Composable
fun FilterChipRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .edgeFade(tokens.surfaceBase, scrollState.value > 0, scrollState.value < scrollState.maxValue)
            .horizontalScroll(scrollState)
            .padding(horizontal = spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

private val MarkerSize = 14.dp

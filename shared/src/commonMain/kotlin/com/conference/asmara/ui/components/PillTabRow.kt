package com.conference.asmara.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.theme.TbcTheme

/** One entry in a [PillTabRow]. */
data class PillTab(
    val label: String,
    val icon: ImageVector? = null,
)

/**
 * The platform's primary navigation: a row of pills where the selected one is a
 * solid blue fill and the rest are plain text with a muted icon.
 *
 * Not Material 3's `TabRow` or `ScrollableTabRow`: both are built around an
 * indicator line under a full-width, equal-weight row, which is a different
 * component with a different visual language.
 *
 * **Mobile adaptation.** The desktop version fits six tabs on one line. A phone
 * fits two or three, so this scrolls horizontally, and an edge fade signals
 * there is more — a scrollbar would not, and cropping a pill mid-word reads as
 * a layout bug rather than an affordance.
 *
 * The selected pill is one of the few places filled blue is allowed to carry
 * text. It is legal here because tab labels are short and the pill also encodes
 * selection through fill, position and (usually) an icon, so the 3.68:1 text is
 * never the only signal.
 */
@Composable
fun PillTabRow(
    tabs: List<PillTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .edgeFade(tokens.surfaceBase, scrollState.value > 0, scrollState.value < scrollState.maxValue)
            .horizontalScroll(scrollState)
            .padding(horizontal = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            PillTabItem(
                tab = tab,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun PillTabItem(
    tab: PillTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    val motion = TbcTheme.motion

    val container by animateColorAsState(
        targetValue = if (selected) tokens.accent else Color.Transparent,
        animationSpec = tween(motion.d(motion.fast)),
        label = "tabContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) tokens.onAccent else tokens.textMuted,
        animationSpec = tween(motion.d(motion.fast)),
        label = "tabContent",
    )

    Row(
        modifier = Modifier
            .clip(tokens.pill)
            .background(container)
            .clickable(role = Role.Tab, onClick = onClick)
            .defaultMinSize(minHeight = spacing.touchTarget)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (tab.icon != null) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

/**
 * Fades the row out at whichever edge has more content beyond it.
 *
 * Drawn over the content with the canvas colour rather than as an overlay
 * gradient, so it stays opaque and cannot double-composite against the pill
 * beneath it.
 *
 * Shared with [FilterChipRow] — both scroll pills horizontally, and two
 * implementations would drift.
 */
internal fun Modifier.edgeFade(
    canvas: Color,
    fadeStart: Boolean,
    fadeEnd: Boolean,
): Modifier = this.drawWithContent {
    drawContent()
    val width = 24.dp.toPx()
    if (fadeStart) {
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(canvas, Color.Transparent),
                startX = 0f,
                endX = width,
            ),
            size = androidx.compose.ui.geometry.Size(width, size.height),
        )
    }
    if (fadeEnd) {
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, canvas),
                startX = size.width - width,
                endX = size.width,
            ),
            topLeft = Offset(size.width - width, 0f),
            size = androidx.compose.ui.geometry.Size(width, size.height),
        )
    }
}

/** The hairline that separates a tab row from the content below it. */
@Composable
fun TabRowDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .size(HairlineWidth)
            .background(TbcTheme.tokens.borderSubtle)
    )
}

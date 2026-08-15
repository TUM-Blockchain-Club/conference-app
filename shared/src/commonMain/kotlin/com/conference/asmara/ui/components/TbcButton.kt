package com.conference.asmara.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.theme.TbcTheme
import com.conference.asmara.ui.theme.primaryGradientBackground

/**
 * The three button treatments the web platform uses, and no others.
 *
 * - [Primary] — the `.btn-primary` pill: a 135-degree blue-to-violet gradient
 *   with a soft blue glow. One per screen. Because white on `#3B82F6` is
 *   3.68:1, it carries short labels and icons only; anything longer than a few
 *   words belongs in a different treatment.
 * - [Secondary] — the `.btn-secondary` pill: transparent with a 20% white stroke.
 * - [Tonal] — an opaque `#242424` fill for dense contexts (rows, toolbars) where
 *   a gradient pill would be too loud. This one is the app's own addition: the
 *   desktop platform leans on hover states for the same job, and hover does not
 *   exist on a phone.
 */
enum class TbcButtonStyle { Primary, Secondary, Tonal }

@Composable
fun TbcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TbcButtonStyle = TbcButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    val shape = tokens.pill

    // The web pill is 14px/32px padding. Horizontal comes down on mobile — a
    // 32dp inset on a 390dp screen makes two side-by-side buttons impossible —
    // while vertical stays, because the 48dp touch target depends on it.
    val contentPadding = PaddingValues(horizontal = spacing.xxl, vertical = spacing.md)

    val label: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    // Decorative: the adjacent label already names the action.
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }

    val baseModifier = modifier.defaultMinSize(minHeight = spacing.touchTarget)

    when (style) {
        TbcButtonStyle.Primary -> Button(
            onClick = onClick,
            // M3's ButtonColors holds flat Colors with no brush slot, so the
            // gradient is painted by a Modifier and the container is made
            // transparent. The glow is applied first so it sits behind the
            // opaque fill rather than showing through it.
            modifier = baseModifier.then(
                if (enabled) {
                    Modifier
                        .accentGlow(tokens.accent, shape)
                        .primaryGradientBackground(shape)
                } else {
                    Modifier
                }
            ),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = tokens.onAccent,
                disabledContainerColor = tokens.surfaceMuted,
                disabledContentColor = tokens.textDisabled,
            ),
            elevation = null,
            contentPadding = contentPadding,
            content = { label() },
        )

        TbcButtonStyle.Secondary -> Button(
            onClick = onClick,
            modifier = baseModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = tokens.textPrimary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = tokens.textDisabled,
            ),
            border = BorderStroke(
                HairlineWidth,
                if (enabled) tokens.borderStrong else tokens.borderSubtle,
            ),
            elevation = null,
            contentPadding = contentPadding,
            content = { label() },
        )

        TbcButtonStyle.Tonal -> Button(
            onClick = onClick,
            modifier = baseModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = tokens.surfaceMuted,
                contentColor = tokens.textPrimary,
                disabledContainerColor = tokens.surfaceMuted,
                disabledContentColor = tokens.textDisabled,
            ),
            border = BorderStroke(HairlineWidth, tokens.borderSubtle),
            elevation = null,
            contentPadding = contentPadding,
            content = { label() },
        )
    }
}

/**
 * An icon-only control, sized to the 48dp minimum target.
 *
 * [contentDescription] is required rather than nullable: an icon with no
 * adjacent label is the one case where a screen reader has nothing to fall back
 * on, so the API refuses to let a caller forget.
 */
@Composable
fun TbcIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val tokens = TbcTheme.tokens
    IconButton(
        onClick = onClick,
        modifier = modifier.size(TbcTheme.spacing.touchTarget),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: tokens.textMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Actions laid out horizontally, wrapping to a new line when they no longer fit.
 *
 * A plain `Row` is the obvious translation of the desktop button group and it
 * is the first thing to break at the largest system font size, where two pills
 * comfortably exceed a phone's width. Flowing costs nothing when they do fit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TbcButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TbcTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(TbcTheme.spacing.md),
    ) {
        content()
    }
}

/**
 * The `box-shadow: 0 4px 20px rgba(59,130,246,0.3)` under the primary pill.
 *
 * This is the one sanctioned shadow in the system, and it is not a depth cue —
 * it is a coloured halo that belongs to the accent, which is why it is tinted
 * rather than black. Coloured shadows need API 28+ on Android; on 26–27 it
 * degrades to a black shadow that is invisible against `#111111`, which is an
 * acceptable no-op rather than a wrong-looking one.
 */
private fun Modifier.accentGlow(
    color: Color,
    shape: androidx.compose.ui.graphics.Shape,
): Modifier = this.shadow(
    elevation = 12.dp,
    shape = shape,
    ambientColor = color,
    spotColor = color,
)

private val IconSize = 18.dp

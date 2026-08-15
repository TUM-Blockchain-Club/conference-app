package com.conference.asmara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme

/** Banner severities, in ascending order of interruption. */
enum class BannerStyle { Info, Success, Warning, Error }

/**
 * An inline message block: coloured icon, coloured text, a faint tint of the
 * same hue behind it, and a matching hairline.
 *
 * This is the system's one intentional exception to "opaque fills". A banner is
 * genuinely an overlay — it appears inside cards, inside lists and directly on
 * the canvas — and a flattened tint computed for one of those surfaces would be
 * visibly wrong on the other two. Translucency here is what keeps it consistent
 * rather than what breaks it.
 *
 * Not `Snackbar`: a banner stays put and belongs to the content it explains,
 * where a snackbar floats over everything and dismisses itself. Errors that a
 * user has to read and act on need the former.
 *
 * Colour is never the only signal — the icon distinguishes the four styles for
 * anyone who cannot separate red from amber.
 */
@Composable
fun Banner(
    text: String,
    modifier: Modifier = Modifier,
    style: BannerStyle = BannerStyle.Info,
    title: String? = null,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    val shape = MaterialTheme.shapes.medium

    // Each style gets a distinct glyph, not just a distinct hue: amber and red
    // are the pair most likely to be indistinguishable, so warning keeps the
    // triangle and error takes the cross.
    val colors = when (style) {
        BannerStyle.Info -> BannerColors(
            content = tokens.accent,
            container = tokens.accentTint,
            border = tokens.accentBorder,
            icon = TbcIcons.Info,
        )
        BannerStyle.Success -> BannerColors(
            content = tokens.success,
            container = tokens.success.copy(alpha = 0.12f),
            border = tokens.success.copy(alpha = 0.30f),
            icon = TbcIcons.CheckCircle,
        )
        BannerStyle.Warning -> BannerColors(
            content = tokens.warning,
            container = tokens.warningTint,
            border = tokens.warningBorder,
            icon = TbcIcons.AlertTriangle,
        )
        BannerStyle.Error -> BannerColors(
            content = tokens.danger,
            container = tokens.dangerTint,
            border = tokens.dangerBorder,
            icon = TbcIcons.ErrorCircle,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.container)
            .border(HairlineWidth, colors.border, shape)
            .padding(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Icon(
            imageVector = colors.icon,
            // The style name is not useful to announce; the message text is.
            contentDescription = null,
            tint = colors.content,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.content,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                // With a title present the body drops to the high-contrast
                // foreground: coloured 14sp copy sits near the 4.5:1 floor for
                // amber and red alike, and the icon plus border already carry
                // the hue.
                color = if (title == null) colors.content else tokens.textPrimary,
            )
        }
    }
}

private data class BannerColors(
    val content: Color,
    val container: Color,
    val border: Color,
    val icon: ImageVector,
)

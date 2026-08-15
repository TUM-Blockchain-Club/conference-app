package com.conference.asmara.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme

/**
 * The uppercase, letter-spaced label above a form field — "FULL NAME",
 * "DEPARTMENT", "SEMESTER JOINED".
 *
 * The component uppercases the text rather than asking callers to, so the
 * source string stays readable, translatable and correct for a screen reader
 * that would otherwise spell out an all-caps word letter by letter.
 *
 * The colour is `textMuted` (`#8A8A8A`, 5.47:1), not the web's flattened
 * `#767676` (4.03:1). At 11sp this text is nowhere near large enough to qualify
 * for the relaxed 3:1 bar, so the web value would simply fail.
 *
 * @param locked draws the small padlock the platform uses for admin-only
 *   fields, and is announced rather than left as decoration.
 */
@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    lockedDescription: String = "Locked",
    trailingIcon: ImageVector? = null,
) {
    val tokens = TbcTheme.tokens
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TbcTheme.spacing.xs),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textMuted,
        )
        val icon = trailingIcon ?: if (locked) TbcIcons.Lock else null
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = if (locked) lockedDescription else null,
                tint = tokens.textFaint,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** Helper text under a field — "Only admins can change this field." */
@Composable
fun FieldHelperText(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val tokens = TbcTheme.tokens
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) tokens.danger else tokens.textMuted,
        modifier = modifier,
    )
}

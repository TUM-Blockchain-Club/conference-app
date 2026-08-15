package com.conference.asmara.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme

/**
 * A text field with its uppercase label above it, matching the profile form.
 *
 * Two structural departures from Material 3:
 *
 * - **The label sits above the field, not inside it.** M3's floating label
 *   animates from placeholder position into the outline, which is a different
 *   visual language and, more practically, makes a lock icon or helper text
 *   impossible to attach cleanly. [FieldLabel] handles it instead.
 * - **The container is an opaque `#242424`, not translucent.** The web uses
 *   `--input: rgba(255,255,255,0.07)`, which renders as a different grey on a
 *   card than on the canvas — the same token producing two colours in one
 *   screenshot.
 *
 * `OutlinedTextField` is still the base rather than `BasicTextField`, because
 * it brings the cursor, selection handles, IME wiring and accessibility
 * semantics that would otherwise have to be rebuilt for two platforms.
 */
@Composable
fun TbcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    locked: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    Column(modifier = modifier) {
        if (label != null) {
            FieldLabel(text = label, locked = locked)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.sm),
            enabled = enabled && !locked,
            singleLine = singleLine,
            isError = isError,
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textFaint,
                    )
                }
            },
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = tokens.surfaceMuted,
                unfocusedContainerColor = tokens.surfaceMuted,
                disabledContainerColor = tokens.surfaceMuted,
                errorContainerColor = tokens.surfaceMuted,
                focusedTextColor = tokens.textPrimary,
                unfocusedTextColor = tokens.textPrimary,
                disabledTextColor = tokens.textDisabled,
                cursorColor = tokens.accent,
                // The web's --ring is rgba(59,130,246,0.4); the focused border
                // is drawn at full strength so it clears the 3:1 UI bar.
                focusedBorderColor = tokens.accent,
                unfocusedBorderColor = tokens.borderSubtle,
                disabledBorderColor = tokens.borderSubtle,
                errorBorderColor = tokens.danger,
            ),
        )
        if (helperText != null) {
            FieldHelperText(
                text = helperText,
                isError = isError,
                modifier = Modifier.padding(top = spacing.xs),
            )
        }
    }
}

/**
 * A search field: the same construction, with a leading magnifier.
 *
 * @param onClear when supplied, a clear button appears once there is something
 *   to clear. It is an [TbcIconButton] rather than a bare [Icon] so it reaches
 *   the 48dp touch target and announces itself — a tappable glyph wired
 *   straight onto the trailing slot is neither.
 */
@Composable
fun TbcSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…",
    onClear: (() -> Unit)? = null,
) {
    val tokens = TbcTheme.tokens
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        leadingIcon = {
            Icon(
                imageVector = TbcIcons.Search,
                contentDescription = null,
                tint = tokens.textMuted,
            )
        },
        trailingIcon = if (onClear != null && value.isNotEmpty()) {
            {
                TbcIconButton(
                    icon = TbcIcons.Close,
                    contentDescription = "Clear search",
                    onClick = onClear,
                )
            }
        } else {
            null
        },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textFaint,
            )
        },
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = tokens.surfaceMuted,
            unfocusedContainerColor = tokens.surfaceMuted,
            focusedTextColor = tokens.textPrimary,
            unfocusedTextColor = tokens.textPrimary,
            cursorColor = tokens.accent,
            focusedBorderColor = tokens.accent,
            unfocusedBorderColor = tokens.borderSubtle,
            focusedLeadingIconColor = tokens.textMuted,
            unfocusedLeadingIconColor = tokens.textMuted,
        ),
    )
}

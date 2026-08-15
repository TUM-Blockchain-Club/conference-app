package com.conference.asmara.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.theme.TbcTheme

/**
 * Stands in for [com.conference.asmara.domain.model.Speaker.photoUrl]: no
 * speaker in the data has a photo yet, so an image loader would be dead code
 * plus per-platform engine wiring.
 *
 * Built from the surface ladder — muted fill, hairline, muted text — so it
 * reads as the same material as the card it sits on rather than as a hole in it.
 */
@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val tokens = TbcTheme.tokens
    Box(
        modifier = modifier
            .size(size)
            .clip(tokens.pill)
            .background(tokens.surfaceMuted)
            .border(1.dp, tokens.borderSubtle, tokens.pill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.initials(),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

internal fun String.initials(): String {
    val words = trim().split(' ', '\t', '\n').filter { it.isNotBlank() }
    return when (words.size) {
        0 -> "?"
        1 -> words[0].take(2).uppercase()
        else -> "${words.first().first()}${words.last().first()}".uppercase()
    }
}

package com.conference.asmara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.theme.TbcTheme

/**
 * The "there is nothing here" panel: a haloed glyph, a headline, an
 * explanation, and at most one way out.
 *
 * A component rather than a `Column` inlined per screen because the states it
 * covers are the ones most likely to be written differently by whoever hits
 * them next — "no results" and "nothing published yet" are different problems
 * with different fixes, and both get solved badly when each screen improvises
 * its own centred `Text`.
 *
 * Not a [Banner]: a banner explains content that is still on screen, whereas
 * this replaces it.
 *
 * @param action typically a single [TbcButton]. Omit it when the user has
 *   nothing to act on — an empty schedule is not their problem to fix.
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.x3l, vertical = spacing.x4l),
        verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(GlyphHalo)
                .clip(tokens.pill)
                .background(tokens.surfaceMuted),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                // The title below says the same thing in words.
                contentDescription = null,
                tint = tokens.textMuted,
                modifier = Modifier.size(GlyphSize),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = tokens.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textMuted,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Box(Modifier.padding(top = spacing.sm)) { action() }
        }
    }
}

private val GlyphHalo = 64.dp
private val GlyphSize = 28.dp

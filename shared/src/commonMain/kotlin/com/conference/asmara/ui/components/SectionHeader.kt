package com.conference.asmara.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.conference.asmara.ui.theme.TbcTheme

/**
 * A section title with an optional subtitle and trailing count — the
 * "Organization Statistics", "Events / 31 tracked events" and
 * "Department Distribution / 7 departments" headers.
 *
 * The title is marked as a heading in the semantics tree, which is what lets a
 * screen reader jump between sections instead of walking every row.
 *
 * @param count renders as a trailing [CountPill]. Pass the already-formatted
 *   string ("7 departments", "132 total") rather than a number: the pluralised
 *   noun belongs with the number, and this component has no business guessing it.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    count: String? = null,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = tokens.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textMuted,
                )
            }
        }
        if (count != null) {
            CountPill(text = count)
        }
    }
}

/**
 * The page-level title — one per screen, above everything else.
 *
 * Separate from [SectionHeader] rather than a size parameter on it because the
 * two are structurally different: a screen title is the accessibility entry
 * point and there is exactly one, while sections repeat.
 */
@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val tokens = TbcTheme.tokens
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TbcTheme.spacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = tokens.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textMuted,
            )
        }
    }
}

/**
 * The page footer — "TUM Blockchain Club Membership Platform / Internal use only".
 *
 * **Mobile adaptation.** The web pins this to the bottom of the viewport. On a
 * phone a pinned footer would eat scarce vertical space on every screen, so it
 * ships as the last item in the scrolling content instead.
 */
@Composable
fun ScreenFooter(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val tokens = TbcTheme.tokens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.textFaint,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textFaint,
            )
        }
    }
}

package com.conference.asmara.ui.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.conference.asmara.ui.components.EmptyState
import com.conference.asmara.ui.components.TbcButton
import com.conference.asmara.ui.components.TbcButtonStyle
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme

/**
 * The three no-content cases stay distinct on purpose. "Your filters match
 * nothing" and "the schedule is not published yet" are different problems with
 * different fixes, and collapsing them into one message sends people looking in
 * the wrong place.
 */
@Composable
fun ScheduleLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = TbcTheme.tokens.accent)
    }
}

@Composable
fun ScheduleNoMatches(onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        title = "No matching sessions",
        description = "Nothing in the schedule matches your search and filters.",
        icon = TbcIcons.Search,
        modifier = modifier,
    ) {
        TbcButton(
            text = "Clear filters",
            onClick = onClearFilters,
            style = TbcButtonStyle.Secondary,
        )
    }
}

@Composable
fun ScheduleNotPublished(modifier: Modifier = Modifier) {
    // No action: an unpublished schedule is not something the attendee can fix.
    EmptyState(
        title = "Schedule coming soon",
        description = "The programme hasn't been published yet. Check back closer to the event.",
        icon = TbcIcons.Calendar,
        modifier = modifier,
    )
}

@Composable
fun ScheduleError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        title = "Couldn't load the schedule",
        description = message,
        icon = TbcIcons.AlertTriangle,
        modifier = modifier,
    ) {
        TbcButton(text = "Try again", onClick = onRetry)
    }
}

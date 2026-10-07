package com.conference.asmara.ui.announcements.components

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
 * The feed's no-content cases, kept apart for the reason `ScheduleStates.kt`
 * gives: "nothing has been announced" and "you muted everything" are different
 * problems, and only the second is the user's to fix.
 */
@Composable
fun AnnouncementsLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = TbcTheme.tokens.accent)
    }
}

@Composable
fun AnnouncementsEmpty(modifier: Modifier = Modifier) {
    // No action: there is nothing an attendee can do to make news happen.
    EmptyState(
        title = "No announcements yet",
        description = "Schedule changes, keynote reminders and venue news will appear here during the event.",
        icon = TbcIcons.Bell,
        modifier = modifier,
    )
}

@Composable
fun AnnouncementsAllMuted(onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        title = "Everything here is muted",
        description = "Every announcement so far is in a category you've turned off below.",
        icon = TbcIcons.Bell,
        modifier = modifier,
    ) {
        TbcButton(text = "Show everything", onClick = onShowAll, style = TbcButtonStyle.Secondary)
    }
}

@Composable
fun AnnouncementsError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        title = "Couldn't load announcements",
        description = message,
        icon = TbcIcons.AlertTriangle,
        modifier = modifier,
    ) {
        TbcButton(text = "Try again", onClick = onRetry)
    }
}

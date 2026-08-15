package com.conference.asmara.ui.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.conference.asmara.ui.components.EmptyState
import com.conference.asmara.ui.components.TbcButton
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme

/**
 * The map's no-content cases, kept distinct for the same reason
 * `ScheduleStates.kt` keeps its three apart: "we could not reach the server"
 * and "there is no map for this venue yet" are different problems, and only one
 * of them has a button.
 */
@Composable
fun MapLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = TbcTheme.tokens.accent)
    }
}

@Composable
fun MapNotPublished(modifier: Modifier = Modifier) {
    // No action: an untraced venue is not something the attendee can fix.
    EmptyState(
        title = "Map coming soon",
        description = "The venue floor plan hasn't been published yet. Check back closer to the event.",
        icon = TbcIcons.MapPin,
        modifier = modifier,
    )
}

@Composable
fun MapError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        title = "Couldn't load the map",
        description = message,
        icon = TbcIcons.AlertTriangle,
        modifier = modifier,
    ) {
        TbcButton(text = "Try again", onClick = onRetry)
    }
}

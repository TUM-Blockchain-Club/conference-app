package com.conference.asmara.ui.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The three no-content cases are deliberately distinct: "your filters match
 * nothing" and "the schedule is empty" are different problems with different
 * fixes, and collapsing them into one message sends people looking in the
 * wrong place.
 */
@Composable
private fun CenteredMessage(
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

@Composable
fun ScheduleLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ScheduleNoMatches(onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    CenteredMessage("No sessions match your filters.", modifier) {
        TextButton(onClick = onClearFilters) { Text("Clear filters") }
    }
}

@Composable
fun ScheduleNotPublished(modifier: Modifier = Modifier) {
    CenteredMessage("The schedule hasn't been published yet.", modifier)
}

@Composable
fun ScheduleError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CenteredMessage(message, modifier) {
        Button(onClick = onRetry) { Text("Try again") }
    }
}

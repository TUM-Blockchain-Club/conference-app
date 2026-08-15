package com.conference.asmara.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.conference.asmara.domain.model.Track
import com.conference.asmara.ui.schedule.ScheduleFilters
import com.conference.asmara.ui.theme.accentColor

/**
 * All three controls write into the one filters flow the screen model owns.
 * An [OutlinedTextField] rather than a `SearchBar`, whose full-screen expand
 * semantics are wrong for a filter that sits above the list it filters.
 */
@Composable
fun ScheduleFilterBar(
    filters: ScheduleFilters,
    tracks: List<Track>,
    onQueryChange: (String) -> Unit,
    onTrackToggle: (String) -> Unit,
    onUpcomingToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = filters.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            placeholder = { Text("Search sessions, speakers, rooms") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            trailingIcon = {
                // No icon set is available on the shared classpath, so the
                // affordance is a text button.
                if (filters.query.isNotEmpty()) {
                    TextButton(onClick = { onQueryChange("") }) { Text("Clear") }
                }
            },
        )

        // No debounce: the dataset is a single conference and filtering runs off
        // the main thread already.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tracks.forEach { track ->
                FilterChip(
                    selected = track.id in filters.trackIds,
                    onClick = { onTrackToggle(track.id) },
                    label = { Text(track.name) },
                    leadingIcon = {
                        Box(Modifier.size(8.dp).background(track.accentColor(), CircleShape))
                    },
                )
            }
            FilterChip(
                selected = filters.upcomingOnly,
                onClick = onUpcomingToggle,
                label = { Text("Upcoming") },
            )
        }
    }
}

@Composable
fun ScheduleOfflineBanner(
    lastSyncedLabel: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (lastSyncedLabel != null) {
                "Offline — showing the saved schedule (updated $lastSyncedLabel)"
            } else {
                "Offline — showing the saved schedule"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(top = 12.dp, end = 8.dp),
        )
        TextButton(onClick = onDismiss) { Text("Dismiss") }
    }
}

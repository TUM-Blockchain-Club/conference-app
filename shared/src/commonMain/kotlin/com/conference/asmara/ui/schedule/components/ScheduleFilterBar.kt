package com.conference.asmara.ui.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.conference.asmara.domain.model.Track
import com.conference.asmara.ui.components.FilterChipRow
import com.conference.asmara.ui.components.TbcFilterChip
import com.conference.asmara.ui.components.TbcSearchField
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.schedule.ScheduleFilters
import com.conference.asmara.ui.theme.TbcTheme
import com.conference.asmara.ui.theme.trackColor

/**
 * Search box plus the track and time chips, all writing into the one filters
 * flow the screen model owns.
 *
 * No debounce on the query: the dataset is a single conference and filtering
 * already runs off the main thread, so the round trip costs less than the
 * dropped keystrokes a debounce would introduce.
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
        verticalArrangement = Arrangement.spacedBy(TbcTheme.spacing.md),
    ) {
        TbcSearchField(
            value = filters.query,
            onValueChange = onQueryChange,
            placeholder = "Search sessions, speakers, rooms",
            onClear = { onQueryChange("") },
        )
        FilterChipRow {
            tracks.forEach { track ->
                TbcFilterChip(
                    label = track.name,
                    selected = track.id in filters.trackIds,
                    onToggle = { onTrackToggle(track.id) },
                    leadingDot = trackColor(track.color, track.sortOrder),
                )
            }
            TbcFilterChip(
                label = "Upcoming",
                selected = filters.upcomingOnly,
                onToggle = onUpcomingToggle,
                icon = TbcIcons.Clock,
            )
        }
    }
}

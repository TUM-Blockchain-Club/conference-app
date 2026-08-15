package com.conference.asmara.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.conference.asmara.ui.detail.EventDetailScreen
import com.conference.asmara.ui.schedule.components.EventRow
import com.conference.asmara.ui.schedule.components.ScheduleError
import com.conference.asmara.ui.schedule.components.ScheduleFilterBar
import com.conference.asmara.ui.schedule.components.ScheduleLoading
import com.conference.asmara.ui.schedule.components.ScheduleNoMatches
import com.conference.asmara.ui.schedule.components.ScheduleNotPublished
import com.conference.asmara.ui.schedule.components.ScheduleOfflineBanner
import org.koin.compose.getKoin

class ScheduleListScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // Hoisted: rememberScreenModel's lambda is @DisallowComposableCalls.
        val koin = getKoin()
        val screenModel = rememberScreenModel { koin.get<ScheduleScreenModel>() }
        val state by screenModel.state.collectAsState()
        val errorMessage = state.errorMessage

        Scaffold(topBar = { TopAppBar(title = { Text("Schedule") }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (state.banner == ScheduleBanner.OFFLINE_SHOWING_CACHE) {
                    ScheduleOfflineBanner(
                        lastSyncedLabel = state.lastSyncedLabel,
                        onDismiss = screenModel::onDismissBanner,
                    )
                }
                ScheduleFilterBar(
                    filters = state.filters,
                    tracks = state.tracks,
                    onQueryChange = screenModel::onQueryChange,
                    onTrackToggle = screenModel::onTrackToggle,
                    onUpcomingToggle = screenModel::onUpcomingToggle,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = screenModel::onPullToRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        state.isLoading -> ScheduleLoading()
                        errorMessage != null -> ScheduleError(errorMessage, screenModel::onRetry)
                        state.isScheduleEmpty -> ScheduleNotPublished()
                        state.isEmptyResult -> ScheduleNoMatches(screenModel::onClearFilters)
                        else -> ScheduleDayList(
                            days = state.days,
                            onEventClick = { navigator.push(EventDetailScreen(it)) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleDayList(
    days: List<ScheduleDay>,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEach { day ->
            stickyHeader(key = "day-${day.date}") {
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            day.slots.forEach { slot ->
                item(key = "slot-${day.date}-${slot.label}") {
                    Text(
                        text = slot.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                items(slot.events, key = { it.id }) { event ->
                    EventRow(
                        event = event,
                        onClick = { onEventClick(event.id) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

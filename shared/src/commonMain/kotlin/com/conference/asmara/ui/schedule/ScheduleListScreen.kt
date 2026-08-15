package com.conference.asmara.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.conference.asmara.ui.components.Banner
import com.conference.asmara.ui.components.BannerStyle
import com.conference.asmara.ui.components.ScreenFooter
import com.conference.asmara.ui.components.ScreenTitle
import com.conference.asmara.ui.components.SectionHeader
import com.conference.asmara.ui.components.TbcScaffold
import com.conference.asmara.ui.detail.EventDetailScreen
import com.conference.asmara.ui.schedule.components.EventRow
import com.conference.asmara.ui.schedule.components.ScheduleError
import com.conference.asmara.ui.schedule.components.ScheduleFilterBar
import com.conference.asmara.ui.schedule.components.ScheduleLoading
import com.conference.asmara.ui.schedule.components.ScheduleNoMatches
import com.conference.asmara.ui.schedule.components.ScheduleNotPublished
import com.conference.asmara.ui.theme.TbcTheme
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
        val tokens = TbcTheme.tokens
        val spacing = TbcTheme.spacing

        // decorated = false: the grid and glow are for landing screens. Behind a
        // dense list they compete with the cards.
        TbcScaffold {
            Column(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screenH)
                        .padding(top = spacing.xxl, bottom = spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    ScreenTitle(
                        title = "Schedule",
                        subtitle = state.sessionCountLabel(),
                    )
                    if (state.banner == ScheduleBanner.OFFLINE_SHOWING_CACHE) {
                        // Warning, not Error: the schedule below is real and
                        // usable, it is only possibly out of date.
                        Banner(
                            title = "Offline",
                            text = state.lastSyncedLabel
                                ?.let { "Showing the saved schedule, last updated $it." }
                                ?: "Showing the saved schedule.",
                            style = BannerStyle.Warning,
                        )
                    }
                    ScheduleFilterBar(
                        filters = state.filters,
                        tracks = state.tracks,
                        onQueryChange = screenModel::onQueryChange,
                        onTrackToggle = screenModel::onTrackToggle,
                        onUpcomingToggle = screenModel::onUpcomingToggle,
                    )
                }

                val refreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = screenModel::onPullToRefresh,
                    modifier = Modifier.fillMaxSize(),
                    state = refreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = refreshState,
                            isRefreshing = state.isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = tokens.surfaceRaised,
                            color = tokens.accent,
                        )
                    },
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
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.screenH, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        days.forEach { day ->
            stickyHeader(key = "day-${day.date}") {
                // The header must be opaque: a sticky header floats over the
                // cards scrolling beneath it, and the canvas colour is the only
                // thing stopping them showing through.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.surfaceBase)
                        .padding(top = spacing.md, bottom = spacing.sm),
                ) {
                    SectionHeader(
                        title = day.label,
                        count = day.sessionCountLabel(),
                    )
                }
            }
            day.slots.forEach { slot ->
                item(key = "slot-${day.date}-${slot.label}") {
                    // The grouping signal for parallel sessions: every card
                    // under this label starts at the same time.
                    Text(
                        text = slot.label,
                        style = TbcTheme.text.monoLabel,
                        color = tokens.textMuted,
                        modifier = Modifier.padding(top = spacing.sm),
                    )
                }
                items(slot.events, key = { it.id }) { event ->
                    EventRow(event = event, onClick = { onEventClick(event.id) })
                }
            }
        }
        item(key = "footer") {
            ScreenFooter(
                text = "TUM Blockchain Club",
                modifier = Modifier.padding(top = spacing.x3l, bottom = spacing.lg),
            )
        }
    }
}

private fun ScheduleUiState.sessionCountLabel(): String {
    val shown = days.sumOf { day -> day.slots.sumOf { it.events.size } }
    return when {
        shown == totalEventCount -> plural(totalEventCount, "session")
        else -> "${plural(shown, "session")} of $totalEventCount"
    }
}

private fun ScheduleDay.sessionCountLabel(): String =
    plural(slots.sumOf { it.events.size }, "session")

private fun plural(count: Int, noun: String): String =
    if (count == 1) "1 $noun" else "$count ${noun}s"

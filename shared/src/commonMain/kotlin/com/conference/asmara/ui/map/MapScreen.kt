package com.conference.asmara.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.conference.asmara.ui.components.Banner
import com.conference.asmara.ui.components.BannerStyle
import com.conference.asmara.ui.components.PillTab
import com.conference.asmara.ui.components.PillTabRow
import com.conference.asmara.ui.components.ScreenTitle
import com.conference.asmara.ui.components.TbcBadge
import com.conference.asmara.ui.components.TbcBadgeStyle
import com.conference.asmara.ui.components.TbcCard
import com.conference.asmara.ui.components.TbcIconButton
import com.conference.asmara.ui.components.TbcRaisedCard
import com.conference.asmara.ui.components.TbcScaffold
import com.conference.asmara.ui.components.TbcSearchField
import com.conference.asmara.ui.detail.EventDetailScreen
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.map.components.MapError
import com.conference.asmara.ui.map.components.MapLoading
import com.conference.asmara.ui.map.components.MapNotPublished
import com.conference.asmara.ui.map.components.MapSessionRow
import com.conference.asmara.ui.theme.TbcTheme
import org.koin.compose.getKoin

/**
 * The Map tab, as a standalone `Screen`.
 *
 * The tab shell renders [MapContent] directly rather than pushing this, for the
 * inset reason spelled out in `RootScreen`. This wrapper is kept because a
 * `Screen` is the unit Voyager can push, and losing the ability to open the map
 * on its own — from a deep link, say — for the sake of deleting six lines would
 * be a bad trade.
 */
class MapScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // Hoisted: rememberScreenModel's lambda is @DisallowComposableCalls.
        val koin = getKoin()
        TbcScaffold {
            MapContent(
                screenModel = rememberScreenModel { koin.get<MapScreenModel>() },
                onEventClick = { navigator.push(EventDetailScreen(it)) },
            )
        }
    }
}

/**
 * The Map tab's body, without a scaffold of its own.
 *
 * @param screenModel passed in rather than obtained here, because Voyager's
 *   `rememberScreenModel` is an extension on `Screen` and this is not one — see
 *   the same note on `ScheduleListContent`.
 * @param focusLocationId a pending "Show on map" request, owned by the shell.
 *   Passed down rather than read from [MapFocusRequests] here, because the
 *   shell has to switch to this tab before the request is consumed — see the
 *   note on that class.
 */
@Composable
fun MapContent(
    screenModel: MapScreenModel,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusLocationId: String? = null,
    onFocusHandled: () -> Unit = {},
) {
    val state by screenModel.state.collectAsState()
    val spacing = TbcTheme.spacing
    val errorMessage = state.errorMessage

    LaunchedEffect(focusLocationId) {
        val locationId = focusLocationId ?: return@LaunchedEffect
        screenModel.onFocusLocation(locationId)
        onFocusHandled()
    }

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenH)
                .padding(top = spacing.xxl, bottom = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            ScreenTitle(title = "Map", subtitle = state.venueName)
            if (state.banner == MapBanner.OFFLINE_SHOWING_CACHE) {
                // Warning, not Error: the plan below is real and usable, it is
                // only possibly out of date.
                Banner(
                    title = "Offline",
                    text = state.lastSyncedLabel
                        ?.let { "Showing the saved map, last updated $it." }
                        ?: "Showing the saved map.",
                    style = BannerStyle.Warning,
                )
            }
            if (!state.isLoading && !state.isMapEmpty && errorMessage == null) {
                TbcSearchField(
                    value = state.query,
                    onValueChange = screenModel::onQueryChange,
                    placeholder = "Find a room…",
                    onClear = screenModel::onClearQuery,
                )
                if (state.hasMultipleLevels) {
                    val tabs = remember(state.levels) { state.levels.map { PillTab(it.name) } }
                    PillTabRow(
                        tabs = tabs,
                        selectedIndex = state.selectedLevelIndex,
                        onSelect = screenModel::onLevelSelected,
                    )
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            val level = state.selectedLevel
            when {
                state.isLoading -> MapLoading()
                errorMessage != null -> MapError(errorMessage, screenModel::onRetry)
                state.isMapEmpty || level == null -> MapNotPublished()
                else -> VenueMapCanvas(
                    level = level,
                    selectedFeatureId = state.selection?.featureId,
                    onFeatureSelect = screenModel::onFeatureSelected,
                )
            }

            // Results float over the plan rather than displacing it: the map
            // has to stay visible while you search it, or picking the right
            // "Restrooms" of three is guesswork.
            if (state.isSearching) {
                SearchResults(
                    results = state.searchResults,
                    onSelect = screenModel::onSearchResultSelected,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = spacing.screenH),
                )
            }

            state.selection?.let { selection ->
                SelectionSheet(
                    selection = selection,
                    onDismiss = { screenModel.onFeatureSelected(null) },
                    onEventClick = onEventClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = spacing.screenH)
                        .padding(bottom = spacing.lg),
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<MapSearchResult>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    // Raised, not a plain card: this overlays the plan, and the surface ladder
    // is what says so.
    TbcRaisedCard(modifier = modifier.fillMaxWidth(), padding = spacing.sm) {
        if (results.isEmpty()) {
            Text(
                text = "No place matches that name.",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textMuted,
                modifier = Modifier.padding(spacing.sm),
            )
            return@TbcRaisedCard
        }
        Column(
            modifier = Modifier
                .heightIn(max = SearchResultsMaxHeight)
                .verticalScroll(rememberScrollState()),
        ) {
            results.forEach { result ->
                MapSessionRow(
                    // The floor is the disambiguator, so it leads — three
                    // "Restrooms" rows are only tellable apart by it.
                    label = result.levelName,
                    title = result.name,
                    timeLabel = if (result.onAnotherLevel) "Another floor" else "This floor",
                    onClick = { onSelect(result.featureId) },
                    modifier = Modifier.padding(spacing.xxs),
                )
            }
        }
    }
}

@Composable
private fun SelectionSheet(
    selection: MapSelection,
    onDismiss: () -> Unit,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    TbcCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = spacing.touchTarget),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Text(
                        text = selection.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.textPrimary,
                    )
                    Text(
                        text = selection.levelName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textMuted,
                    )
                }
                TbcIconButton(
                    icon = TbcIcons.Close,
                    contentDescription = "Clear selection",
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }

            // The badge is what stops colour being the only thing saying what
            // kind of place this is — the fill on the plan is the other half.
            TbcBadge(text = selection.category.label(), style = TbcBadgeStyle.Neutral)

            selection.nowSession?.let { session ->
                MapSessionRow(
                    label = "Now",
                    title = session.title,
                    timeLabel = session.timeLabel,
                    onClick = { onEventClick(session.eventId) },
                )
            }
            selection.nextSession?.let { session ->
                MapSessionRow(
                    label = "Next",
                    title = session.title,
                    timeLabel = session.timeLabel,
                    onClick = { onEventClick(session.eventId) },
                )
            }
            // Only for rooms that host sessions. Saying "nothing scheduled" of
            // a staircase is noise.
            if (selection.hostsSessions && selection.nowSession == null && selection.nextSession == null) {
                Text(
                    text = "Nothing else scheduled here today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textMuted,
                )
            }
        }
    }
}

/** Bounded so a search matching everything cannot cover the whole plan. */
private val SearchResultsMaxHeight = 240.dp

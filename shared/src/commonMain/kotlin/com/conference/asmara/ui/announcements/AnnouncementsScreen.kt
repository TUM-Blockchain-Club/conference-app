package com.conference.asmara.ui.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.conference.asmara.domain.model.AnnouncementCategory
import com.conference.asmara.ui.announcements.components.AnnouncementRow
import com.conference.asmara.ui.announcements.components.AnnouncementsAllMuted
import com.conference.asmara.ui.announcements.components.AnnouncementsEmpty
import com.conference.asmara.ui.announcements.components.AnnouncementsError
import com.conference.asmara.ui.announcements.components.AnnouncementsLoading
import com.conference.asmara.ui.announcements.components.NotificationSettingsCard
import com.conference.asmara.ui.components.Banner
import com.conference.asmara.ui.components.BannerStyle
import com.conference.asmara.ui.components.ScreenFooter
import com.conference.asmara.ui.components.ScreenTitle
import com.conference.asmara.ui.components.SectionHeader
import com.conference.asmara.ui.components.TbcScaffold
import com.conference.asmara.ui.theme.TbcTheme
import org.koin.compose.getKoin

/**
 * The News tab, as a standalone `Screen`.
 *
 * The tab shell renders [AnnouncementsContent] directly rather than pushing
 * this, for the inset reason spelled out in `RootScreen`. Kept, like
 * `MapScreen`, because a `Screen` is what a notification tap or deep link can
 * push.
 */
class AnnouncementsScreen : Screen {
    @Composable
    override fun Content() {
        // Hoisted: rememberScreenModel's lambda is @DisallowComposableCalls.
        val koin = getKoin()
        TbcScaffold {
            AnnouncementsContent(
                screenModel = rememberScreenModel { koin.get<AnnouncementsScreenModel>() },
            )
        }
    }
}

/**
 * The News tab's body, without a scaffold of its own — nesting a second
 * `TbcScaffold` inside the shell's would apply the safe-drawing insets twice.
 *
 * Telling the model when the feed is on screen is this composable's job, not
 * the shell's: being composed is exactly "on screen" here, since the shell
 * renders one tab at a time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsContent(screenModel: AnnouncementsScreenModel, modifier: Modifier = Modifier) {
    val state by screenModel.state.collectAsState()
    val errorMessage = state.errorMessage
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    DisposableEffect(screenModel) {
        screenModel.onShown()
        onDispose { screenModel.onHidden() }
    }

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenH)
                .padding(top = spacing.xxl, bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            ScreenTitle(title = "News", subtitle = state.subtitle())
            if (state.banner == AnnouncementsBanner.OFFLINE_SHOWING_CACHE) {
                // Warning, not Error: the feed below is real, only possibly
                // missing the newest message.
                Banner(
                    title = "Offline",
                    text = state.lastSyncedLabel
                        ?.let { "Showing saved announcements, last updated $it." }
                        ?: "Showing saved announcements.",
                    style = BannerStyle.Warning,
                )
            }
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
                state.isLoading -> AnnouncementsLoading()

                errorMessage != null -> AnnouncementsError(errorMessage, screenModel::onRetry)

                else -> AnnouncementFeed(
                    state = state,
                    onToggle = screenModel::onCategoryToggle,
                    onShowAll = screenModel::onEnableAllCategories,
                )
            }
        }
    }
}

/**
 * The feed, then the settings. The settings live in the same scroll — not
 * behind a header button — because "everything is muted" has to be fixable from
 * the screen that says so.
 */
@Composable
private fun AnnouncementFeed(
    state: AnnouncementsUiState,
    onToggle: (AnnouncementCategory, Boolean) -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = TbcTheme.spacing

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.screenH, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        when {
            state.isEmpty -> item(key = "empty") {
                AnnouncementsEmpty(Modifier.fillParentMaxHeight(EmptyStateFraction))
            }

            state.isAllMuted -> item(key = "muted") {
                AnnouncementsAllMuted(onShowAll, Modifier.fillParentMaxHeight(EmptyStateFraction))
            }

            else -> items(state.announcements, key = { it.id }) { item ->
                AnnouncementRow(item)
            }
        }
        item(key = "settings-header") {
            SectionHeader(
                title = "Notifications",
                subtitle = "Choose what you hear about.",
                modifier = Modifier.padding(top = spacing.xxl),
            )
        }
        item(key = "settings") {
            NotificationSettingsCard(categories = state.categories, onToggle = onToggle)
        }
        item(key = "footer") {
            ScreenFooter(
                text = "TUM Blockchain Club",
                modifier = Modifier.padding(top = spacing.x3l, bottom = spacing.lg),
            )
        }
    }
}

private fun AnnouncementsUiState.subtitle(): String? = when {
    isLoading -> null
    unreadCount == 1 -> "1 unread"
    unreadCount > 1 -> "$unreadCount unread"
    announcements.isNotEmpty() -> "You're up to date"
    else -> null
}

/**
 * Not the full height: the settings card sits below the empty state, and has
 * to be reachable without a scroll on a phone for the "all muted" case.
 */
private const val EmptyStateFraction = 0.6f

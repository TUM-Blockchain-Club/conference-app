package com.conference.asmara.ui.announcements

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.conference.asmara.domain.model.Announcement
import com.conference.asmara.domain.model.AnnouncementCategory
import com.conference.asmara.domain.repository.AnnouncementRepository
import com.conference.asmara.domain.repository.NotificationPreferencesRepository
import com.conference.asmara.domain.repository.Outcome
import com.conference.asmara.domain.repository.SyncResult
import com.conference.asmara.push.PushRegistrar
import com.conference.asmara.ui.common.ConferenceTimeZone
import com.conference.asmara.ui.common.syncedAtLabel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * The News tab: the announcement feed, the unread badge, and the per-category
 * preferences.
 *
 * Created by the tab shell at launch rather than on first visit, unlike the
 * Schedule and Map models: the shell needs the unread count for the tab badge
 * before anyone has opened the tab, and the first refresh is exactly what
 * produces it.
 *
 * Free of Voyager types beyond the base class and [screenModelScope], for the
 * same reason as `ScheduleScreenModel`.
 */
class AnnouncementsScreenModel(
    private val repository: AnnouncementRepository,
    private val preferences: NotificationPreferencesRepository,
    private val pushRegistrar: PushRegistrar,
    private val zone: TimeZone = ConferenceTimeZone,
    // Injected so tests can put derivation on the test scheduler.
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StateScreenModel<AnnouncementsUiState>(AnnouncementsUiState()) {

    /** True while the feed is on screen. */
    private val visible = MutableStateFlow(false)

    /** Ids that were unread at some point during the current visit — see [AnnouncementItem.isNew]. */
    private val seenThisVisit = MutableStateFlow<Set<String>>(emptySet())

    private var loadJob: Job? = null
    private var permissionAsked = false

    init {
        screenModelScope.launch {
            combine(
                repository.observeAnnouncements(),
                preferences.observeEnabledCategories(),
                seenThisVisit,
                ::Triple,
            )
                .flowOn(defaultDispatcher)
                .collect { (all, enabled, seen) -> render(all, enabled, seen) }
        }
        // Anything that is in the feed while the feed is on screen has been
        // seen — including an announcement a push refresh drops in while the
        // user is looking at it. Marking read re-emits with nothing unread,
        // which is what stops this from looping.
        screenModelScope.launch {
            combine(visible, repository.observeAnnouncements()) { isVisible, all ->
                if (isVisible) all.filterNot { it.isRead }.map { it.id } else emptyList()
            }.collect { unreadIds ->
                if (unreadIds.isEmpty()) return@collect
                seenThisVisit.update { it + unreadIds }
                repository.markAllRead()
            }
        }
        load(force = false, userInitiated = false)
    }

    /**
     * The feed came on screen. Clears the badge; keeps the "New" markers for
     * this visit.
     *
     * Also where the notification permission is asked for, once per process:
     * on first opening the feed the prompt has a context, where at launch it
     * would be a dialog over a schedule the user came to read. A no-op with
     * push disabled — the registrar is then the no-op one.
     */
    fun onShown() {
        visible.value = true
        if (!permissionAsked) {
            permissionAsked = true
            screenModelScope.launch { pushRegistrar.requestPermission() }
        }
    }

    /** The feed left the screen. The next visit starts with no "New" markers but the genuinely new. */
    fun onHidden() {
        visible.value = false
        seenThisVisit.value = emptySet()
    }

    fun onCategoryToggle(category: AnnouncementCategory, enabled: Boolean) {
        screenModelScope.launch { preferences.setCategoryEnabled(category, enabled) }
    }

    /** The way out of "everything is muted", from the empty state's button. */
    fun onEnableAllCategories() {
        screenModelScope.launch {
            AnnouncementCategory.entries.forEach { preferences.setCategoryEnabled(it, true) }
        }
    }

    fun onPullToRefresh() = load(force = true, userInitiated = true)

    fun onRetry() = load(force = true, userInitiated = false)

    private fun load(force: Boolean, userInitiated: Boolean) {
        if (loadJob?.isActive == true) return
        if (userInitiated) mutableState.update { it.copy(isRefreshing = true) }
        loadJob = screenModelScope.launch {
            repository.refresh(force)
                .onSuccess { applySyncResult(it) }
                .onFailure { applySyncFailure() }
            mutableState.update { it.copy(isRefreshing = false, initialSyncDone = true) }
        }
    }

    private fun render(all: List<Announcement>, enabled: Set<AnnouncementCategory>, seen: Set<String>) {
        val shown = all.filter { it.category in enabled }
        mutableState.update { previous ->
            previous.copy(
                announcements = shown.map { announcement ->
                    AnnouncementItem(
                        id = announcement.id,
                        title = announcement.title,
                        body = announcement.body,
                        category = announcement.category,
                        timeLabel = announcement.publishedAt.syncedAtLabel(zone),
                        isNew = !announcement.isRead || announcement.id in seen,
                    )
                },
                totalCount = all.size,
                unreadCount = shown.count { !it.isRead },
                categories = AnnouncementCategory.entries.map { category ->
                    CategoryToggle(category, enabled = category in enabled, locked = !category.canOptOut)
                },
            )
        }
    }

    private suspend fun applySyncResult(result: SyncResult) {
        when (result.outcome) {
            Outcome.REFRESHED, Outcome.SKIPPED_FRESH -> mutableState.update {
                it.copy(
                    banner = null,
                    errorMessage = null,
                    lastSyncedLabel = result.syncedAt?.syncedAtLabel(zone),
                )
            }

            Outcome.FAILED_SERVING_CACHE -> mutableState.update {
                // syncedAt is the *old* timestamp here — exactly what the banner needs.
                it.copy(
                    banner = AnnouncementsBanner.OFFLINE_SHOWING_CACHE,
                    errorMessage = null,
                    lastSyncedLabel = result.syncedAt?.syncedAtLabel(zone),
                )
            }

            // AnnouncementRepositoryImpl never produces this; mapping it to the
            // same error state keeps the `when` exhaustive without an `else`.
            Outcome.FAILED_NO_CACHE -> applySyncFailure()
        }
    }

    // Reachable only with an empty cache — a failure with anything cached comes
    // back as success(FAILED_SERVING_CACHE).
    private suspend fun applySyncFailure() {
        val lastSynced = runCatching { repository.lastSyncedAt() }.getOrNull()
        mutableState.update {
            it.copy(
                banner = null,
                errorMessage = "Check your connection and try again.",
                lastSyncedLabel = lastSynced?.syncedAtLabel(zone),
            )
        }
    }
}

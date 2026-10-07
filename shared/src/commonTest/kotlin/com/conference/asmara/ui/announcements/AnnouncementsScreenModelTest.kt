package com.conference.asmara.ui.announcements

import com.conference.asmara.domain.model.Announcement
import com.conference.asmara.domain.model.AnnouncementCategory
import com.conference.asmara.domain.repository.AnnouncementRepository
import com.conference.asmara.domain.repository.NotificationPreferencesRepository
import com.conference.asmara.domain.repository.Outcome
import com.conference.asmara.domain.repository.SyncResult
import com.conference.asmara.push.PushRegistrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private class FakeAnnouncementRepository(initial: List<Announcement> = emptyList()) : AnnouncementRepository {
    val announcements = MutableStateFlow(initial)
    var nextResult: Result<SyncResult> = Result.success(SyncResult(Outcome.REFRESHED, null, 0))
    var refreshCalls = mutableListOf<Boolean>()
        private set

    override fun observeAnnouncements(): Flow<List<Announcement>> = announcements

    override suspend fun refresh(force: Boolean): Result<SyncResult> {
        refreshCalls += force
        return nextResult
    }

    override suspend fun lastSyncedAt(): Instant? = null

    override suspend fun markAllRead() {
        announcements.update { list -> list.map { it.copy(isRead = true) } }
    }
}

/** Behaves like the real one: opt-out, and emergency cannot be turned off. */
private class FakeNotificationPreferences : NotificationPreferencesRepository {
    private val disabled = MutableStateFlow<Set<AnnouncementCategory>>(emptySet())
    val enabled = MutableStateFlow(AnnouncementCategory.entries.toSet())

    override fun observeEnabledCategories(): Flow<Set<AnnouncementCategory>> = enabled

    override suspend fun setCategoryEnabled(category: AnnouncementCategory, enabled: Boolean) {
        if (!category.canOptOut) return
        disabled.update { if (enabled) it - category else it + category }
        this.enabled.value = AnnouncementCategory.entries.toSet() - disabled.value
    }
}

private class CountingPushRegistrar : PushRegistrar {
    var permissionRequests = 0
        private set

    override suspend fun currentToken(): String? = null

    override suspend fun requestPermission(): Boolean {
        permissionRequests++
        return true
    }

    override val tokenUpdates: Flow<String> = emptyFlow()
}

private fun announcement(
    id: String,
    category: AnnouncementCategory = AnnouncementCategory.GENERAL,
    isRead: Boolean = false,
    publishedAt: String = "2026-09-01T08:00:00Z",
) = Announcement(
    id = id,
    slug = id,
    title = "Title $id",
    body = "Body $id",
    category = category,
    publishedAt = Instant.parse(publishedAt),
    isRead = isRead,
)

private fun TestScope.model(
    repository: AnnouncementRepository,
    preferences: NotificationPreferencesRepository = FakeNotificationPreferences(),
    pushRegistrar: PushRegistrar = CountingPushRegistrar(),
) = AnnouncementsScreenModel(
    repository = repository,
    preferences = preferences,
    pushRegistrar = pushRegistrar,
    defaultDispatcher = StandardTestDispatcher(testScheduler),
)

class AnnouncementsScreenModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun coldStartShowsLoadingThenContent() = runTest {
        val repository = FakeAnnouncementRepository()
        val model = model(repository)

        assertTrue(model.state.value.isLoading)

        repository.announcements.value = listOf(announcement("a"), announcement("b"))
        testScheduler.advanceUntilIdle()

        val state = model.state.value
        assertFalse(state.isLoading)
        assertEquals(listOf("a", "b"), state.announcements.map { it.id })
        assertNull(state.errorMessage)
        assertEquals(listOf(false), repository.refreshCalls, "the first load honours the TTL")
    }

    @Test
    fun unreadCountDrivesTheTabBadge() = runTest {
        val repository = FakeAnnouncementRepository(
            listOf(announcement("a"), announcement("b"), announcement("c", isRead = true)),
        )
        val model = model(repository)
        testScheduler.advanceUntilIdle()

        assertEquals(2, model.state.value.unreadCount)
        assertEquals("2", model.state.value.unreadBadge)
    }

    @Test
    fun aMutedCategoryNeverBadgesTheTab() = runTest {
        val preferences = FakeNotificationPreferences()
        preferences.setCategoryEnabled(AnnouncementCategory.KEYNOTE, false)
        val repository = FakeAnnouncementRepository(listOf(announcement("k", AnnouncementCategory.KEYNOTE)))
        val model = model(repository, preferences)
        testScheduler.advanceUntilIdle()

        assertEquals(0, model.state.value.unreadCount)
        assertNull(model.state.value.unreadBadge)
    }

    @Test
    fun openingTheFeedClearsTheBadgeButKeepsTheNewMarkers() = runTest {
        val repository = FakeAnnouncementRepository(listOf(announcement("a"), announcement("b", isRead = true)))
        val model = model(repository)
        testScheduler.advanceUntilIdle()

        model.onShown()
        testScheduler.advanceUntilIdle()

        val state = model.state.value
        assertNull(state.unreadBadge)
        assertTrue(repository.announcements.value.all { it.isRead })
        // Still marked for this visit — otherwise it vanishes the instant it matters.
        assertEquals(mapOf("a" to true, "b" to false), state.announcements.associate { it.id to it.isNew })

        model.onHidden()
        testScheduler.advanceUntilIdle()
        assertTrue(model.state.value.announcements.none { it.isNew })
    }

    @Test
    fun anAnnouncementArrivingWhileTheFeedIsOpenIsMarkedReadButShownAsNew() = runTest {
        val repository = FakeAnnouncementRepository(listOf(announcement("a", isRead = true)))
        val model = model(repository)
        model.onShown()
        testScheduler.advanceUntilIdle()

        repository.announcements.update { it + announcement("b") }
        testScheduler.advanceUntilIdle()

        assertNull(model.state.value.unreadBadge)
        assertTrue(model.state.value.announcements.single { it.id == "b" }.isNew)
    }

    @Test
    fun aCategoryTogglePersistsAndReFilters() = runTest {
        val preferences = FakeNotificationPreferences()
        val repository = FakeAnnouncementRepository(
            listOf(announcement("g"), announcement("k", AnnouncementCategory.KEYNOTE)),
        )
        val model = model(repository, preferences)
        testScheduler.advanceUntilIdle()

        model.onCategoryToggle(AnnouncementCategory.KEYNOTE, false)
        testScheduler.advanceUntilIdle()

        assertFalse(AnnouncementCategory.KEYNOTE in preferences.enabled.value)
        assertEquals(listOf("g"), model.state.value.announcements.map { it.id })
        assertEquals(false, model.state.value.categories.single { it.category == AnnouncementCategory.KEYNOTE }.enabled)

        model.onCategoryToggle(AnnouncementCategory.KEYNOTE, true)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("g", "k"), model.state.value.announcements.map { it.id })
    }

    @Test
    fun emergencyIsAlwaysShownAndItsToggleIsLocked() = runTest {
        val repository = FakeAnnouncementRepository(listOf(announcement("e", AnnouncementCategory.EMERGENCY)))
        val model = model(repository)
        testScheduler.advanceUntilIdle()

        model.onCategoryToggle(AnnouncementCategory.EMERGENCY, false)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("e"), model.state.value.announcements.map { it.id })
        val toggle = model.state.value.categories.single { it.category == AnnouncementCategory.EMERGENCY }
        assertTrue(toggle.enabled)
        assertTrue(toggle.locked)
    }

    @Test
    fun mutingEverythingIsDistinctFromNothingPublished() = runTest {
        val preferences = FakeNotificationPreferences()
        val repository = FakeAnnouncementRepository(listOf(announcement("k", AnnouncementCategory.KEYNOTE)))
        val model = model(repository, preferences)
        testScheduler.advanceUntilIdle()

        model.onCategoryToggle(AnnouncementCategory.KEYNOTE, false)
        testScheduler.advanceUntilIdle()
        assertTrue(model.state.value.isAllMuted)
        assertFalse(model.state.value.isEmpty)

        model.onEnableAllCategories()
        testScheduler.advanceUntilIdle()
        assertFalse(model.state.value.isAllMuted)
        assertEquals(listOf("k"), model.state.value.announcements.map { it.id })
    }

    @Test
    fun theOfflineBannerClearsOnPullToRefresh() = runTest {
        val repository = FakeAnnouncementRepository(listOf(announcement("a")))
        repository.nextResult = Result.success(
            SyncResult(Outcome.FAILED_SERVING_CACHE, Instant.parse("2026-09-01T07:00:00Z"), 1),
        )
        val model = model(repository)
        testScheduler.advanceUntilIdle()

        assertEquals(AnnouncementsBanner.OFFLINE_SHOWING_CACHE, model.state.value.banner)
        assertNotNull(model.state.value.lastSyncedLabel)
        assertEquals(listOf("a"), model.state.value.announcements.map { it.id })

        repository.nextResult = Result.success(SyncResult(Outcome.REFRESHED, Instant.parse("2026-09-01T08:00:00Z"), 1))
        model.onPullToRefresh()
        assertTrue(model.state.value.isRefreshing)
        testScheduler.advanceUntilIdle()

        assertNull(model.state.value.banner)
        assertFalse(model.state.value.isRefreshing)
        assertEquals(listOf(false, true), repository.refreshCalls, "pull-to-refresh forces past the TTL")
    }

    @Test
    fun failureOnAnEmptyCacheIsAnErrorThatRetryClears() = runTest {
        val repository = FakeAnnouncementRepository()
        repository.nextResult = Result.failure(RuntimeException("network down"))
        val model = model(repository)
        testScheduler.advanceUntilIdle()

        assertNotNull(model.state.value.errorMessage)
        assertFalse(model.state.value.isLoading)

        repository.nextResult = Result.success(SyncResult(Outcome.REFRESHED, null, 1))
        model.onRetry()
        repository.announcements.value = listOf(announcement("a"))
        testScheduler.advanceUntilIdle()

        assertNull(model.state.value.errorMessage)
        assertEquals(1, model.state.value.announcements.size)
    }

    @Test
    fun notificationPermissionIsAskedOnFirstOpeningTheFeedAndOnlyThen() = runTest {
        val registrar = CountingPushRegistrar()
        val model = model(FakeAnnouncementRepository(), pushRegistrar = registrar)
        testScheduler.advanceUntilIdle()
        assertEquals(0, registrar.permissionRequests, "not at launch, before the feed has been seen")

        model.onShown()
        model.onHidden()
        model.onShown()
        testScheduler.advanceUntilIdle()

        assertEquals(1, registrar.permissionRequests)
    }

    @Test
    fun theBadgeIsCappedSoThePillStaysAPill() {
        assertEquals("99+", AnnouncementsUiState(unreadCount = 150).unreadBadge)
        assertEquals("99", AnnouncementsUiState(unreadCount = 99).unreadBadge)
    }
}

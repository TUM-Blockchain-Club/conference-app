package com.conference.asmara.ui.schedule

import com.conference.asmara.domain.model.Event
import com.conference.asmara.domain.model.EventType
import com.conference.asmara.domain.model.Track
import com.conference.asmara.domain.repository.Outcome
import com.conference.asmara.domain.repository.ScheduleRepository
import com.conference.asmara.domain.repository.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.time.Clock
import kotlin.time.Instant

private class FakeClock(var current: Instant) : Clock {
    override fun now(): Instant = current
}

private class FakeScheduleRepository(
    initial: List<Event> = emptyList(),
) : ScheduleRepository {
    val events = MutableStateFlow(initial)
    var nextResult: Result<SyncResult> = Result.success(SyncResult(Outcome.REFRESHED, null, 0))
    var lastSyncedAtValue: Instant? = null
    var refreshCallCount = 0
        private set

    override fun observeSchedule(): Flow<List<Event>> = events

    override suspend fun refresh(force: Boolean): Result<SyncResult> {
        refreshCallCount++
        return nextResult
    }

    override suspend fun lastSyncedAt(): Instant? = lastSyncedAtValue
}

private val platform = Track("t1", "platform", "Platform", null, "#4F46E5", 0)

private fun event(
    id: String,
    title: String = "Session $id",
    start: String = "2026-09-01T09:00:00Z",
    end: String = "2026-09-01T09:45:00Z",
    isPublished: Boolean = true,
) = Event(
    id = id,
    slug = id,
    title = title,
    description = null,
    startTime = Instant.parse(start),
    endTime = Instant.parse(end),
    track = platform,
    location = null,
    eventType = EventType.TALK,
    isPublished = isPublished,
    speakers = emptyList(),
)

/**
 * Both the model's own scope (Main) and its filtering dispatcher have to run on
 * the test scheduler, or `advanceUntilIdle()` returns before the derived state
 * has been produced.
 */
private fun TestScope.scheduleScreenModel(repository: ScheduleRepository, now: String) =
    ScheduleScreenModel(
        repository = repository,
        clock = FakeClock(Instant.parse(now)),
        defaultDispatcher = StandardTestDispatcher(testScheduler),
    )

class ScheduleScreenModelTest {

    // screenModelScope runs on Dispatchers.Main, which has no default binding
    // in a multiplatform test process. Setting it to a TestDispatcher also makes
    // runTest adopt that dispatcher's scheduler.
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
        val repository = FakeScheduleRepository()
        val model = scheduleScreenModel(repository, "2026-09-01T00:00:00Z")

        assertTrue(model.state.value.isLoading)

        repository.nextResult = Result.success(SyncResult(Outcome.REFRESHED, Instant.parse("2026-09-01T00:00:00Z"), 2))
        repository.events.value = listOf(event("a"), event("b"))
        testScheduler.advanceUntilIdle()

        val state = model.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.days.size)
        assertEquals(2, state.totalEventCount)
        assertNull(state.errorMessage)
        assertNull(state.banner)
    }

    @Test
    fun unpublishedEventsNeverReachTheUi() = runTest {
        val repository = FakeScheduleRepository(listOf(event("a"), event("draft", isPublished = false)))
        val model = scheduleScreenModel(repository, "2026-09-01T00:00:00Z")
        testScheduler.advanceUntilIdle()

        assertEquals(1, model.state.value.totalEventCount)
        assertEquals(listOf("a"), model.state.value.days.flatMap { it.slots }.flatMap { s -> s.events.map { it.id } })
    }

    @Test
    fun failedRefreshServingCacheSetsTheBannerAndKeepsTheContent() = runTest {
        val repository = FakeScheduleRepository(listOf(event("a")))
        val staleSync = Instant.parse("2026-08-31T18:00:00Z")
        repository.nextResult = Result.success(SyncResult(Outcome.FAILED_SERVING_CACHE, staleSync, 1))
        val model = scheduleScreenModel(repository, "2026-09-01T00:00:00Z")
        testScheduler.advanceUntilIdle()

        val state = model.state.value
        assertEquals(ScheduleBanner.OFFLINE_SHOWING_CACHE, state.banner)
        assertNull(state.errorMessage)
        assertEquals(1, state.days.size)
        assertNotNull(state.lastSyncedLabel)
    }

    @Test
    fun failureOnAnEmptyCacheSurfacesAnErrorThatRetryClears() = runTest {
        val repository = FakeScheduleRepository()
        repository.nextResult = Result.failure(RuntimeException("network down"))
        val model = scheduleScreenModel(repository, "2026-09-01T00:00:00Z")
        testScheduler.advanceUntilIdle()

        assertNotNull(model.state.value.errorMessage)
        assertFalse(model.state.value.isLoading)

        repository.nextResult = Result.success(SyncResult(Outcome.REFRESHED, Instant.parse("2026-09-01T00:05:00Z"), 1))
        model.onRetry()
        repository.events.value = listOf(event("a"))
        testScheduler.advanceUntilIdle()

        assertNull(model.state.value.errorMessage)
        assertEquals(1, model.state.value.days.size)
        assertEquals(2, repository.refreshCallCount)
    }

    @Test
    fun filterChangesReDeriveDaysWithoutHittingTheRepositoryAgain() = runTest {
        val repository = FakeScheduleRepository(listOf(event("a", title = "Kotlin"), event("b", title = "Rust")))
        val model = scheduleScreenModel(repository, "2026-09-01T00:00:00Z")
        testScheduler.advanceUntilIdle()
        assertEquals(1, repository.refreshCallCount)

        model.onQueryChange("kotlin")
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("a"), model.state.value.days.flatMap { it.slots }.flatMap { s -> s.events.map { it.id } })
        assertEquals(2, model.state.value.totalEventCount)
        assertFalse(model.state.value.isEmptyResult)
        assertEquals(1, repository.refreshCallCount)
    }

    @Test
    fun aQueryMatchingNothingIsAnEmptyResultNotAnEmptySchedule() = runTest {
        val repository = FakeScheduleRepository(listOf(event("a", title = "Kotlin")))
        val model = scheduleScreenModel(repository, "2026-09-01T00:00:00Z")
        testScheduler.advanceUntilIdle()

        model.onQueryChange("nothing matches this")
        testScheduler.advanceUntilIdle()

        assertTrue(model.state.value.isEmptyResult)
        assertFalse(model.state.value.isScheduleEmpty)
    }

    @Test
    fun anEmptyPublishedScheduleIsDistinguishableFromLoading() = runTest {
        val repository = FakeScheduleRepository()
        val model = scheduleScreenModel(repository, "2026-09-01T00:00:00Z")
        testScheduler.advanceUntilIdle()

        assertTrue(model.state.value.isScheduleEmpty)
        assertFalse(model.state.value.isLoading)
    }

    @Test
    fun trackTogglesAccumulateAndClearFiltersResetsEverything() = runTest {
        val security = Track("t2", "security", "Security", null, null, 1)
        val repository = FakeScheduleRepository(
            listOf(event("a"), event("b").copy(track = security)),
        )
        val model = scheduleScreenModel(repository, "2026-09-01T00:00:00Z")
        testScheduler.advanceUntilIdle()

        model.onTrackToggle("t1")
        model.onTrackToggle("t2")
        testScheduler.advanceUntilIdle()
        assertEquals(setOf("t1", "t2"), model.state.value.filters.trackIds)

        model.onTrackToggle("t1")
        testScheduler.advanceUntilIdle()
        assertEquals(setOf("t2"), model.state.value.filters.trackIds)

        model.onUpcomingToggle()
        model.onClearFilters()
        testScheduler.advanceUntilIdle()
        assertEquals(ScheduleFilters(), model.state.value.filters)
        assertEquals(2, model.state.value.days.flatMap { it.slots }.flatMap { it.events }.size)
    }

    @Test
    fun tracksComeFromUnfilteredDataSoChipsSurviveTheirOwnFilter() = runTest {
        val security = Track("t2", "security", "Security", null, null, 1)
        val repository = FakeScheduleRepository(
            listOf(event("a"), event("b").copy(track = security)),
        )
        val model = scheduleScreenModel(repository, "2026-09-01T00:00:00Z")
        testScheduler.advanceUntilIdle()

        model.onTrackToggle("t1")
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("Platform", "Security"), model.state.value.tracks.map { it.name })
    }

    @Test
    fun upcomingOnlyUsesTheInjectedClock() = runTest {
        val repository = FakeScheduleRepository(
            listOf(
                event("past", start = "2026-09-01T08:00:00Z", end = "2026-09-01T08:45:00Z"),
                event("future", start = "2026-09-01T14:00:00Z", end = "2026-09-01T14:45:00Z"),
            ),
        )
        val model = scheduleScreenModel(repository, "2026-09-01T10:00:00Z")
        testScheduler.advanceUntilIdle()

        model.onUpcomingToggle()
        testScheduler.advanceUntilIdle()

        assertEquals(
            listOf("future"),
            model.state.value.days.flatMap { it.slots }.flatMap { s -> s.events.map { it.id } },
        )
    }

    @Test
    fun pullToRefreshClearsAStaleOfflineBanner() = runTest {
        val repository = FakeScheduleRepository(listOf(event("a")))
        repository.nextResult = Result.success(
            SyncResult(Outcome.FAILED_SERVING_CACHE, Instant.parse("2026-08-31T18:00:00Z"), 1),
        )
        val model = scheduleScreenModel(repository, "2026-09-01T00:00:00Z")
        testScheduler.advanceUntilIdle()
        assertEquals(ScheduleBanner.OFFLINE_SHOWING_CACHE, model.state.value.banner)

        repository.nextResult = Result.success(SyncResult(Outcome.REFRESHED, Instant.parse("2026-09-01T00:05:00Z"), 1))
        model.onPullToRefresh()
        testScheduler.advanceUntilIdle()

        assertNull(model.state.value.banner)
        assertFalse(model.state.value.isRefreshing)
    }
}

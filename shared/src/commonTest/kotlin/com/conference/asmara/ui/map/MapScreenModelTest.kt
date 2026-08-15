package com.conference.asmara.ui.map

import com.conference.asmara.domain.model.Event
import com.conference.asmara.domain.model.EventType
import com.conference.asmara.domain.model.FeatureShape
import com.conference.asmara.domain.model.Location
import com.conference.asmara.domain.model.MapCategory
import com.conference.asmara.domain.model.MapFeature
import com.conference.asmara.domain.model.Point
import com.conference.asmara.domain.model.Polygon
import com.conference.asmara.domain.model.VenueLevel
import com.conference.asmara.domain.model.VenueMap
import com.conference.asmara.domain.repository.Outcome
import com.conference.asmara.domain.repository.ScheduleRepository
import com.conference.asmara.domain.repository.SyncResult
import com.conference.asmara.domain.repository.VenueMapRepository
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

private class FakeVenueMapRepository(initial: VenueMap? = null) : VenueMapRepository {
    val map = MutableStateFlow(initial)
    var nextResult: Result<SyncResult> = Result.success(SyncResult(Outcome.REFRESHED, null, 0))
    var lastSyncedAtValue: Instant? = null
    var refreshCallCount = 0
        private set

    override fun observeVenueMap(): Flow<VenueMap?> = map

    override suspend fun refresh(force: Boolean): Result<SyncResult> {
        refreshCallCount++
        return nextResult
    }

    override suspend fun lastSyncedAt(): Instant? = lastSyncedAtValue
}

private class FakeScheduleRepository(initial: List<Event> = emptyList()) : ScheduleRepository {
    val events = MutableStateFlow(initial)
    override fun observeSchedule(): Flow<List<Event>> = events
    override suspend fun refresh(force: Boolean): Result<SyncResult> =
        Result.success(SyncResult(Outcome.REFRESHED, null, 0))
    override suspend fun lastSyncedAt(): Instant? = null
}

private fun rect(x0: Float, y0: Float, x1: Float, y1: Float) = Polygon(
    listOf(Point(x0, y0), Point(x1, y0), Point(x1, y1), Point(x0, y1)),
)

private fun feature(
    id: String,
    name: String,
    locationId: String? = null,
    category: MapCategory = MapCategory.ROOM,
) = MapFeature(
    id = id,
    slug = id,
    name = name,
    category = category,
    locationId = locationId,
    shape = FeatureShape.Area(rect(0f, 0f, 10f, 10f)),
    labelAnchor = null,
    sortOrder = 0,
)

private val mainStage = feature("f-stage", "Main Stage", locationId = "loc-stage", category = MapCategory.STAGE)
private val roomA = feature("f-room-a", "Workshop Room A", locationId = "loc-room-a")
private val groundRestrooms = feature("f-wc-0", "Restrooms", category = MapCategory.RESTROOM)
private val firstRestrooms = feature("f-wc-1", "Restrooms", category = MapCategory.RESTROOM)

private val venue = VenueMap(
    id = "v1",
    slug = "venue",
    name = "TBC Conference Venue",
    levels = listOf(
        VenueLevel("l0", "ground", "Ground", 0, rect(0f, 0f, 40f, 30f), listOf(mainStage, groundRestrooms)),
        VenueLevel("l1", "first", "1st Floor", 1, rect(0f, 0f, 40f, 30f), listOf(roomA, firstRestrooms)),
    ),
)

private fun event(
    id: String,
    title: String,
    locationId: String?,
    start: String,
    end: String,
    isPublished: Boolean = true,
) = Event(
    id = id,
    slug = id,
    title = title,
    description = null,
    startTime = Instant.parse(start),
    endTime = Instant.parse(end),
    track = null,
    location = locationId?.let { Location(it, it, it, null, null) },
    eventType = EventType.TALK,
    isPublished = isPublished,
    speakers = emptyList(),
)

/**
 * Both the model's own scope (Main) and its derivation dispatcher have to run
 * on the test scheduler, or `advanceUntilIdle()` returns before the derived
 * state has been produced.
 */
private fun TestScope.mapScreenModel(
    venueMapRepository: VenueMapRepository,
    scheduleRepository: ScheduleRepository,
    now: String = "2026-09-01T10:00:00Z",
) = MapScreenModel(
    venueMapRepository = venueMapRepository,
    scheduleRepository = scheduleRepository,
    clock = FakeClock(Instant.parse(now)),
    defaultDispatcher = StandardTestDispatcher(testScheduler),
)

class MapScreenModelTest {

    // screenModelScope runs on Dispatchers.Main, which has no default binding
    // in a multiplatform test process.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun coldStartShowsLoadingThenTheGroundFloor() = runTest {
        val venueMapRepository = FakeVenueMapRepository()
        val model = mapScreenModel(venueMapRepository, FakeScheduleRepository())

        assertTrue(model.state.value.isLoading)

        venueMapRepository.map.value = venue
        testScheduler.advanceUntilIdle()

        val state = model.state.value
        assertFalse(state.isLoading)
        assertEquals("TBC Conference Venue", state.venueName)
        assertEquals(2, state.levels.size)
        assertEquals(0, state.selectedLevelIndex)
        assertEquals("Ground", state.selectedLevel?.name)
        assertTrue(state.hasMultipleLevels)
    }

    @Test
    fun anEmptyVenueIsDistinguishableFromLoading() = runTest {
        val model = mapScreenModel(FakeVenueMapRepository(), FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        assertTrue(model.state.value.isMapEmpty)
        assertFalse(model.state.value.isLoading)
    }

    @Test
    fun levelSelectionSwitchesFloorsAndKeepsTheSelection() = runTest {
        val model = mapScreenModel(FakeVenueMapRepository(venue), FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        model.onFeatureSelected(mainStage.id)
        model.onLevelSelected(1)
        testScheduler.advanceUntilIdle()

        assertEquals("1st Floor", model.state.value.selectedLevel?.name)
        // Still highlighted: going upstairs to look and coming back should not
        // have cleared it.
        assertEquals(mainStage.id, model.state.value.selection?.featureId)
    }

    @Test
    fun tappingBareFloorClearsTheSelection() = runTest {
        val model = mapScreenModel(FakeVenueMapRepository(venue), FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        model.onFeatureSelected(mainStage.id)
        testScheduler.advanceUntilIdle()
        assertNotNull(model.state.value.selection)

        model.onFeatureSelected(null)
        testScheduler.advanceUntilIdle()
        assertNull(model.state.value.selection)
    }

    @Test
    fun theSelectionCarriesTheCategoryAndFloor() = runTest {
        val model = mapScreenModel(FakeVenueMapRepository(venue), FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        model.onFeatureSelected(firstRestrooms.id)
        testScheduler.advanceUntilIdle()

        val selection = model.state.value.selection!!
        assertEquals("Restrooms", selection.name)
        assertEquals(MapCategory.RESTROOM, selection.category)
        assertEquals("1st Floor", selection.levelName)
        assertFalse(selection.hostsSessions)
    }

    @Test
    fun searchSpansEveryFloorAndFlagsTheOnesElsewhere() = runTest {
        val model = mapScreenModel(FakeVenueMapRepository(venue), FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        model.onQueryChange("restroom")
        testScheduler.advanceUntilIdle()

        val results = model.state.value.searchResults
        assertEquals(2, results.size)
        assertEquals(listOf("Ground", "1st Floor"), results.map { it.levelName })
        assertEquals(listOf(false, true), results.map { it.onAnotherLevel })
    }

    @Test
    fun choosingASearchResultJumpsToItsFloorHighlightsItAndDropsTheQuery() = runTest {
        val model = mapScreenModel(FakeVenueMapRepository(venue), FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        model.onQueryChange("Workshop")
        testScheduler.advanceUntilIdle()
        val hit = model.state.value.searchResults.single()

        model.onSearchResultSelected(hit.featureId)
        testScheduler.advanceUntilIdle()

        val state = model.state.value
        assertEquals("", state.query)
        assertFalse(state.isSearching)
        assertEquals(1, state.selectedLevelIndex)
        assertEquals(roomA.id, state.selection?.featureId)
    }

    @Test
    fun aQueryMatchingNothingIsAnEmptyResultNotAnEmptyMap() = runTest {
        val model = mapScreenModel(FakeVenueMapRepository(venue), FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        model.onQueryChange("helipad")
        testScheduler.advanceUntilIdle()

        assertTrue(model.state.value.isSearching)
        assertTrue(model.state.value.searchResults.isEmpty())
        assertFalse(model.state.value.isMapEmpty)
    }

    @Test
    fun focusByLocationIdPicksTheRightFloorAndFeature() = runTest {
        val model = mapScreenModel(FakeVenueMapRepository(venue), FakeScheduleRepository())
        testScheduler.advanceUntilIdle()
        assertEquals(0, model.state.value.selectedLevelIndex)

        model.onFocusLocation("loc-room-a")
        testScheduler.advanceUntilIdle()

        assertEquals(1, model.state.value.selectedLevelIndex)
        assertEquals(roomA.id, model.state.value.selection?.featureId)
    }

    @Test
    fun aFocusThatArrivesBeforeTheMapIsAppliedWhenTheMapLands() = runTest {
        // The cold-start case the cross-link has to survive: tapping "Show on
        // map" on the very first run, before the venue has been fetched.
        val venueMapRepository = FakeVenueMapRepository()
        val model = mapScreenModel(venueMapRepository, FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        model.onFocusLocation("loc-stage")
        testScheduler.advanceUntilIdle()
        assertNull(model.state.value.selection)

        venueMapRepository.map.value = venue
        testScheduler.advanceUntilIdle()

        assertEquals(0, model.state.value.selectedLevelIndex)
        assertEquals(mainStage.id, model.state.value.selection?.featureId)
    }

    @Test
    fun focusOnALocationWithNoFeatureLeavesTheMapWhereItWas() = runTest {
        val model = mapScreenModel(FakeVenueMapRepository(venue), FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        model.onFeatureSelected(groundRestrooms.id)
        model.onFocusLocation("loc-unmapped")
        testScheduler.advanceUntilIdle()

        assertEquals(0, model.state.value.selectedLevelIndex)
        assertEquals(groundRestrooms.id, model.state.value.selection?.featureId)
    }

    // -- now / next --------------------------------------------------------

    @Test
    fun nowAndNextResolveAgainstTheInjectedClock() = runTest {
        val schedule = FakeScheduleRepository(
            listOf(
                event("past", "Opening", "loc-stage", "2026-09-01T09:00:00Z", "2026-09-01T09:45:00Z"),
                event("current", "Keynote", "loc-stage", "2026-09-01T09:50:00Z", "2026-09-01T10:30:00Z"),
                event("later", "Panel", "loc-stage", "2026-09-01T11:00:00Z", "2026-09-01T11:45:00Z"),
                event("elsewhere", "Workshop", "loc-room-a", "2026-09-01T09:55:00Z", "2026-09-01T10:40:00Z"),
            ),
        )
        val model = mapScreenModel(FakeVenueMapRepository(venue), schedule, now = "2026-09-01T10:00:00Z")
        testScheduler.advanceUntilIdle()

        model.onFeatureSelected(mainStage.id)
        testScheduler.advanceUntilIdle()

        val selection = model.state.value.selection!!
        assertTrue(selection.hostsSessions)
        assertEquals("current", selection.nowSession?.eventId)
        assertEquals("later", selection.nextSession?.eventId)
    }

    @Test
    fun aSessionEndingExactlyNowIsNoLongerOn() = runTest {
        // Half-open: a talk that has just ended must not compete with the one
        // that has just started.
        val schedule = FakeScheduleRepository(
            listOf(
                event("ending", "Ending", "loc-stage", "2026-09-01T09:00:00Z", "2026-09-01T10:00:00Z"),
                event("starting", "Starting", "loc-stage", "2026-09-01T10:00:00Z", "2026-09-01T10:45:00Z"),
            ),
        )
        val model = mapScreenModel(FakeVenueMapRepository(venue), schedule, now = "2026-09-01T10:00:00Z")
        testScheduler.advanceUntilIdle()

        model.onFeatureSelected(mainStage.id)
        testScheduler.advanceUntilIdle()

        assertEquals("starting", model.state.value.selection?.nowSession?.eventId)
        assertNull(model.state.value.selection?.nextSession)
    }

    @Test
    fun draftSessionsNeverReachTheSheet() = runTest {
        val schedule = FakeScheduleRepository(
            listOf(
                event("draft", "Draft", "loc-stage", "2026-09-01T09:50:00Z", "2026-09-01T10:30:00Z", isPublished = false),
            ),
        )
        val model = mapScreenModel(FakeVenueMapRepository(venue), schedule)
        testScheduler.advanceUntilIdle()

        model.onFeatureSelected(mainStage.id)
        testScheduler.advanceUntilIdle()

        assertNull(model.state.value.selection?.nowSession)
        assertNull(model.state.value.selection?.nextSession)
    }

    @Test
    fun aFeatureWithNoLocationNeverLooksForSessions() = runTest {
        val schedule = FakeScheduleRepository(
            listOf(event("now", "Keynote", "loc-stage", "2026-09-01T09:50:00Z", "2026-09-01T10:30:00Z")),
        )
        val model = mapScreenModel(FakeVenueMapRepository(venue), schedule)
        testScheduler.advanceUntilIdle()

        model.onFeatureSelected(groundRestrooms.id)
        testScheduler.advanceUntilIdle()

        val selection = model.state.value.selection!!
        assertFalse(selection.hostsSessions)
        assertNull(selection.nowSession)
    }

    @Test
    fun theSheetFollowsARefreshThatRemovesTheFeature() = runTest {
        val venueMapRepository = FakeVenueMapRepository(venue)
        val model = mapScreenModel(venueMapRepository, FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        model.onFeatureSelected(firstRestrooms.id)
        testScheduler.advanceUntilIdle()
        assertNotNull(model.state.value.selection)

        venueMapRepository.map.value = venue.copy(
            levels = venue.levels.map { level -> level.copy(features = level.features - firstRestrooms) },
        )
        testScheduler.advanceUntilIdle()

        assertNull(model.state.value.selection)
    }

    // -- sync --------------------------------------------------------------

    @Test
    fun failedRefreshServingCacheSetsTheBannerAndKeepsTheMap() = runTest {
        val venueMapRepository = FakeVenueMapRepository(venue)
        val staleSync = Instant.parse("2026-08-31T18:00:00Z")
        venueMapRepository.nextResult = Result.success(SyncResult(Outcome.FAILED_SERVING_CACHE, staleSync, 4))
        val model = mapScreenModel(venueMapRepository, FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        val state = model.state.value
        assertEquals(MapBanner.OFFLINE_SHOWING_CACHE, state.banner)
        assertNull(state.errorMessage)
        assertEquals(2, state.levels.size)
        assertNotNull(state.lastSyncedLabel)
    }

    @Test
    fun failureOnAnEmptyCacheSurfacesAnErrorThatRetryClears() = runTest {
        val venueMapRepository = FakeVenueMapRepository()
        venueMapRepository.nextResult = Result.failure(RuntimeException("network down"))
        val model = mapScreenModel(venueMapRepository, FakeScheduleRepository())
        testScheduler.advanceUntilIdle()

        assertNotNull(model.state.value.errorMessage)
        assertFalse(model.state.value.isLoading)

        venueMapRepository.nextResult =
            Result.success(SyncResult(Outcome.REFRESHED, Instant.parse("2026-09-01T10:05:00Z"), 4))
        model.onRetry()
        venueMapRepository.map.value = venue
        testScheduler.advanceUntilIdle()

        assertNull(model.state.value.errorMessage)
        assertEquals(2, model.state.value.levels.size)
        assertEquals(2, venueMapRepository.refreshCallCount)
    }
}

package com.conference.asmara.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.conference.asmara.data.local.VenueMapLocalDataSource
import com.conference.asmara.data.local.createTestDriver
import com.conference.asmara.data.remote.VenueMapRemoteDataSource
import com.conference.asmara.db.ScheduleDatabase
import com.conference.asmara.domain.repository.Outcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private class VenueMapFakeClock(var current: Instant) : Clock {
    override fun now(): Instant = current
}

private class FakeVenueMapRemoteDataSource(
    private var fetch: () -> String?,
) : VenueMapRemoteDataSource {
    var callCount = 0
        private set

    fun respondWith(next: () -> String?) {
        fetch = next
    }

    override suspend fun fetchVenueMap(venueSlug: String): String? {
        callCount++
        return fetch()
    }
}

private const val DOCUMENT = """
    {
      "id": "v1", "slug": "tbc-conference", "name": "TBC Conference Venue",
      "levels": [
        {
          "id": "l1", "slug": "ground", "name": "Ground", "ordinal": 0,
          "outline": {"type":"Polygon","coordinates":[[[0,0],[10,0],[10,10],[0,10],[0,0]]]},
          "features": [
            {"id":"f1","slug":"main-stage","name":"Main Stage","category":"stage",
             "location_id":"loc-1",
             "geometry":{"type":"Polygon","coordinates":[[[1,1],[5,1],[5,5],[1,5],[1,1]]]},
             "label_anchor":null,"sort_order":0}
          ]
        }
      ]
    }
"""

/**
 * Runs against the real SQLDelight table, not a fake cache — which also makes
 * this the test that would notice `VenueMap.sq` failing to create.
 */
class VenueMapRepositoryTest {
    private lateinit var driver: SqlDriver
    private lateinit var local: VenueMapLocalDataSource

    @BeforeTest
    fun setUp() {
        driver = createTestDriver()
        local = VenueMapLocalDataSource(ScheduleDatabase(driver))
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun repository(
        remote: VenueMapRemoteDataSource,
        clock: Clock,
    ) = VenueMapRepositoryImpl(remote, local, "tbc-conference", clock)

    @Test
    fun successfulFetchPopulatesTheCacheAndObserveEmitsTheParsedMap() = runTest {
        val remote = FakeVenueMapRemoteDataSource { DOCUMENT }
        val repository = repository(remote, VenueMapFakeClock(Instant.fromEpochMilliseconds(0)))

        val result = repository.refresh(force = true)

        assertTrue(result.isSuccess)
        assertEquals(Outcome.REFRESHED, result.getOrNull()?.outcome)
        assertEquals(1, result.getOrNull()?.itemCount)

        val map = repository.observeVenueMap().first()!!
        assertEquals("TBC Conference Venue", map.name)
        assertEquals("Main Stage", map.levels.single().features.single().name)
    }

    @Test
    fun anEmptyCacheObservesNullRatherThanAnEmptyMap() = runTest {
        assertNull(repository(FakeVenueMapRemoteDataSource { null }, VenueMapFakeClock(Instant.fromEpochMilliseconds(0)))
            .observeVenueMap().first())
    }

    @Test
    fun refreshWithinTheTtlSkipsTheRemoteCall() = runTest {
        val remote = FakeVenueMapRemoteDataSource { DOCUMENT }
        val clock = VenueMapFakeClock(Instant.fromEpochMilliseconds(0))
        val repository = repository(remote, clock)
        repository.refresh(force = true)

        // Well past the schedule's 15 minutes, well inside the map's 24 hours.
        clock.current += 6.hours
        val result = repository.refresh(force = false)

        assertEquals(Outcome.SKIPPED_FRESH, result.getOrNull()?.outcome)
        assertEquals(1, remote.callCount)
    }

    @Test
    fun theTtlDoesExpire() = runTest {
        val remote = FakeVenueMapRemoteDataSource { DOCUMENT }
        val clock = VenueMapFakeClock(Instant.fromEpochMilliseconds(0))
        val repository = repository(remote, clock)
        repository.refresh(force = true)

        clock.current += 25.hours
        val result = repository.refresh(force = false)

        assertEquals(Outcome.REFRESHED, result.getOrNull()?.outcome)
        assertEquals(2, remote.callCount)
    }

    @Test
    fun remoteFailureWithAWarmCacheServesStaleData() = runTest {
        val clock = VenueMapFakeClock(Instant.fromEpochMilliseconds(0))
        val remote = FakeVenueMapRemoteDataSource { DOCUMENT }
        val repository = repository(remote, clock)
        val syncedAt = repository.refresh(force = true).getOrNull()?.syncedAt

        remote.respondWith { throw RuntimeException("network down") }
        clock.current += 25.hours
        val result = repository.refresh(force = false)

        assertTrue(result.isSuccess)
        assertEquals(Outcome.FAILED_SERVING_CACHE, result.getOrNull()?.outcome)
        assertEquals(syncedAt, result.getOrNull()?.syncedAt)
        // The point of the whole exercise: the map is still on screen.
        assertEquals("TBC Conference Venue", repository.observeVenueMap().first()?.name)
    }

    @Test
    fun remoteFailureWithAColdCacheFails() = runTest {
        val remote = FakeVenueMapRemoteDataSource { throw RuntimeException("network down") }
        val result = repository(remote, VenueMapFakeClock(Instant.fromEpochMilliseconds(0))).refresh(force = true)

        assertTrue(result.isFailure)
    }

    @Test
    fun aVenueWithdrawnServerSideClearsTheCache() = runTest {
        // The server answered, and its answer was "there is no map". Keeping
        // the old one would show rooms that are no longer part of the
        // conference.
        val clock = VenueMapFakeClock(Instant.fromEpochMilliseconds(0))
        val remote = FakeVenueMapRemoteDataSource { DOCUMENT }
        val repository = repository(remote, clock)
        repository.refresh(force = true)

        remote.respondWith { null }
        val result = repository.refresh(force = true)

        assertEquals(Outcome.REFRESHED, result.getOrNull()?.outcome)
        assertEquals(0, result.getOrNull()?.itemCount)
        assertNull(repository.observeVenueMap().first())
    }

    @Test
    fun aCacheRowThatWillNotDecodeReadsAsNoCache() = runTest {
        // Not a hypothetical: the row is raw RPC JSON, and a shape change would
        // otherwise leave the screen erroring on a row the user cannot clear.
        local.replace("tbc-conference", "{ not json", Instant.fromEpochMilliseconds(0))

        assertNull(local.observeVenueMap().first())
        assertEquals(0, local.featureCount())
    }

    @Test
    fun theCacheIsSingleRowAndTheLatestWriteWins() = runTest {
        local.replace("tbc-conference", DOCUMENT, Instant.fromEpochMilliseconds(0))
        local.replace("tbc-conference", DOCUMENT, Instant.fromEpochMilliseconds(5_000))

        assertEquals(Instant.fromEpochMilliseconds(5_000), local.lastSyncedAt())
        assertEquals(1, local.featureCount())
    }
}

package com.conference.asmara.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.conference.asmara.data.dto.EventDto
import com.conference.asmara.data.local.ScheduleLocalDataSource
import com.conference.asmara.data.local.createTestDriver
import com.conference.asmara.data.remote.ScheduleRemoteDataSource
import com.conference.asmara.db.ScheduleDatabase
import com.conference.asmara.domain.repository.Outcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private class FakeClock(var current: Instant) : Clock {
    override fun now(): Instant = current
}

private class FakeScheduleRemoteDataSource(
    private val fetch: () -> List<EventDto>,
) : ScheduleRemoteDataSource {
    var callCount = 0
        private set

    override suspend fun fetchSchedule(): List<EventDto> {
        callCount++
        return fetch()
    }
}

class ScheduleRepositoryTest {
    private lateinit var driver: SqlDriver
    private lateinit var local: ScheduleLocalDataSource

    @BeforeTest
    fun setUp() {
        driver = createTestDriver()
        local = ScheduleLocalDataSource(ScheduleDatabase(driver))
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun successfulFetchPopulatesCacheAndObserveEmitsIt() = runTest {
        val remote = FakeScheduleRemoteDataSource { listOf(sampleEventDto()) }
        val repository = ScheduleRepositoryImpl(remote, local, FakeClock(Instant.fromEpochMilliseconds(0)))

        val result = repository.refresh(force = true)

        assertTrue(result.isSuccess)
        assertEquals(Outcome.REFRESHED, result.getOrNull()?.outcome)
        assertEquals(1, repository.observeSchedule().first().size)
    }

    @Test
    fun refreshWithinTtlSkipsRemoteCall() = runTest {
        val remote = FakeScheduleRemoteDataSource { listOf(sampleEventDto()) }
        val clock = FakeClock(Instant.fromEpochMilliseconds(0))
        val repository = ScheduleRepositoryImpl(remote, local, clock)
        repository.refresh(force = true)

        clock.current += 5.minutes
        val result = repository.refresh(force = false)

        assertEquals(Outcome.SKIPPED_FRESH, result.getOrNull()?.outcome)
        assertEquals(1, remote.callCount)
    }

    @Test
    fun forcedRefreshWithinTtlCallsRemoteAgain() = runTest {
        val remote = FakeScheduleRemoteDataSource { listOf(sampleEventDto()) }
        val clock = FakeClock(Instant.fromEpochMilliseconds(0))
        val repository = ScheduleRepositoryImpl(remote, local, clock)
        repository.refresh(force = true)

        clock.current += 5.minutes
        repository.refresh(force = true)

        assertEquals(2, remote.callCount)
    }

    @Test
    fun remoteFailureWithWarmCacheServesStaleData() = runTest {
        val clock = FakeClock(Instant.fromEpochMilliseconds(0))
        var shouldFail = false
        val remote = FakeScheduleRemoteDataSource {
            if (shouldFail) throw RuntimeException("network down") else listOf(sampleEventDto())
        }
        val repository = ScheduleRepositoryImpl(remote, local, clock)
        val firstResult = repository.refresh(force = true)
        val syncedAt = firstResult.getOrNull()?.syncedAt

        shouldFail = true
        clock.current += 20.minutes
        val result = repository.refresh(force = false)

        assertTrue(result.isSuccess)
        assertEquals(Outcome.FAILED_SERVING_CACHE, result.getOrNull()?.outcome)
        assertEquals(syncedAt, result.getOrNull()?.syncedAt)
    }

    @Test
    fun remoteFailureWithColdCacheFails() = runTest {
        val remote = FakeScheduleRemoteDataSource { throw RuntimeException("network down") }
        val repository = ScheduleRepositoryImpl(remote, local, FakeClock(Instant.fromEpochMilliseconds(0)))

        val result = repository.refresh(force = true)

        assertTrue(result.isFailure)
    }

    private fun sampleEventDto() = EventDto(
        id = "e1",
        slug = "opening-keynote",
        title = "Opening Keynote",
        startTime = "2026-09-01T09:00:00Z",
        endTime = "2026-09-01T09:45:00Z",
        eventType = "keynote",
        isPublished = true,
    )
}

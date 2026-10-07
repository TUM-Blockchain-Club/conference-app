package com.conference.asmara.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.conference.asmara.data.dto.AnnouncementDto
import com.conference.asmara.data.local.AnnouncementLocalDataSource
import com.conference.asmara.data.local.createTestDriver
import com.conference.asmara.data.remote.AnnouncementRemoteDataSource
import com.conference.asmara.db.ScheduleDatabase
import com.conference.asmara.domain.model.AnnouncementCategory
import com.conference.asmara.domain.repository.Outcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private class AnnouncementFakeClock(var current: Instant) : Clock {
    override fun now(): Instant = current
}

private class FakeAnnouncementRemoteDataSource(private var fetch: () -> List<AnnouncementDto>) :
    AnnouncementRemoteDataSource {
    var callCount = 0
        private set

    fun respondWith(next: () -> List<AnnouncementDto>) {
        fetch = next
    }

    override suspend fun fetchAnnouncements(): List<AnnouncementDto> {
        callCount++
        return fetch()
    }
}

private fun dto(id: String, category: String = "general") = AnnouncementDto(
    id = id,
    slug = id,
    title = "Title $id",
    body = "Body $id",
    category = category,
    publishedAt = "2026-09-01T08:00:00Z",
)

class AnnouncementRepositoryTest {
    private lateinit var driver: SqlDriver
    private lateinit var local: AnnouncementLocalDataSource

    @BeforeTest
    fun setUp() {
        driver = createTestDriver()
        local = AnnouncementLocalDataSource(ScheduleDatabase(driver))
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun repository(remote: AnnouncementRemoteDataSource, clock: Clock) =
        AnnouncementRepositoryImpl(remote, local, clock)

    @Test
    fun refreshPopulatesTheCache() = runTest {
        val remote = FakeAnnouncementRemoteDataSource { listOf(dto("a"), dto("b", "emergency")) }
        val repository = repository(remote, AnnouncementFakeClock(Instant.fromEpochMilliseconds(0)))

        val result = repository.refresh(force = true)

        assertEquals(Outcome.REFRESHED, result.getOrNull()?.outcome)
        assertEquals(2, result.getOrNull()?.itemCount)
        val cached = repository.observeAnnouncements().first().associateBy { it.id }
        assertEquals(AnnouncementCategory.EMERGENCY, cached.getValue("b").category)
    }

    @Test
    fun refreshWithinTheTtlSkipsTheRemoteCall() = runTest {
        val remote = FakeAnnouncementRemoteDataSource { listOf(dto("a")) }
        val clock = AnnouncementFakeClock(Instant.fromEpochMilliseconds(0))
        val repository = repository(remote, clock)
        repository.refresh(force = true)

        clock.current += 4.minutes
        val result = repository.refresh(force = false)

        assertEquals(Outcome.SKIPPED_FRESH, result.getOrNull()?.outcome)
        assertEquals(1, result.getOrNull()?.itemCount)
        assertEquals(1, remote.callCount)
    }

    @Test
    fun theTtlIsFiveMinutesNotTheSchedulesFifteen() = runTest {
        val remote = FakeAnnouncementRemoteDataSource { listOf(dto("a")) }
        val clock = AnnouncementFakeClock(Instant.fromEpochMilliseconds(0))
        val repository = repository(remote, clock)
        repository.refresh(force = true)

        clock.current += 6.minutes
        val result = repository.refresh(force = false)

        assertEquals(Outcome.REFRESHED, result.getOrNull()?.outcome)
        assertEquals(2, remote.callCount)
    }

    @Test
    fun aForcedRefreshIgnoresTheTtl() = runTest {
        val remote = FakeAnnouncementRemoteDataSource { listOf(dto("a")) }
        val repository = repository(remote, AnnouncementFakeClock(Instant.fromEpochMilliseconds(0)))
        repository.refresh(force = true)
        repository.refresh(force = true)

        assertEquals(2, remote.callCount)
    }

    @Test
    fun failureWithAWarmCacheServesTheCache() = runTest {
        val clock = AnnouncementFakeClock(Instant.fromEpochMilliseconds(0))
        val remote = FakeAnnouncementRemoteDataSource { listOf(dto("a")) }
        val repository = repository(remote, clock)
        val syncedAt = repository.refresh(force = true).getOrNull()?.syncedAt

        remote.respondWith { throw RuntimeException("network down") }
        clock.current += 10.minutes
        val result = repository.refresh(force = false)

        assertTrue(result.isSuccess)
        assertEquals(Outcome.FAILED_SERVING_CACHE, result.getOrNull()?.outcome)
        assertEquals(syncedAt, result.getOrNull()?.syncedAt)
        assertEquals(listOf("a"), repository.observeAnnouncements().first().map { it.id })
    }

    @Test
    fun failureWithAColdCacheFails() = runTest {
        val remote = FakeAnnouncementRemoteDataSource { throw RuntimeException("network down") }
        val result = repository(remote, AnnouncementFakeClock(Instant.fromEpochMilliseconds(0))).refresh(force = true)

        assertTrue(result.isFailure)
    }

    @Test
    fun markAllReadSurvivesTheNextRefresh() = runTest {
        val remote = FakeAnnouncementRemoteDataSource { listOf(dto("a")) }
        val repository = repository(remote, AnnouncementFakeClock(Instant.fromEpochMilliseconds(0)))
        repository.refresh(force = true)

        repository.markAllRead()
        remote.respondWith { listOf(dto("a"), dto("b")) }
        repository.refresh(force = true)

        val byId = repository.observeAnnouncements().first().associateBy { it.id }
        assertTrue(byId.getValue("a").isRead)
        assertFalse(byId.getValue("b").isRead)
    }

    @Test
    fun emergencyCannotBeOptedOutOfThroughThePreferencesRepository() = runTest {
        val preferences = NotificationPreferencesRepositoryImpl(local)

        preferences.setCategoryEnabled(AnnouncementCategory.EMERGENCY, false)
        preferences.setCategoryEnabled(AnnouncementCategory.SCHEDULE, false)

        val enabled = preferences.observeEnabledCategories().first()
        assertTrue(AnnouncementCategory.EMERGENCY in enabled)
        assertFalse(AnnouncementCategory.SCHEDULE in enabled)
    }
}

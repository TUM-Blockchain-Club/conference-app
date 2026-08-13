package com.conference.asmara.data.local

import app.cash.sqldelight.db.SqlDriver
import com.conference.asmara.db.ScheduleDatabase
import com.conference.asmara.domain.model.Event
import com.conference.asmara.domain.model.EventSpeaker
import com.conference.asmara.domain.model.EventType
import com.conference.asmara.domain.model.Location
import com.conference.asmara.domain.model.Speaker
import com.conference.asmara.domain.model.SpeakerRole
import com.conference.asmara.domain.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant

class ScheduleLocalDataSourceTest {
    private lateinit var driver: SqlDriver
    private lateinit var dataSource: ScheduleLocalDataSource

    @BeforeTest
    fun setUp() {
        driver = createTestDriver()
        dataSource = ScheduleLocalDataSource(ScheduleDatabase(driver))
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun replaceAllThenObserveEmitsInsertedEvents() = runTest {
        dataSource.replaceAll(listOf(sampleEvent()), Instant.fromEpochMilliseconds(1_000))

        val observed = dataSource.observeSchedule().first()

        assertEquals(1, observed.size)
        assertEquals("opening-keynote", observed.first().slug)
        assertEquals(listOf("jane-doe"), observed.first().speakers.map { it.speaker.slug })
    }

    @Test
    fun replaceAllReplacesPreviousRows() = runTest {
        dataSource.replaceAll(listOf(sampleEvent(slug = "first", id = "e1")), Instant.fromEpochMilliseconds(1_000))
        dataSource.replaceAll(listOf(sampleEvent(slug = "second", id = "e2")), Instant.fromEpochMilliseconds(2_000))

        val observed = dataSource.observeSchedule().first()

        assertEquals(listOf("second"), observed.map { it.slug })
    }

    @Test
    fun replaceAllRollsBackOnFailure() = runTest {
        dataSource.replaceAll(listOf(sampleEvent(slug = "first", id = "e1")), Instant.fromEpochMilliseconds(1_000))

        val duplicateIdEvents = listOf(
            sampleEvent(slug = "second", id = "dup"),
            sampleEvent(slug = "third", id = "dup"),
        )
        assertFailsWith<Throwable> {
            dataSource.replaceAll(duplicateIdEvents, Instant.fromEpochMilliseconds(2_000))
        }

        val observed = dataSource.observeSchedule().first()
        assertEquals(listOf("first"), observed.map { it.slug })
    }

    @Test
    fun lastSyncedAtRoundTrips() = runTest {
        assertNull(dataSource.lastSyncedAt())

        val instant = Instant.fromEpochMilliseconds(42_000)
        dataSource.replaceAll(emptyList(), instant)

        assertEquals(instant, dataSource.lastSyncedAt())
    }

    private fun sampleEvent(slug: String = "opening-keynote", id: String = "e1") = Event(
        id = id,
        slug = slug,
        title = "Opening Keynote",
        description = null,
        startTime = Instant.fromEpochMilliseconds(0),
        endTime = Instant.fromEpochMilliseconds(3_600_000),
        track = Track("t1", "defi", "DeFi", null, null, 0),
        location = Location("l1", "main-stage", "Main Stage", null, null),
        eventType = EventType.KEYNOTE,
        isPublished = true,
        speakers = listOf(
            EventSpeaker(
                speaker = Speaker("s1", "jane-doe", "Jane Doe", null, null, null, null, emptyMap()),
                role = SpeakerRole.SPEAKER,
                sortOrder = 0,
            ),
        ),
    )
}

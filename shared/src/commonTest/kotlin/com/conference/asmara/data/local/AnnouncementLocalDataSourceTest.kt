package com.conference.asmara.data.local

import app.cash.sqldelight.db.SqlDriver
import com.conference.asmara.db.ScheduleDatabase
import com.conference.asmara.domain.model.Announcement
import com.conference.asmara.domain.model.AnnouncementCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

private fun announcement(
    id: String,
    title: String = "Announcement $id",
    category: AnnouncementCategory = AnnouncementCategory.GENERAL,
    publishedAt: String = "2026-09-01T08:00:00Z",
) = Announcement(
    id = id,
    slug = id,
    title = title,
    body = "Body of $id",
    category = category,
    publishedAt = Instant.parse(publishedAt),
    isRead = false,
)

/** Runs against the real SQLDelight schema — the test that notices `Announcements.sq` failing to create. */
class AnnouncementLocalDataSourceTest {
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

    private val t0 = Instant.fromEpochMilliseconds(0)

    @Test
    fun observeEmitsNewestFirst() = runTest {
        local.replaceAll(
            listOf(
                announcement("old", publishedAt = "2026-09-01T07:00:00Z"),
                announcement("new", publishedAt = "2026-09-01T09:00:00Z"),
            ),
            t0,
        )

        assertEquals(listOf("new", "old"), local.observeAnnouncements().first().map { it.id })
    }

    @Test
    fun replaceAllPreservesReadAtForRowsThatStillExist() = runTest {
        local.replaceAll(listOf(announcement("a"), announcement("b")), t0)
        local.markRead(Instant.fromEpochMilliseconds(1_000))

        // A refresh that edits "a", keeps "b" and adds "c".
        local.replaceAll(listOf(announcement("a", title = "Edited"), announcement("b"), announcement("c")), t0)

        val byId = local.observeAnnouncements().first().associateBy { it.id }
        assertEquals("Edited", byId.getValue("a").title)
        assertTrue(byId.getValue("a").isRead, "an edit must not resurrect a read announcement as unread")
        assertTrue(byId.getValue("b").isRead)
        assertFalse(byId.getValue("c").isRead)
    }

    @Test
    fun replaceAllDropsRowsTheServerNoLongerHas() = runTest {
        local.replaceAll(listOf(announcement("a"), announcement("b")), t0)
        local.replaceAll(listOf(announcement("b")), t0)
        assertEquals(listOf("b"), local.observeAnnouncements().first().map { it.id })

        local.replaceAll(emptyList(), t0)
        assertEquals(0, local.announcementCount())
    }

    @Test
    fun unreadCountTracksMarkRead() = runTest {
        local.replaceAll(listOf(announcement("a"), announcement("b")), t0)
        assertEquals(2, local.unreadCount().first())

        local.markRead(Instant.fromEpochMilliseconds(1_000))
        assertEquals(0, local.unreadCount().first())

        local.replaceAll(listOf(announcement("a"), announcement("b"), announcement("c")), t0)
        assertEquals(1, local.unreadCount().first())
    }

    @Test
    fun anInstalledVersion2DatabaseUpgradesThrough2Sqm() = runTest {
        // Simulate a database from the previous release: everything but the
        // tables this schema version adds. Without 2.sqm the first query
        // below would fail with "no such table".
        listOf("announcementEntity", "notificationPreference", "announcementSyncMetadata").forEach {
            driver.execute(null, "DROP TABLE $it", 0)
        }

        ScheduleDatabase.Schema.migrate(driver, oldVersion = 2, newVersion = ScheduleDatabase.Schema.version)

        assertEquals(3, ScheduleDatabase.Schema.version)
        local.replaceAll(listOf(announcement("a")), t0)
        local.setCategoryEnabled(AnnouncementCategory.KEYNOTE, false)
        assertEquals(1, local.unreadCount().first())
    }

    @Test
    fun replaceAllRecordsTheSyncTime() = runTest {
        local.replaceAll(listOf(announcement("a")), Instant.fromEpochMilliseconds(5_000))
        assertEquals(Instant.fromEpochMilliseconds(5_000), local.lastSyncedAt())
    }

    @Test
    fun everyCategoryIsEnabledUntilTheUserSaysOtherwise() = runTest {
        assertEquals(AnnouncementCategory.entries.toSet(), local.observeEnabledCategories().first())
    }

    @Test
    fun aDisabledCategoryStaysDisabledAndCanBeReEnabled() = runTest {
        local.setCategoryEnabled(AnnouncementCategory.KEYNOTE, false)
        assertFalse(AnnouncementCategory.KEYNOTE in local.observeEnabledCategories().first())

        local.setCategoryEnabled(AnnouncementCategory.KEYNOTE, true)
        assertTrue(AnnouncementCategory.KEYNOTE in local.observeEnabledCategories().first())
    }

    @Test
    fun emergencyIsEnabledWhateverIsStored() = runTest {
        // Defence in depth: the repository refuses this write, but a row that
        // got here anyway must still not be able to silence an emergency.
        local.setCategoryEnabled(AnnouncementCategory.EMERGENCY, false)
        assertTrue(AnnouncementCategory.EMERGENCY in local.observeEnabledCategories().first())
    }
}

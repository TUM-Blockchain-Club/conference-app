package com.conference.asmara.push

import com.conference.asmara.domain.model.AnnouncementCategory
import com.conference.asmara.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private class FakePushRegistrar(private val initial: String?) : PushRegistrar {
    val updates = MutableSharedFlow<String>(extraBufferCapacity = 8)

    override suspend fun currentToken(): String? = initial

    override suspend fun requestPermission(): Boolean = true

    override val tokenUpdates: Flow<String> = updates
}

private data class Registration(
    val token: String,
    val platform: PushPlatform,
    val categories: Set<AnnouncementCategory>,
)

/** The fake RPC caller: records every call, and can be told to fail the next few. */
private class RecordingRegistry : DeviceTokenRegistry {
    val calls = mutableListOf<Registration>()
    var failuresRemaining = 0

    override suspend fun register(token: String, platform: PushPlatform, categories: Set<AnnouncementCategory>) {
        if (failuresRemaining > 0) {
            failuresRemaining--
            throw RuntimeException("offline")
        }
        calls += Registration(token, platform, categories)
    }
}

private class FakePreferences : NotificationPreferencesRepository {
    val enabled = MutableStateFlow(AnnouncementCategory.entries.toSet())

    override fun observeEnabledCategories(): Flow<Set<AnnouncementCategory>> = enabled

    override suspend fun setCategoryEnabled(category: AnnouncementCategory, enabled: Boolean) {
        if (!category.canOptOut) return
        this.enabled.value = if (enabled) this.enabled.value + category else this.enabled.value - category
    }
}

// The syncer runs in backgroundScope, as it would in a process-lifetime scope.
// advanceUntilIdle() deliberately does not wait for background work, so these
// tests drive the scheduler with runCurrent() instead.
private fun TestScope.syncer(
    registrar: PushRegistrar,
    registry: DeviceTokenRegistry,
    preferences: NotificationPreferencesRepository = FakePreferences(),
) = PushTokenSyncer(registrar, registry, preferences, PushPlatform.ANDROID, backgroundScope, retryDelay = 1.seconds)

class PushTokenSyncerTest {

    @Test
    fun withPushDisabledTheServerIsNeverCalled() = runTest {
        // The acceptance bar for a build with no credentials: not an empty
        // registration, no registration at all.
        val registry = RecordingRegistry()
        val preferences = FakePreferences()
        syncer(NoOpPushRegistrar, registry, preferences).start()
        testScheduler.runCurrent()

        preferences.setCategoryEnabled(AnnouncementCategory.KEYNOTE, false)
        testScheduler.runCurrent()

        assertEquals(emptyList(), registry.calls)
    }

    @Test
    fun registersTheCurrentTokenWithEveryEnabledCategoryOnStart() = runTest {
        val registry = RecordingRegistry()
        syncer(FakePushRegistrar("token-1"), registry).start()
        testScheduler.runCurrent()

        assertEquals(
            listOf(Registration("token-1", PushPlatform.ANDROID, AnnouncementCategory.entries.toSet())),
            registry.calls,
        )
    }

    @Test
    fun aRotatedTokenIsRegisteredAgain() = runTest {
        val registrar = FakePushRegistrar("token-1")
        val registry = RecordingRegistry()
        syncer(registrar, registry).start()
        testScheduler.runCurrent()

        registrar.updates.emit("token-2")
        testScheduler.runCurrent()

        assertEquals(listOf("token-1", "token-2"), registry.calls.map { it.token })
    }

    @Test
    fun theSameTokenTwiceIsNotRegisteredTwice() = runTest {
        val registrar = FakePushRegistrar("token-1")
        val registry = RecordingRegistry()
        syncer(registrar, registry).start()
        testScheduler.runCurrent()

        registrar.updates.emit("token-1")
        testScheduler.runCurrent()

        assertEquals(1, registry.calls.size)
    }

    @Test
    fun aTokenThatArrivesLaterStillRegisters() = runTest {
        // iOS: there is no token until the AppDelegate is handed one.
        val registrar = FakePushRegistrar(initial = null)
        val registry = RecordingRegistry()
        syncer(registrar, registry).start()
        testScheduler.runCurrent()
        assertEquals(0, registry.calls.size)

        registrar.updates.emit("late-token")
        testScheduler.runCurrent()

        assertEquals(listOf("late-token"), registry.calls.map { it.token })
    }

    @Test
    fun aPreferenceChangeReSendsTheCategoryList() = runTest {
        val preferences = FakePreferences()
        val registry = RecordingRegistry()
        syncer(FakePushRegistrar("token-1"), registry, preferences).start()
        testScheduler.runCurrent()

        preferences.setCategoryEnabled(AnnouncementCategory.KEYNOTE, false)
        testScheduler.runCurrent()

        assertEquals(2, registry.calls.size)
        val latest = registry.calls.last().categories
        assertTrue(AnnouncementCategory.KEYNOTE !in latest)
        assertTrue(AnnouncementCategory.EMERGENCY in latest, "emergency is always listed")
    }

    @Test
    fun aFailedRegistrationIsRetried() = runTest {
        val registry = RecordingRegistry().apply { failuresRemaining = 2 }
        syncer(FakePushRegistrar("token-1"), registry).start()
        testScheduler.runCurrent()
        assertEquals(0, registry.calls.size)

        // Back-off is 1s then 2s with the test's retryDelay.
        testScheduler.advanceTimeBy(4.seconds)
        testScheduler.runCurrent()

        assertEquals(listOf("token-1"), registry.calls.map { it.token })
    }

    @Test
    fun startIsIdempotent() = runTest {
        val registry = RecordingRegistry()
        val syncer = syncer(FakePushRegistrar("token-1"), registry)
        syncer.start()
        syncer.start()
        testScheduler.runCurrent()

        assertEquals(1, registry.calls.size)
    }
}

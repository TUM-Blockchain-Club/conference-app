package com.conference.asmara.push

import com.conference.asmara.domain.model.AnnouncementCategory
import com.conference.asmara.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps the server's `device_tokens` row for this device current: registers on
 * every launch (which is also what keeps `last_seen_at` fresh against pruning),
 * again whenever the token rotates, and again whenever the user changes a
 * category toggle.
 *
 * The only thing in the app that talks to `register_device_token`.
 *
 * With [NoOpPushRegistrar] bound there is no token and no update, so the
 * combined flow below never emits and the server is never called — push off
 * means *no* write surface exercised, not an empty one.
 */
class PushTokenSyncer(
    private val registrar: PushRegistrar,
    private val registry: DeviceTokenRegistry,
    private val preferences: NotificationPreferencesRepository,
    private val platform: PushPlatform,
    private val scope: CoroutineScope,
    private val retryDelay: Duration = 30.seconds,
) {
    private var started = false

    /** Idempotent; called once from `initKoin`. */
    fun start() {
        if (started) return
        started = true
        scope.launch { run() }
    }

    private suspend fun run() {
        val tokens = flow {
            registrar.currentToken()?.let { emit(it) }
            emitAll(registrar.tokenUpdates)
        }.distinctUntilChanged()

        // collectLatest: a newer token or toggle supersedes a registration that
        // is still retrying, rather than queueing behind it.
        combine(tokens, preferences.observeEnabledCategories().distinctUntilChanged(), ::Pair)
            .collectLatest { (token, categories) -> registerWithRetry(token, categories) }
    }

    private suspend fun registerWithRetry(token: String, categories: Set<AnnouncementCategory>) {
        // Emergency is always listed: the server sends it regardless, but a row
        // that says so is easier to reason about when reading the table.
        val wanted = categories + AnnouncementCategory.EMERGENCY
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                registry.register(token, platform, wanted)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Offline at launch is the common case. The next launch, token
                // rotation or toggle retries anyway; these attempts only cover
                // a connection that comes back within a minute or two.
                if (attempt < MAX_ATTEMPTS - 1) delay(retryDelay * (attempt + 1))
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}

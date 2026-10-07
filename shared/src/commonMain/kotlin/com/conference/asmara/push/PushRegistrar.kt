package com.conference.asmara.push

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * The device side of push: getting a token the server can send to, and asking
 * the user whether notifications may be shown.
 *
 * A plain interface rather than `expect`/`actual`: that lets [NoOpPushRegistrar]
 * and test fakes exist without a third target. The real implementations are
 * bound by each platform's Koin module — `FirebasePushRegistrar` on Android,
 * `ApnsPushRegistrar` on iOS — and only when that platform's generated
 * `PushConfig.ENABLED` is true. That binding is the one place the flag is read.
 */
interface PushRegistrar {
    /** The current token, or null if there is none (yet, or ever). */
    suspend fun currentToken(): String?

    /**
     * Asks the user to allow notifications. Returns whether they are allowed.
     *
     * Getting a token does not depend on this on either platform: a device that
     * declined still registers, and so still refreshes its feed on a data push.
     */
    suspend fun requestPermission(): Boolean

    /** Tokens issued after [currentToken] — FCM and APNs both rotate them. */
    val tokenUpdates: Flow<String>
}

/**
 * What every build without push credentials gets. No token, no prompt, no
 * updates — so [PushTokenSyncer] has nothing to send and never calls the server.
 */
object NoOpPushRegistrar : PushRegistrar {
    override suspend fun currentToken(): String? = null

    override suspend fun requestPermission(): Boolean = false

    override val tokenUpdates: Flow<String> = emptyFlow()
}

/** Mirrors the `device_tokens_platform_valid` check constraint. */
enum class PushPlatform(val wireName: String) { ANDROID("android"), IOS("ios") }

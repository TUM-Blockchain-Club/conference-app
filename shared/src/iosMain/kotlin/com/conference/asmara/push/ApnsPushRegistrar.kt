package com.conference.asmara.push

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * Push on iOS. Bound only when `PushConfig.ENABLED` — see `iosPlatformModule`.
 *
 * The token itself is delivered to the Swift `AppDelegate`, which hands it to
 * Kotlin through [onPushTokenReceived]. What that token *is* matters: the send
 * path speaks FCM only, so the AppDelegate must forward the FCM registration
 * token from the Firebase iOS SDK (`Messaging.messaging().fcmToken`), not the
 * raw APNs device token. See the note in `AppDelegate.swift`.
 *
 * Without an Apple Developer account none of this can run: the
 * `aps-environment` entitlement cannot be signed, `registerForRemoteNotifications`
 * fails on device, and the simulator never produces a real token. It compiles
 * and links, which is what `make ci` checks.
 */
class ApnsPushRegistrar : PushRegistrar {
    private val latest = MutableStateFlow<String?>(null)

    override val tokenUpdates: Flow<String> = latest.filterNotNull()

    override suspend fun currentToken(): String? {
        // Registering does not need the user's permission — only *showing* a
        // notification does — so this runs on every launch and the token
        // arrives asynchronously through tokenUpdates.
        withContext(Dispatchers.Main) { UIApplication.sharedApplication.registerForRemoteNotifications() }
        return latest.value
    }

    override suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { granted, _ -> continuation.resume(granted) }
    }

    fun onToken(token: String) {
        latest.value = token
    }
}

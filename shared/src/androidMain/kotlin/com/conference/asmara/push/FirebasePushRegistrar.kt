package com.conference.asmara.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * FCM on Android. Bound only when `PushConfig.ENABLED` — see
 * `androidPlatformModule`.
 *
 * Token rotation arrives in `FirebaseMessagingService.onNewToken`, which lives
 * in the host app and forwards here through [onNewToken].
 */
class FirebasePushRegistrar(private val context: Context) : PushRegistrar {
    private val updates = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override val tokenUpdates: Flow<String> = updates.asSharedFlow()

    override suspend fun currentToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            // A failed fetch (no Play services, offline first launch) is "no
            // token yet", not an error: FCM calls onNewToken once it has one.
            continuation.resume(if (task.isSuccessful) task.result else null)
        }
    }

    override suspend fun requestPermission(): Boolean = AndroidNotificationPermission.request(context)

    fun onNewToken(token: String) {
        updates.tryEmit(token)
    }
}

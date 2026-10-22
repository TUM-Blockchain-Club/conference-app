package com.conference.asmara.push

import com.conference.asmara.domain.repository.AnnouncementRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

// Called by the Swift AppDelegate. Top-level functions rather than methods on
// a Koin-resolved object, so Swift needs no knowledge of the DI container and
// a call that arrives before Koin is up (it cannot, in practice) is a no-op
// rather than a crash.

private val scope = MainScope()

/** A new push token. Ignored unless the real registrar is bound — i.e. push is enabled. */
fun onPushTokenReceived(token: String) {
    (KoinPlatform.getKoinOrNull()?.getOrNull<PushRegistrar>() as? ApnsPushRegistrar)?.onToken(token)
}

/**
 * A push arrived, in the foreground or by a tap. The push never carries the
 * feed; it only says "something changed", and the cache is refreshed from the
 * server — the same path a pull-to-refresh takes.
 */
fun onRemoteNotificationReceived() {
    val repository = KoinPlatform.getKoinOrNull()?.getOrNull<AnnouncementRepository>() ?: return
    scope.launch { repository.refresh(force = true) }
}

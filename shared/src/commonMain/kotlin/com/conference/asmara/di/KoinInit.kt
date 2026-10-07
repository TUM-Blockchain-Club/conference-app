package com.conference.asmara.di

import com.conference.asmara.push.PushTokenSyncer
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatform

/**
 * Starts the app's one DI container, or returns it if it is already running.
 *
 * Called from each platform's process-level entry point — `AsmaraApplication`
 * on Android, `iOSApp.init` on iOS — never from inside the composition. The
 * container used to be built by `KoinApplication { … }` in `App()`, which tied
 * it to the composition's lifetime: an Android activity recreation built a
 * second `SupabaseClient` and a second SQLite driver, and anything that runs
 * without an activity (a `FirebaseMessagingService`, say) had no container to
 * reach at all.
 *
 * Idempotent, so a second call is harmless rather than a "Koin already
 * started" crash: the iOS host can be torn down and rebuilt by SwiftUI, and a
 * test may start the app twice in one process.
 */
fun initKoin(platformModule: Module): Koin {
    KoinPlatform.getKoinOrNull()?.let { return it }
    val koin = startKoin { modules(platformModule, appModule, dataModule) }.koin
    // Push registration belongs to the process, not to a screen. With push
    // disabled this starts a syncer that has nothing to sync.
    koin.get<PushTokenSyncer>().start()
    return koin
}

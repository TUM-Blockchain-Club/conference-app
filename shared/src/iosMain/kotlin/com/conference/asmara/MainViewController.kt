package com.conference.asmara

import androidx.compose.ui.window.ComposeUIViewController
import com.conference.asmara.di.initKoin
import org.koin.core.Koin
import platform.UIKit.UIViewController

/**
 * Called from `iOSApp.init`, before any view exists — the iOS counterpart of
 * `AsmaraApplication.onCreate`. The platform module is chosen here rather than
 * by Swift so the choice is type-checked.
 */
fun startKoinIos(): Koin = initKoin(iosPlatformModule())

fun MainViewController(): UIViewController {
    // initKoin is idempotent, so this reads the container iOSApp.init already
    // started rather than building a second one.
    val koin = startKoinIos()
    return ComposeUIViewController { App(koin) }
}

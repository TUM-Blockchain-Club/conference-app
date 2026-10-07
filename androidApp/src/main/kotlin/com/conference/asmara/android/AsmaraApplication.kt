package com.conference.asmara.android

import android.app.Application
import com.conference.asmara.androidPlatformModule
import com.conference.asmara.di.initKoin

/**
 * Owns the DI container for the life of the process.
 *
 * Started here rather than in `MainActivity` because an activity is not the
 * process: it is destroyed and rebuilt on every configuration change, and some
 * entry points (a `FirebaseMessagingService`) run with no activity at all.
 */
class AsmaraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidPlatformModule(this))
    }
}

package com.conference.asmara.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
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
        createAnnouncementsChannel()
    }

    /**
     * The channel FCM draws background notifications into (named in the
     * manifest) and AsmaraMessagingService draws foreground ones into. Created
     * whether or not push is configured: harmless if unused, and a channel
     * that only appears once push is switched on would be one more thing that
     * differs between the two builds. High importance, because the channel
     * carries emergency notices.
     */
    private fun createAnnouncementsChannel() {
        val channel = NotificationChannel(
            ANNOUNCEMENTS_CHANNEL_ID,
            "Announcements",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Schedule changes, keynote reminders and safety notices." }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

/** Also referenced from AndroidManifest.xml — keep the two in step. */
const val ANNOUNCEMENTS_CHANNEL_ID = "announcements"

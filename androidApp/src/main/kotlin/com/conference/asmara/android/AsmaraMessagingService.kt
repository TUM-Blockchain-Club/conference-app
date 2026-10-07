package com.conference.asmara.android

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.conference.asmara.domain.repository.AnnouncementRepository
import com.conference.asmara.push.FirebasePushRegistrar
import com.conference.asmara.push.PushRegistrar
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import kotlin.time.Duration.Companion.seconds

/**
 * FCM's entry point. Declared unconditionally in the manifest but inert
 * without google-services.json: there is no FirebaseApp to deliver anything,
 * so neither callback ever fires.
 *
 * Runs with no activity, which is why the DI container lives in
 * [AsmaraApplication] rather than in the composition.
 */
class AsmaraMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        (KoinPlatform.getKoinOrNull()?.getOrNull<PushRegistrar>() as? FirebasePushRegistrar)?.onNewToken(token)
    }

    /**
     * Called for every message while the app is in the foreground, and for
     * data-only messages in the background. (A notification message to a
     * backgrounded app is drawn by the system instead, and the tap lands in
     * [MainActivity], which refreshes from there.)
     *
     * The message never *is* the announcement — the feed reads only from the
     * cache — so the work here is a forced refresh. Blocking is allowed: this
     * runs on FCM's worker thread, which grants about ten seconds.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val repository = KoinPlatform.getKoinOrNull()?.getOrNull<AnnouncementRepository>() ?: return
        runBlocking { withTimeoutOrNull(REFRESH_BUDGET) { repository.refresh(force = true) } }

        // Android does not draw a notification message for a foreground app.
        // Drawing it here keeps an emergency notice from going unseen by
        // someone looking at the map.
        message.notification?.let { show(it.title, it.body, message.data[EXTRA_ANNOUNCEMENT_ID]) }
    }

    private fun show(title: String?, body: String?, announcementId: String?) {
        val allowed = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33
        if (!allowed) return

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_ANNOUNCEMENT_ID, announcementId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, ANNOUNCEMENTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(announcementId.hashCode(), notification)
    }

    companion object {
        /** Set by the send-announcement Edge Function in every message's data payload. */
        const val EXTRA_ANNOUNCEMENT_ID = "announcement_id"
        private val REFRESH_BUDGET = 8.seconds
    }
}

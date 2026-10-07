package com.conference.asmara.android

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.conference.asmara.App
import com.conference.asmara.domain.repository.AnnouncementRepository
import com.conference.asmara.push.AndroidNotificationPermission
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Must happen before the activity is started (ActivityResult
        // registration rule). Unconditional and inert: nothing asks for the
        // permission unless the Firebase registrar is bound.
        AndroidNotificationPermission.bind(this)
        refreshIfOpenedFromNotification(intent)

        // Both bars are forced to the dark style rather than left on the
        // default. Bare enableEdgeToEdge() uses SystemBarStyle.auto, which
        // follows the *system* theme — so on a phone in light mode it would
        // pick dark status-bar icons and render them invisible against this
        // app's #111111. The app has no light variant, so neither should the
        // system bars.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        // The container was started by AsmaraApplication and outlives this
        // activity, so a recreation reuses it instead of building another.
        setContent {
            App(KoinPlatform.getKoin())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        refreshIfOpenedFromNotification(intent)
    }

    /**
     * A notification tap means the server has something the cache may not —
     * possibly within the 5-minute TTL, which a plain launch would honour and
     * so miss it. Forced, for that reason.
     */
    private fun refreshIfOpenedFromNotification(intent: Intent?) {
        if (intent?.hasExtra(AsmaraMessagingService.EXTRA_ANNOUNCEMENT_ID) != true) return
        val repository = KoinPlatform.getKoin().get<AnnouncementRepository>()
        lifecycleScope.launch { repository.refresh(force = true) }
    }
}

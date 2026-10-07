package com.conference.asmara.push

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

/**
 * The Android 13+ `POST_NOTIFICATIONS` runtime prompt.
 *
 * A runtime permission can only be requested through an activity, and the
 * registrar lives in the process-wide DI container, which must not hold one.
 * So the activity [bind]s itself here on creation and is released on
 * destruction; a request with no activity bound simply reports "not allowed"
 * and is asked again next time.
 */
object AndroidNotificationPermission {
    private var requester: (suspend () -> Boolean)? = null

    /** Call from `onCreate`, before the activity is started — ActivityResult registration requires it. */
    fun bind(activity: ComponentActivity) {
        val results = Channel<Boolean>(Channel.CONFLATED)
        val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            results.trySend(it)
        }
        val bound: suspend () -> Boolean = {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            results.receive()
        }
        requester = bound
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    if (requester === bound) requester = null
                }
            },
        )
    }

    suspend fun request(context: Context): Boolean {
        // Below API 33 notifications are allowed unless the user turned them off in settings.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        if (isGranted(context)) return true
        val ask = requester ?: return false
        return withContext(Dispatchers.Main) { ask() }
    }

    private fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}

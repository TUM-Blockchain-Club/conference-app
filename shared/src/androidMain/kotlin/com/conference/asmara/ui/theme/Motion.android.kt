package com.conference.asmara.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android has no single "reduce motion" switch. The closest signal available to
 * an app is the global animator duration scale, which is what both Developer
 * Options -> "Animator duration scale: Off" and the accessibility
 * "Remove animations" toggle write to. Zero means the user wants no animation.
 */
@Composable
actual fun reduceMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

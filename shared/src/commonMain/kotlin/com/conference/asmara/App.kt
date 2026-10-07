package com.conference.asmara

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import com.conference.asmara.ui.RootScreen
import com.conference.asmara.ui.theme.TbcTheme
import org.koin.compose.KoinContext
import org.koin.core.Koin

/**
 * @param koin an already-started container — see
 *   [com.conference.asmara.di.initKoin]. Taken as a parameter rather than built
 *   here so the container outlives the composition: an Android activity
 *   recreation reuses it instead of building a second `SupabaseClient` and
 *   SQLite driver, and code that runs with no UI at all can reach it.
 */
@Composable
fun App(koin: Koin) {
    KoinContext(koin) {
        // TbcTheme sits inside KoinContext rather than outside it because
        // reduceMotionEnabled() reads LocalContext on Android, which must
        // already be in scope.
        TbcTheme {
            // The explicit root Surface is not redundant with TbcScaffold. Voyager
            // animates screens by translating them, and mid-transition the gap
            // between the outgoing and incoming screen exposes whatever is behind
            // the navigator — the window background, which is not #111111 on every
            // platform. This paints the canvas once, below navigation.
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                // The tab shell, not a tab: EventDetailScreen pushes onto this
                // same navigator and so renders over the tab bar.
                Navigator(RootScreen())
            }
        }
    }
}

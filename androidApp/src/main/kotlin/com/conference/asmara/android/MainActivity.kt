package com.conference.asmara.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.conference.asmara.App
import com.conference.asmara.androidPlatformModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        setContent {
            App(androidPlatformModule(applicationContext))
        }
    }
}

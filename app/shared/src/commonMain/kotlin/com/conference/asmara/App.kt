package com.conference.asmara

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.conference.asmara.di.appModule
import com.conference.asmara.navigation.HomeScreen
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = { modules(appModule) }) {
        Navigator(HomeScreen())
    }
}

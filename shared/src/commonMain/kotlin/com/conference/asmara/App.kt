package com.conference.asmara

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.conference.asmara.di.appModule
import com.conference.asmara.di.dataModule
import com.conference.asmara.navigation.HomeScreen
import org.koin.compose.KoinApplication
import org.koin.core.module.Module

@Composable
fun App(platformModule: Module) {
    KoinApplication(application = { modules(platformModule, appModule, dataModule) }) {
        Navigator(HomeScreen())
    }
}

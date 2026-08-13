package com.conference.asmara

import android.content.Context
import com.conference.asmara.data.local.DriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidPlatformModule(context: Context): Module = module {
    single { DriverFactory(context) }
}

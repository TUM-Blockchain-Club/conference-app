package com.conference.asmara

import com.conference.asmara.data.local.DriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

fun iosPlatformModule(): Module = module {
    single { DriverFactory() }
}

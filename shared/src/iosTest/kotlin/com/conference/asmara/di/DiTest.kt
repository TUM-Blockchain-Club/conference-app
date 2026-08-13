package com.conference.asmara.di

import com.conference.asmara.iosPlatformModule
import org.koin.test.check.checkModules
import kotlin.test.Test

/** Repeatable DI wiring check, replacing hand-editing a screen to see if it crashes. */
class DiTest {
    @Test
    fun allModulesResolve() {
        checkModules {
            modules(iosPlatformModule(), appModule, dataModule)
        }
    }
}

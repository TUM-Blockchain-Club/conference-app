package com.conference.asmara.di

import com.conference.asmara.iosPlatformModule
import com.conference.asmara.ui.detail.EventDetailScreenModel
import org.koin.test.check.checkModules
import kotlin.test.Test

/** Repeatable DI wiring check, replacing hand-editing a screen to see if it crashes. */
class DiTest {
    @Test
    fun allModulesResolve() {
        checkModules(
            // EventDetailScreenModel is a parameterised factory, so the check
            // needs an id to build one with.
            parameters = { withParameter<EventDetailScreenModel> { "event-id" } },
        ) {
            modules(iosPlatformModule(), appModule, dataModule)
        }
    }
}

package com.conference.asmara.di

import com.conference.asmara.ui.detail.EventDetailScreenModel
import com.conference.asmara.ui.map.MapFocusRequests
import com.conference.asmara.ui.map.MapScreenModel
import com.conference.asmara.ui.schedule.ScheduleScreenModel
import org.koin.dsl.module

// factory, not single: Voyager's ScreenModelStore owns and disposes these
// instances, so a singleton would hand back a model whose scope is already
// cancelled once the screen has been popped.
val appModule = module {
    factory { ScheduleScreenModel(get()) }
    factory { MapScreenModel(get(), get()) }
    factory { (eventId: String) -> EventDetailScreenModel(eventId, get(), get()) }

    // single, and deliberately so: this one outlives every screen that touches
    // it. A "show on map" request is made on the detail screen, survives that
    // screen being popped, and is read by the shell — a factory would hand each
    // of them its own empty flow.
    single { MapFocusRequests() }
}

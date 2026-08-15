package com.conference.asmara.di

import com.conference.asmara.ui.detail.EventDetailScreenModel
import com.conference.asmara.ui.schedule.ScheduleScreenModel
import org.koin.dsl.module

// factory, not single: Voyager's ScreenModelStore owns and disposes these
// instances, so a singleton would hand back a model whose scope is already
// cancelled once the screen has been popped.
val appModule = module {
    factory { ScheduleScreenModel(get()) }
    factory { (eventId: String) -> EventDetailScreenModel(eventId, get()) }
}

package com.conference.asmara.di

import com.conference.asmara.network.ApiService
import com.conference.asmara.network.httpClient
import org.koin.dsl.module

val appModule = module {
    single { httpClient() }
    single { ApiService(get()) }
}

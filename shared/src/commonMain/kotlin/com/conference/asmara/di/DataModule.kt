package com.conference.asmara.di

import com.conference.asmara.data.local.DriverFactory
import com.conference.asmara.data.local.ScheduleLocalDataSource
import com.conference.asmara.data.remote.ScheduleRemoteDataSource
import com.conference.asmara.data.remote.SupabaseScheduleRemoteDataSource
import com.conference.asmara.data.remote.createScheduleSupabaseClient
import com.conference.asmara.data.repository.ScheduleRepositoryImpl
import com.conference.asmara.db.ScheduleDatabase
import com.conference.asmara.domain.repository.ScheduleRepository
import io.github.jan.supabase.SupabaseClient
import org.koin.dsl.module

val dataModule = module {
    single<SupabaseClient> { createScheduleSupabaseClient() }
    single { ScheduleDatabase(get<DriverFactory>().createDriver()) }
    single { ScheduleLocalDataSource(get()) }
    single<ScheduleRemoteDataSource> { SupabaseScheduleRemoteDataSource(get()) }
    single<ScheduleRepository> { ScheduleRepositoryImpl(get(), get()) }
}

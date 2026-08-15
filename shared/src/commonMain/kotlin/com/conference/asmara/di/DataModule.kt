package com.conference.asmara.di

import com.conference.asmara.data.local.DriverFactory
import com.conference.asmara.data.local.ScheduleLocalDataSource
import com.conference.asmara.data.local.VenueMapLocalDataSource
import com.conference.asmara.data.remote.ScheduleRemoteDataSource
import com.conference.asmara.data.remote.SupabaseScheduleRemoteDataSource
import com.conference.asmara.data.remote.SupabaseVenueMapRemoteDataSource
import com.conference.asmara.data.remote.VenueMapRemoteDataSource
import com.conference.asmara.data.remote.createScheduleSupabaseClient
import com.conference.asmara.data.repository.ScheduleRepositoryImpl
import com.conference.asmara.data.repository.VenueMapRepositoryImpl
import com.conference.asmara.db.ScheduleDatabase
import com.conference.asmara.domain.repository.ScheduleRepository
import com.conference.asmara.domain.repository.VenueMapRepository
import io.github.jan.supabase.SupabaseClient
import org.koin.dsl.module

val dataModule = module {
    single<SupabaseClient> { createScheduleSupabaseClient() }
    // One database for both caches: the venue map is a single small row, and a
    // second SQLite file would mean a second driver and a second schema for it.
    single { ScheduleDatabase(get<DriverFactory>().createDriver()) }
    single { ScheduleLocalDataSource(get()) }
    single<ScheduleRemoteDataSource> { SupabaseScheduleRemoteDataSource(get()) }
    single<ScheduleRepository> { ScheduleRepositoryImpl(get(), get()) }
    single { VenueMapLocalDataSource(get()) }
    single<VenueMapRemoteDataSource> { SupabaseVenueMapRemoteDataSource(get()) }
    single<VenueMapRepository> { VenueMapRepositoryImpl(get(), get()) }
}

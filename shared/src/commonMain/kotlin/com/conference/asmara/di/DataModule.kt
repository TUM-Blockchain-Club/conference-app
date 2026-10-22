package com.conference.asmara.di

import com.conference.asmara.data.local.AnnouncementLocalDataSource
import com.conference.asmara.data.local.DriverFactory
import com.conference.asmara.data.local.ScheduleLocalDataSource
import com.conference.asmara.data.local.VenueMapLocalDataSource
import com.conference.asmara.data.remote.AnnouncementRemoteDataSource
import com.conference.asmara.data.remote.ScheduleRemoteDataSource
import com.conference.asmara.data.remote.SupabaseAnnouncementRemoteDataSource
import com.conference.asmara.data.remote.SupabaseScheduleRemoteDataSource
import com.conference.asmara.data.remote.SupabaseVenueMapRemoteDataSource
import com.conference.asmara.data.remote.VenueMapRemoteDataSource
import com.conference.asmara.data.remote.createScheduleSupabaseClient
import com.conference.asmara.data.repository.AnnouncementRepositoryImpl
import com.conference.asmara.data.repository.NotificationPreferencesRepositoryImpl
import com.conference.asmara.data.repository.ScheduleRepositoryImpl
import com.conference.asmara.data.repository.VenueMapRepositoryImpl
import com.conference.asmara.db.ScheduleDatabase
import com.conference.asmara.domain.repository.AnnouncementRepository
import com.conference.asmara.domain.repository.NotificationPreferencesRepository
import com.conference.asmara.domain.repository.ScheduleRepository
import com.conference.asmara.domain.repository.VenueMapRepository
import com.conference.asmara.push.DeviceTokenRegistry
import com.conference.asmara.push.PushTokenSyncer
import com.conference.asmara.push.SupabaseDeviceTokenRegistry
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val dataModule = module {
    single<SupabaseClient> { createScheduleSupabaseClient() }
    // One database for every cache: the venue map is a single small row and the
    // announcements a handful, and a second SQLite file would mean a second
    // driver and a second schema for each.
    single { ScheduleDatabase(get<DriverFactory>().createDriver()) }
    single { ScheduleLocalDataSource(get()) }
    single<ScheduleRemoteDataSource> { SupabaseScheduleRemoteDataSource(get()) }
    single<ScheduleRepository> { ScheduleRepositoryImpl(get(), get()) }
    single { VenueMapLocalDataSource(get()) }
    single<VenueMapRemoteDataSource> { SupabaseVenueMapRemoteDataSource(get()) }
    single<VenueMapRepository> { VenueMapRepositoryImpl(get(), get()) }
    single { AnnouncementLocalDataSource(get()) }
    single<AnnouncementRemoteDataSource> { SupabaseAnnouncementRemoteDataSource(get()) }
    single<AnnouncementRepository> { AnnouncementRepositoryImpl(get(), get()) }
    single<NotificationPreferencesRepository> { NotificationPreferencesRepositoryImpl(get()) }

    // PushRegistrar and PushPlatform come from the platform module, which is
    // where PushConfig.ENABLED decides between the real registrar and the no-op.
    single<DeviceTokenRegistry> { SupabaseDeviceTokenRegistry(get()) }
    // Process-lifetime scope: the syncer outlives every screen, and on Android
    // it must keep running while no activity exists.
    single { PushTokenSyncer(get(), get(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}

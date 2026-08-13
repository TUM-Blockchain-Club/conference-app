package com.conference.asmara.data.remote

import com.conference.asmara.data.dto.EventDto

/** Interface so tests can substitute a fake instead of hitting Supabase. */
interface ScheduleRemoteDataSource {
    suspend fun fetchSchedule(): List<EventDto>
}

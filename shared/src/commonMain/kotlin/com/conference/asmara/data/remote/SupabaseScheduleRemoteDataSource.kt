package com.conference.asmara.data.remote

import com.conference.asmara.data.dto.EventDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

/**
 * Single round-trip nested PostgREST select. RLS restricts anon/authenticated
 * reads to published events (and their speakers) server-side, so no
 * `is_published` filter is needed here.
 */
private const val SCHEDULE_COLUMNS =
    "id,slug,title,description,start_time,end_time,event_type,is_published," +
        "track:tracks(*),location:locations(*)," +
        "event_speakers(role,sort_order,speaker:speakers(*))"

class SupabaseScheduleRemoteDataSource(
    private val client: SupabaseClient,
) : ScheduleRemoteDataSource {
    override suspend fun fetchSchedule(): List<EventDto> =
        client.postgrest.from("events")
            .select(columns = Columns.raw(SCHEDULE_COLUMNS))
            .decodeList()
}

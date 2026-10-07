package com.conference.asmara.data.remote

import com.conference.asmara.data.dto.AnnouncementDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/**
 * RLS restricts anon reads to published announcements server-side, so no
 * `is_published` filter is needed here — the same arrangement as the schedule.
 */
private const val ANNOUNCEMENT_COLUMNS = "id,slug,title,body,category,published_at"

class SupabaseAnnouncementRemoteDataSource(private val client: SupabaseClient) : AnnouncementRemoteDataSource {
    override suspend fun fetchAnnouncements(): List<AnnouncementDto> = client.postgrest.from("announcements")
        .select(columns = Columns.raw(ANNOUNCEMENT_COLUMNS)) {
            order("published_at", Order.DESCENDING)
        }
        .decodeList()
}

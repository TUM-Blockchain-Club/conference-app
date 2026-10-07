package com.conference.asmara.data.remote

import com.conference.asmara.data.dto.AnnouncementDto

/** Interface so tests can substitute a fake instead of hitting Supabase. */
interface AnnouncementRemoteDataSource {
    suspend fun fetchAnnouncements(): List<AnnouncementDto>
}

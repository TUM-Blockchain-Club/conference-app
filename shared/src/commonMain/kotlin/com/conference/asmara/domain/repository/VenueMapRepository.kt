package com.conference.asmara.domain.repository

import com.conference.asmara.domain.model.VenueMap
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Same contract as [ScheduleRepository], down to reusing [SyncResult] and
 * [Outcome]: the offline story is identical, and a second vocabulary for it
 * would be two things to keep in step.
 *
 * The one difference is emptiness. `observeSchedule()` emits an empty list both
 * for "not loaded yet" and "nothing published"; a venue map is a single
 * document, so it emits `null` until one exists and the caller can tell the two
 * apart with [SyncResult.outcome].
 */
interface VenueMapRepository {
    /** Emits from the local cache; unaffected by [refresh]'s outcome. Survives process death. */
    fun observeVenueMap(): Flow<VenueMap?>

    suspend fun refresh(force: Boolean = false): Result<SyncResult>

    suspend fun lastSyncedAt(): Instant?
}

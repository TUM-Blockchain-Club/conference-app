package com.conference.asmara.data.repository

import com.conference.asmara.data.local.VenueMapLocalDataSource
import com.conference.asmara.data.remote.DEFAULT_VENUE_SLUG
import com.conference.asmara.data.remote.VenueMapRemoteDataSource
import com.conference.asmara.domain.model.VenueMap
import com.conference.asmara.domain.repository.Outcome
import com.conference.asmara.domain.repository.SyncResult
import com.conference.asmara.domain.repository.VenueMapRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * 24 hours, against the schedule's 15 minutes.
 *
 * A talk can be moved an hour before it starts; a wall cannot. The map changes
 * between conferences, not during one, so re-fetching it every quarter hour
 * would be pure battery and bandwidth on a document nobody edited.
 */
private val CACHE_TTL = 24.hours

/**
 * DB is the single source of truth, exactly as in [ScheduleRepositoryImpl]:
 * [observeVenueMap] always reads the cache and [refresh] only decides whether
 * to repopulate it. A failed refresh with a warm cache still reports success,
 * so callers can distinguish stale-but-served from truly failed.
 */
class VenueMapRepositoryImpl(
    private val remote: VenueMapRemoteDataSource,
    private val local: VenueMapLocalDataSource,
    private val venueSlug: String = DEFAULT_VENUE_SLUG,
    private val clock: Clock = Clock.System,
) : VenueMapRepository {

    override fun observeVenueMap(): Flow<VenueMap?> = local.observeVenueMap()

    override suspend fun refresh(force: Boolean): Result<SyncResult> {
        val lastSynced = local.lastSyncedAt()
        val isFresh = lastSynced != null && (clock.now() - lastSynced) < CACHE_TTL
        if (!force && isFresh) {
            return Result.success(SyncResult(Outcome.SKIPPED_FRESH, lastSynced, local.featureCount()))
        }

        return try {
            val document = remote.fetchVenueMap(venueSlug)
            val syncedAt = clock.now()
            if (document == null) {
                // The server answered, and its answer is "there is no map".
                // Clearing is the honest response: keeping a cached map for a
                // venue that has been withdrawn would show rooms that are no
                // longer part of the conference.
                local.clear()
                Result.success(SyncResult(Outcome.REFRESHED, syncedAt, 0))
            } else {
                local.replace(venueSlug, document, syncedAt)
                Result.success(SyncResult(Outcome.REFRESHED, syncedAt, local.featureCount()))
            }
        } catch (e: Exception) {
            val cachedCount = local.featureCount()
            if (cachedCount > 0) {
                Result.success(SyncResult(Outcome.FAILED_SERVING_CACHE, lastSynced, cachedCount))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun lastSyncedAt(): Instant? = local.lastSyncedAt()
}

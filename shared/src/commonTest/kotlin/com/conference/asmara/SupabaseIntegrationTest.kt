package com.conference.asmara

import com.conference.asmara.data.dto.VenueMapDto
import com.conference.asmara.data.mapper.toDomain
import com.conference.asmara.data.remote.DEFAULT_VENUE_SLUG
import com.conference.asmara.data.remote.SupabaseScheduleRemoteDataSource
import com.conference.asmara.data.remote.SupabaseVenueMapRemoteDataSource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

expect fun getEnvVar(name: String): String?

/**
 * Opt-in end-to-end test: skipped unless SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY
 * are set in the environment, since it hits the real backend.
 *
 * Deliberately uses runBlocking, not runTest: runTest's virtual time collapses
 * Ktor's internal request-timeout delay() instantly, firing a spurious
 * HttpRequestTimeoutException on any real (uncontrolled) network call.
 */
class SupabaseIntegrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun anonClient(): SupabaseClient? {
        val url = getEnvVar("SUPABASE_URL") ?: return null
        val key = getEnvVar("SUPABASE_PUBLISHABLE_KEY") ?: return null
        return createSupabaseClient(url, key) { install(Postgrest) }
    }

    @Test
    fun fetchScheduleDecodesAndReturnsOnlyPublishedEvents() = runBlocking {
        val client = anonClient() ?: return@runBlocking
        val remote = SupabaseScheduleRemoteDataSource(client)

        val events = remote.fetchSchedule()

        assertTrue(events.all { it.isPublished })
    }

    /**
     * The one thing unit tests cannot cover: that the `rpc()` call shape, the
     * argument name and the jsonb-scalar response actually line up with the
     * migration. Everything downstream of the returned string is covered by
     * `VenueMapMapperTest`.
     */
    @Test
    fun fetchVenueMapReturnsOneParseableDocument() = runBlocking {
        val client = anonClient() ?: return@runBlocking
        val remote = SupabaseVenueMapRemoteDataSource(client)

        val document = remote.fetchVenueMap(DEFAULT_VENUE_SLUG) ?: return@runBlocking
        val map = json.decodeFromString<VenueMapDto>(document).toDomain()

        assertTrue(map.levels.isNotEmpty(), "seeded venue should have levels")
        assertTrue(map.levels.all { level -> level.features.all { it.name.isNotBlank() } })
        // Levels come back in ordinal order, and at least one room carries the
        // schedule join that the whole feature hangs off.
        assertTrue(map.levels.map { it.ordinal } == map.levels.map { it.ordinal }.sorted())
        assertTrue(map.levels.any { level -> level.features.any { it.locationId != null } })
    }

    @Test
    fun anUnknownVenueSlugReadsAsNoMapRatherThanAnError() = runBlocking {
        val client = anonClient() ?: return@runBlocking
        assertNull(SupabaseVenueMapRemoteDataSource(client).fetchVenueMap("no-such-venue"))
    }
}

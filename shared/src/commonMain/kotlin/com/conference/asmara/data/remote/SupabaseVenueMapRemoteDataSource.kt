package com.conference.asmara.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The slug the app asks for. One venue per conference, so this is a constant
 * rather than configuration — a second venue is a product decision that would
 * need a picker in the UI, not a build flag.
 */
const val DEFAULT_VENUE_SLUG: String = "tbc-conference"

/**
 * One RPC, one round-trip, one document.
 *
 * `get_venue_map()` runs `security invoker`, so the public-read RLS policies on
 * `venues` / `venue_levels` / `map_features` still apply — this is not a way
 * around them.
 */
class SupabaseVenueMapRemoteDataSource(
    private val client: SupabaseClient,
) : VenueMapRemoteDataSource {

    override suspend fun fetchVenueMap(venueSlug: String): String? {
        val result = client.postgrest.rpc(
            function = "get_venue_map",
            parameters = buildJsonObject { put("venue_slug", venueSlug) },
        )
        // A jsonb-returning function with no matching venue returns SQL NULL,
        // which PostgREST serialises as the four characters `null`. That is
        // "no map published yet", not a failure.
        return result.data.takeIf { it.isNotBlank() && it != "null" }
    }
}

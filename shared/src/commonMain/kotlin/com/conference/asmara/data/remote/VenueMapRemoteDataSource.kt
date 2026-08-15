package com.conference.asmara.data.remote

/** Interface so tests can substitute a fake instead of hitting Supabase. */
interface VenueMapRemoteDataSource {
    /**
     * The raw `get_venue_map()` document, or null when no venue with that slug
     * exists yet.
     *
     * Returns JSON text rather than a decoded DTO because the cache stores the
     * document verbatim: decoding here and re-encoding to write it would be two
     * conversions and one more place for the geometry shape to drift.
     */
    suspend fun fetchVenueMap(venueSlug: String): String?
}

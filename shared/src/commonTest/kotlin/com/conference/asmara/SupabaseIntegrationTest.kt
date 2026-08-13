package com.conference.asmara

import com.conference.asmara.data.remote.SupabaseScheduleRemoteDataSource
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
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
    @Test
    fun fetchScheduleDecodesAndReturnsOnlyPublishedEvents() = runBlocking {
        val url = getEnvVar("SUPABASE_URL") ?: return@runBlocking
        val key = getEnvVar("SUPABASE_PUBLISHABLE_KEY") ?: return@runBlocking

        val client = createSupabaseClient(url, key) { install(Postgrest) }
        val remote = SupabaseScheduleRemoteDataSource(client)

        val events = remote.fetchSchedule()

        assertTrue(events.all { it.isPublished })
    }
}

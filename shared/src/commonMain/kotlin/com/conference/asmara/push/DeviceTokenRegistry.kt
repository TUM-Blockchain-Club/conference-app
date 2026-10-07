package com.conference.asmara.push

import com.conference.asmara.domain.model.AnnouncementCategory
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** The server side of registration. An interface so tests can count calls without a network. */
interface DeviceTokenRegistry {
    suspend fun register(token: String, platform: PushPlatform, categories: Set<AnnouncementCategory>)
}

/**
 * Calls `register_device_token`, the only write path `anon` has into this
 * schema. It returns nothing by design — see the migration — so there is
 * nothing to decode.
 */
class SupabaseDeviceTokenRegistry(private val client: SupabaseClient) : DeviceTokenRegistry {
    override suspend fun register(token: String, platform: PushPlatform, categories: Set<AnnouncementCategory>) {
        client.postgrest.rpc(
            function = "register_device_token",
            parameters = buildJsonObject {
                put("p_token", token)
                put("p_platform", platform.wireName)
                putJsonArray("p_categories") {
                    categories.map { it.wireName }.sorted().forEach { add(it) }
                }
            },
        )
    }
}

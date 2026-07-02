package com.conference.asmara.network

import com.conference.asmara.common.api.Routes
import com.conference.asmara.common.model.Session
import com.conference.asmara.common.model.Speaker
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class ApiService(private val client: HttpClient) {
    private val baseUrl = "https://httpbin.org"

    suspend fun fetchData(): String = client.get("$baseUrl/get").bodyAsText()

    suspend fun getSessions(): List<Session> = client.get("$baseUrl${Routes.SESSIONS}").body()

    suspend fun getSpeakers(): List<Speaker> = client.get("$baseUrl${Routes.SPEAKERS}").body()
}

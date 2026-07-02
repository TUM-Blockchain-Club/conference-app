package com.conference.asmara.network

import com.conference.asmara.common.api.Routes
import com.conference.asmara.common.model.Session
import com.conference.asmara.common.model.Speaker
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ApiService(
    private val client: HttpClient,
    private val baseUrl: String = serverBaseUrl(),
) {
    suspend fun getSessions(): List<Session> = client.get("$baseUrl${Routes.SESSIONS}").body()

    suspend fun getSpeakers(): List<Speaker> = client.get("$baseUrl${Routes.SPEAKERS}").body()

    suspend fun createSession(session: Session): Session = client.post("$baseUrl${Routes.SESSIONS}") {
        contentType(ContentType.Application.Json)
        setBody(session)
    }.body()
}

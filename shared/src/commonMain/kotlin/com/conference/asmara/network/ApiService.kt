package com.conference.asmara.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class ApiService(private val client: HttpClient) {
    suspend fun fetchData(): String = client.get("https://httpbin.org/get").bodyAsText()
}

package com.conference.asmara.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.conference.asmara.common.model.Session
import com.conference.asmara.network.ApiService
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

class DetailScreen : Screen {
    @Composable
    override fun Content() {
        val apiService: ApiService = koinInject()
        val scope = rememberCoroutineScope()
        var result by remember { mutableStateOf("Press a button to test") }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("API Test")
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                scope.launch {
                    result = try {
                        val sessions = apiService.getSessions()
                        "Got ${sessions.size} sessions:\n${sessions.joinToString("\n") { it.title }}"
                    } catch (e: Exception) {
                        "Error: ${e.message}"
                    }
                }
            }) {
                Text("GET /sessions")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                scope.launch {
                    result = try {
                        val testSession = Session(
                            id = "test",
                            title = "Test Session",
                            description = "Created from app",
                            speakerIds = emptyList(),
                            startTime = Instant.parse("2026-07-10T09:00:00Z"),
                            endTime = Instant.parse("2026-07-10T10:00:00Z"),
                            room = "Room B",
                        )
                        val echoed = apiService.createSession(testSession)
                        "POST echoed: ${echoed.title}"
                    } catch (e: Exception) {
                        "Error: ${e.message}"
                    }
                }
            }) {
                Text("POST /sessions")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(result)
        }
    }
}

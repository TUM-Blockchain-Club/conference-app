package com.conference.asmara.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import com.conference.asmara.network.ApiService
import org.koin.compose.koinInject

class DetailScreen : Screen {
    @Composable
    override fun Content() {
        val apiService: ApiService = koinInject()
        var result by remember { mutableStateOf("Loading…") }

        LaunchedEffect(Unit) {
            result = try {
                apiService.fetchData()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Detail")
            Text(result)
        }
    }
}

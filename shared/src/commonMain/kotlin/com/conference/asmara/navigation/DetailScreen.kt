package com.conference.asmara.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.conference.asmara.network.ApiService
import com.conference.asmara.ui.components.Banner
import com.conference.asmara.ui.components.BannerStyle
import com.conference.asmara.ui.components.FieldLabel
import com.conference.asmara.ui.components.ScreenTitle
import com.conference.asmara.ui.components.TbcButton
import com.conference.asmara.ui.components.TbcButtonStyle
import com.conference.asmara.ui.components.TbcCard
import com.conference.asmara.ui.components.TbcScaffold
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme
import org.koin.compose.koinInject

/**
 * Placeholder detail screen, restyled onto the design system.
 *
 * The httpbin fetch through an injected [ApiService] is deliberately preserved:
 * it is the only place the dependency graph is exercised at runtime, so
 * deleting it would remove the smoke test along with the placeholder.
 */
class DetailScreen : Screen {

    private sealed interface State {
        data object Loading : State
        data class Loaded(val body: String) : State
        data class Failed(val message: String) : State
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val apiService: ApiService = koinInject()
        val spacing = TbcTheme.spacing
        var state by remember { mutableStateOf<State>(State.Loading) }

        LaunchedEffect(Unit) {
            state = try {
                State.Loaded(apiService.fetchData())
            } catch (e: Exception) {
                State.Failed(e.message ?: "Unknown error")
            }
        }

        TbcScaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenH, vertical = spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(spacing.xxl),
            ) {
                ScreenTitle(
                    title = "Detail",
                    subtitle = "Dependency graph smoke test",
                )

                when (val current = state) {
                    is State.Loading -> Banner(
                        text = "Fetching…",
                        style = BannerStyle.Info,
                    )

                    is State.Loaded -> TbcCard {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            FieldLabel("Response")
                            Text(
                                text = current.body,
                                style = TbcTheme.text.monoSmall,
                                color = TbcTheme.tokens.textMuted,
                            )
                        }
                    }

                    is State.Failed -> Banner(
                        text = current.message,
                        style = BannerStyle.Error,
                        title = "Request failed",
                    )
                }

                TbcButton(
                    text = "Back",
                    onClick = { navigator.pop() },
                    style = TbcButtonStyle.Secondary,
                    icon = TbcIcons.ArrowLeft,
                )
            }
        }
    }
}

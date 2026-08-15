package com.conference.asmara.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.conference.asmara.domain.model.Event
import com.conference.asmara.domain.model.EventType
import com.conference.asmara.ui.common.ConferenceTimeZone
import com.conference.asmara.ui.common.dateIn
import com.conference.asmara.ui.common.dayLabel
import com.conference.asmara.ui.common.timeRangeLabel
import com.conference.asmara.ui.detail.components.SpeakerRow
import com.conference.asmara.ui.schedule.components.TrackPill
import com.conference.asmara.ui.theme.accentColor
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf

/**
 * Carries only the id: Voyager `Screen`s are `rememberSaveable`d and serialized
 * on Android, so a domain [Event] in the constructor would compile and then
 * throw `NotSerializableException` on rotation or process death.
 */
data class EventDetailScreen(val eventId: String) : Screen {

    // Required. The default key is the qualified class name, identical for every
    // instance, so without this two sessions would share one ScreenModel.
    override val key: ScreenKey = "event-detail-$eventId"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val koin = getKoin()
        val screenModel = rememberScreenModel {
            koin.get<EventDetailScreenModel> { parametersOf(eventId) }
        }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Session") },
                    // No icon set on the shared classpath; hardware and gesture
                    // back are handled by Voyager regardless.
                    navigationIcon = {
                        TextButton(onClick = navigator::pop) { Text("Back") }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                when (val current = state) {
                    EventDetailUiState.Loading -> Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { CircularProgressIndicator() }

                    EventDetailUiState.NotFound -> Text(
                        text = "This session is no longer on the schedule.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp),
                    )

                    is EventDetailUiState.Content -> EventDetailContent(current.event)
                }
            }
        }
    }
}

@Composable
private fun EventDetailContent(event: Event, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "${event.startTime.dateIn(ConferenceTimeZone).dayLabel()} · " +
                timeRangeLabel(event.startTime, event.endTime, ConferenceTimeZone),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        event.location?.let { location ->
            Text(
                text = listOfNotNull(location.name, location.floor).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = event.eventType.label(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        event.track?.let { TrackPill(it.name, it.accentColor()) }

        event.description?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(Modifier.fillMaxWidth())
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (event.speakers.isNotEmpty()) {
            HorizontalDivider(Modifier.fillMaxWidth())
            Text(
                text = if (event.speakers.size == 1) "Speaker" else "Speakers",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            event.speakers.sortedBy { it.sortOrder }.forEach { SpeakerRow(it) }
        }
    }
}

private fun EventType.label(): String = when (this) {
    EventType.TALK -> "Talk"
    EventType.PANEL -> "Panel"
    EventType.WORKSHOP -> "Workshop"
    EventType.KEYNOTE -> "Keynote"
    EventType.BREAK -> "Break"
    EventType.OTHER -> "Session"
}

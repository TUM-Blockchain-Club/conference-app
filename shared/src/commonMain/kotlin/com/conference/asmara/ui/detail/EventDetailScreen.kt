package com.conference.asmara.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.conference.asmara.ui.components.ColorDot
import com.conference.asmara.ui.components.EmptyState
import com.conference.asmara.ui.components.ScreenFooter
import com.conference.asmara.ui.components.ScreenTitle
import com.conference.asmara.ui.components.SectionHeader
import com.conference.asmara.ui.components.TbcButton
import com.conference.asmara.ui.components.TbcButtonStyle
import com.conference.asmara.ui.components.TbcCard
import com.conference.asmara.ui.components.TbcIconButton
import com.conference.asmara.ui.components.TbcScaffold
import com.conference.asmara.ui.detail.components.SpeakerRow
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.map.MapFocusRequests
import com.conference.asmara.ui.theme.TbcTheme
import com.conference.asmara.ui.theme.eventTypeColor
import com.conference.asmara.ui.theme.trackColor
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

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val koin = getKoin()
        val screenModel = rememberScreenModel {
            koin.get<EventDetailScreenModel> { parametersOf(eventId) }
        }
        val state by screenModel.state.collectAsState()
        val focusRequests = remember(koin) { koin.get<MapFocusRequests>() }
        val spacing = TbcTheme.spacing

        TbcScaffold {
            Column(Modifier.fillMaxSize()) {
                // The system has no app bar, so back is a plain icon button at
                // the top of the content. Hardware and gesture back are handled
                // by Voyager regardless.
                TbcIconButton(
                    icon = TbcIcons.ArrowLeft,
                    contentDescription = "Back to schedule",
                    onClick = navigator::pop,
                    modifier = Modifier.padding(horizontal = spacing.sm),
                )
                when (val current = state) {
                    EventDetailUiState.Loading -> Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { CircularProgressIndicator(color = TbcTheme.tokens.accent) }

                    EventDetailUiState.NotFound -> EmptyState(
                        title = "Session not found",
                        description = "This session is no longer on the schedule.",
                        icon = TbcIcons.Calendar,
                    )

                    is EventDetailUiState.Content -> EventDetailContent(
                        event = current.event,
                        onShowOnMap = current.mappedLocationId?.let { locationId ->
                            {
                                // Request first, then pop: the shell reads the
                                // request as it comes back into composition, so
                                // the tab is already switching by the time the
                                // pop animation finishes.
                                focusRequests.request(locationId)
                                navigator.pop()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EventDetailContent(
    event: Event,
    modifier: Modifier = Modifier,
    onShowOnMap: (() -> Unit)? = null,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenH)
            .padding(top = spacing.sm, bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.xxl),
    ) {
        ScreenTitle(
            title = event.title,
            subtitle = event.startTime.dateIn(ConferenceTimeZone).dayLabel(),
        )

        TbcCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                DetailFact(
                    icon = TbcIcons.Clock,
                    text = timeRangeLabel(event.startTime, event.endTime, ConferenceTimeZone),
                )
                event.location?.let { location ->
                    DetailFact(
                        icon = TbcIcons.MapPin,
                        text = listOfNotNull(location.name, location.floor).joinToString(" · "),
                    )
                }
                if (onShowOnMap != null) {
                    // Tonal, not Primary: the screen's one primary action slot
                    // belongs to whatever the attendee came here to *do*, and
                    // this is a way to look at the room, not a commitment.
                    TbcButton(
                        text = "Show on map",
                        onClick = onShowOnMap,
                        style = TbcButtonStyle.Tonal,
                        icon = TbcIcons.Map,
                    )
                }
                event.track?.let { track ->
                    // A dot, because here the colour *is* the identity — it is
                    // the same marker this track carries in the list.
                    DetailFact(
                        dotColor = trackColor(track.color, track.sortOrder),
                        text = track.name,
                    )
                }
                // A glyph rather than a second dot. Track colours come from the
                // database and can land on the same hue as the event type's,
                // and two identical dots meaning different things is worse than
                // no colour at all.
                DetailFact(
                    icon = TbcIcons.Hexagon,
                    iconTint = eventTypeColor(event.eventType),
                    text = event.eventType.label(),
                )
            }
        }

        event.description?.takeIf { it.isNotBlank() }?.let { description ->
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                SectionHeader(title = "About")
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.textMuted,
                )
            }
        }

        if (event.speakers.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                SectionHeader(
                    title = if (event.speakers.size == 1) "Speaker" else "Speakers",
                    count = if (event.speakers.size > 1) "${event.speakers.size} total" else null,
                )
                event.speakers.sortedBy { it.sortOrder }.forEach { SpeakerRow(it) }
            }
        }

        ScreenFooter(text = "TUM Blockchain Club", modifier = Modifier.padding(top = spacing.lg))
    }
}

/**
 * One labelled fact about the session. Either a glyph or a category dot leads
 * it — a dot where the colour itself is the information (track, event type),
 * an icon where it is not.
 */
@Composable
private fun DetailFact(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    dotColor: Color? = null,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A fixed-width gutter, so a 16dp glyph and an 8dp dot leave their
        // labels aligned down the stack instead of stepping in and out.
        Box(
            modifier = Modifier.size(FactMarkerBox),
            contentAlignment = Alignment.Center,
        ) {
            when {
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: tokens.textMuted,
                    modifier = Modifier.size(FactIconSize),
                )
                dotColor != null -> ColorDot(dotColor)
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textPrimary,
        )
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

private val FactMarkerBox = 18.dp
private val FactIconSize = 16.dp

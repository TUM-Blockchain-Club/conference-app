package com.conference.asmara.ui.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.conference.asmara.domain.model.EventSpeaker
import com.conference.asmara.domain.model.SpeakerRole
import com.conference.asmara.ui.common.InitialsAvatar

@Composable
fun SpeakerRow(
    eventSpeaker: EventSpeaker,
    modifier: Modifier = Modifier,
) {
    val speaker = eventSpeaker.speaker
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // TODO(#4-followup): render speaker.photoUrl once speakers have photos.
        InitialsAvatar(speaker.name)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = speaker.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val affiliation = listOfNotNull(speaker.title, speaker.company).joinToString(" @ ")
            if (affiliation.isNotEmpty()) {
                Text(
                    text = affiliation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (eventSpeaker.role != SpeakerRole.SPEAKER) {
                Text(
                    text = eventSpeaker.role.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            speaker.bio?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun SpeakerRole.label(): String = when (this) {
    SpeakerRole.SPEAKER -> "Speaker"
    SpeakerRole.MODERATOR -> "Moderator"
    SpeakerRole.HOST -> "Host"
    SpeakerRole.OTHER -> "Participant"
}

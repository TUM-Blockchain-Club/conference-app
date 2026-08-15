package com.conference.asmara.ui.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.conference.asmara.domain.model.EventSpeaker
import com.conference.asmara.domain.model.SpeakerRole
import com.conference.asmara.ui.common.InitialsAvatar
import com.conference.asmara.ui.components.TbcBadge
import com.conference.asmara.ui.components.TbcCard
import com.conference.asmara.ui.theme.TbcTheme

@Composable
fun SpeakerRow(
    eventSpeaker: EventSpeaker,
    modifier: Modifier = Modifier,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    val speaker = eventSpeaker.speaker

    TbcCard(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            // TODO(#4-followup): render speaker.photoUrl once speakers have photos.
            InitialsAvatar(speaker.name)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = speaker.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.textPrimary,
                )
                val affiliation = listOfNotNull(speaker.title, speaker.company).joinToString(" · ")
                if (affiliation.isNotEmpty()) {
                    Text(
                        text = affiliation,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                    )
                }
                // Only worth the pixels when it is not the default. Badging
                // every speaker "Speaker" says nothing.
                if (eventSpeaker.role != SpeakerRole.SPEAKER) {
                    TbcBadge(text = eventSpeaker.role.label())
                }
                speaker.bio?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textMuted,
                    )
                }
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

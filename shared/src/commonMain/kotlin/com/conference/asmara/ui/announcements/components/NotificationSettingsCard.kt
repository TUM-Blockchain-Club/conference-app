package com.conference.asmara.ui.announcements.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.conference.asmara.domain.model.AnnouncementCategory
import com.conference.asmara.ui.announcements.CategoryToggle
import com.conference.asmara.ui.announcements.description
import com.conference.asmara.ui.announcements.label
import com.conference.asmara.ui.components.TbcCard
import com.conference.asmara.ui.components.TbcSwitch
import com.conference.asmara.ui.theme.TbcTheme

/**
 * Per-category opt-out. One toggle governs both what the feed shows and what
 * this device is sent as a push, so there is no second settings screen to keep
 * in step with this one.
 *
 * Each row is the toggleable, not the switch — the row form documented on
 * [TbcSwitch] — so the label is a tap target and is what a screen reader
 * announces alongside the state. A locked row (emergency) stays on and is not
 * focusable as a control; its description says why.
 */
@Composable
fun NotificationSettingsCard(
    categories: List<CategoryToggle>,
    onToggle: (AnnouncementCategory, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = TbcTheme.spacing
    TbcCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            categories.forEach { toggle ->
                CategoryRow(toggle = toggle, onToggle = { onToggle(toggle.category, it) })
            }
        }
    }
}

@Composable
private fun CategoryRow(toggle: CategoryToggle, onToggle: (Boolean) -> Unit) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = toggle.enabled,
                enabled = !toggle.locked,
                role = Role.Switch,
                onValueChange = onToggle,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = toggle.category.label(),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.textPrimary,
            )
            Text(
                text = toggle.category.description(),
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
        }
        TbcSwitch(checked = toggle.enabled, onCheckedChange = null, enabled = !toggle.locked)
    }
}

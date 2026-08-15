package com.conference.asmara.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.conference.asmara.domain.model.Track

/**
 * Track colours come from the database, so anything can be in there. Bad
 * content yields null and the caller falls back to a theme colour rather than
 * throwing.
 */
fun String?.toTrackColorOrNull(): Color? {
    val hex = this?.trim()?.removePrefix("#") ?: return null
    if (hex.length != 6 && hex.length != 8) return null
    val value = hex.toLongOrNull(16) ?: return null
    return Color(if (hex.length == 6) value or 0xFF000000L else value)
}

/**
 * Use the result only as a dot, border or leading stripe — never as text or a
 * fill. A saturated brand hex has no contrast guarantee against either surface.
 */
@Composable
fun Track?.accentColor(): Color =
    this?.color.toTrackColorOrNull() ?: MaterialTheme.colorScheme.secondary

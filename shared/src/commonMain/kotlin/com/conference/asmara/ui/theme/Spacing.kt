package com.conference.asmara.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A 4dp-based spacing ramp with two half-steps at the small end, where mobile
 * density actually needs them (icon-to-label gaps, badge padding).
 *
 * Lives in its own composition local rather than inside [TbcTokens] so that a
 * spacing change never invalidates colour readers.
 */
@Immutable
data class TbcSpacing(
    /** 2dp — optical nudges only. */
    val xxs: Dp = 2.dp,
    /** 4dp — icon-to-label, dot-to-text. */
    val xs: Dp = 4.dp,
    /** 8dp — inside a badge or pill, between tightly related rows. */
    val sm: Dp = 8.dp,
    /** 12dp — between list items. */
    val md: Dp = 12.dp,
    /** 16dp — the default gap. Card padding, between form fields. */
    val lg: Dp = 16.dp,
    /** 20dp — screen horizontal margin. */
    val xl: Dp = 20.dp,
    /** 24dp — between cards. */
    val xxl: Dp = 24.dp,
    /** 32dp — between major sections. */
    val x3l: Dp = 32.dp,
    /** 48dp — above a page footer, below a hero. */
    val x4l: Dp = 48.dp,

    /** Screen horizontal margin. Aliased so screens read intent, not a number. */
    val screenH: Dp = 20.dp,
    /** Interior padding of a [com.conference.asmara.ui.components.TbcCard]. */
    val cardPadding: Dp = 16.dp,
    /** WCAG / platform minimum for anything tappable. */
    val touchTarget: Dp = 48.dp,
)

val LocalTbcSpacing = staticCompositionLocalOf { TbcSpacing() }

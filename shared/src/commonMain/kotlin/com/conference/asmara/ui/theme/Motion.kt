package com.conference.asmara.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Duration and easing tokens.
 *
 * Motion in this system is decorative — it never carries information that is
 * not also carried by layout or colour. That is what makes honouring the
 * platform's reduce-motion setting a free win rather than a trade-off: with
 * [reduced] set, [d] collapses every duration to zero and the UI snaps.
 *
 * Always route a duration through [d]:
 * ```
 * val motion = TbcTheme.motion
 * animateFloatAsState(target, tween(motion.d(motion.standard)))
 * ```
 */
@Immutable
data class TbcMotion(
    /** 120ms — state layers, ripples, colour swaps. */
    val fast: Int = 120,
    /** 220ms — the default. Expand/collapse, tab indicator, content swap. */
    val standard: Int = 220,
    /** 320ms — screen-level transitions, sheets. */
    val slow: Int = 320,
    /** 800ms — the web's `fadeIn` / `scaleIn` entrance animations. */
    val entrance: Int = 800,
    /** Whether the platform has asked for reduced motion. */
    val reduced: Boolean = false,
) {
    /** `ease-out`, matching the web's `0.8s ease-out` entrances. */
    val easeOut: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    /** `ease-in-out` for anything that reverses. */
    val easeStandard: Easing = FastOutSlowInEasing

    /** The duration to actually pass to `tween`. Zero when motion is reduced. */
    fun d(duration: Int): Int = if (reduced) 0 else duration
}

val LocalTbcMotion = staticCompositionLocalOf { TbcMotion() }

/**
 * Whether the user has asked the OS to reduce motion.
 *
 * `@Composable` because the Android implementation needs `LocalContext`; this
 * is also why [TbcTheme] must sit *inside* any provider that supplies that
 * context.
 */
@Composable
expect fun reduceMotionEnabled(): Boolean

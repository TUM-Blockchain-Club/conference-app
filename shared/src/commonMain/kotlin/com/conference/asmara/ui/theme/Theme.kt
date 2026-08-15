package com.conference.asmara.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/**
 * The TBC design system.
 *
 * Wrap the whole app once, as high as possible but *below* anything that
 * provides a platform context — [reduceMotionEnabled] reads `LocalContext` on
 * Android.
 *
 * ```
 * KoinApplication(...) {
 *     TbcTheme { Navigator(HomeScreen()) }
 * }
 * ```
 *
 * The system is dark-only by design, not by omission: the source visual
 * language is a pure-neutral dark dashboard, and a light variant would need its
 * own contrast audit and its own surface ladder rather than an inversion.
 *
 * Note there is no global `ProvideTextStyle` here. Doing so would silently
 * reshape every stock Material 3 component that reads `LocalTextStyle`, which
 * is a much wider blast radius than it looks.
 */
@Composable
fun TbcTheme(content: @Composable () -> Unit) {
    val reduced = reduceMotionEnabled()
    val motion = remember(reduced) { TbcMotion(reduced = reduced) }
    val tokens = remember { tbcTokens() }
    val spacing = remember { TbcSpacing() }
    val typography = rememberTbcTypography()
    val textStyles = rememberTbcTextStyles()

    CompositionLocalProvider(
        LocalTbcTokens provides tokens,
        LocalTbcSpacing provides spacing,
        LocalTbcMotion provides motion,
        LocalTbcTextStyles provides textStyles,
    ) {
        MaterialTheme(
            colorScheme = tbcDarkColorScheme(),
            shapes = tbcShapes(),
            typography = typography,
            content = content,
        )
    }
}

/**
 * Accessors for the non-Material half of the system, mirroring how
 * [MaterialTheme] exposes `colorScheme` / `typography` / `shapes`.
 *
 * ```
 * val tokens = TbcTheme.tokens
 * val spacing = TbcTheme.spacing
 * ```
 */
object TbcTheme {

    /** Colours and the pill shape that Material 3 has no role for. */
    val tokens: TbcTokens
        @Composable @ReadOnlyComposable get() = LocalTbcTokens.current

    /** The 4dp spacing ramp. */
    val spacing: TbcSpacing
        @Composable @ReadOnlyComposable get() = LocalTbcSpacing.current

    /** Durations and easings, already reduce-motion aware via `motion.d(...)`. */
    val motion: TbcMotion
        @Composable @ReadOnlyComposable get() = LocalTbcMotion.current

    /** Monospace styles, which [Typography] cannot hold. */
    val text: TbcTextStyles
        @Composable @ReadOnlyComposable get() = LocalTbcTextStyles.current
}

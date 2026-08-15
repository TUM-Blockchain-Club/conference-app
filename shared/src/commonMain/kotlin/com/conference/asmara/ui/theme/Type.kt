package com.conference.asmara.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.conference.asmara.resources.Res
import com.conference.asmara.resources.geist_bold
import com.conference.asmara.resources.geist_medium
import com.conference.asmara.resources.geist_mono_regular
import com.conference.asmara.resources.geist_regular
import com.conference.asmara.resources.geist_semibold
import org.jetbrains.compose.resources.Font

/**
 * Geist, bundled as **static** TTFs.
 *
 * The variable `Geist[wght].ttf` is deliberately not used: Skia's
 * `FontVariation` support is uneven across targets and `minSdk 26` sits right
 * at the Android boundary where it stops being reliable. Four static weights
 * cost ~510 KB and behave identically everywhere.
 *
 * License: SIL OFL 1.1 — see `third_party/geist/OFL.txt`. The license file
 * deliberately does *not* live in `composeResources/font/`, where the resource
 * generator would try to map it to a `FontResource` and fail codegen.
 */
object TbcFonts {

    @Composable
    fun sans(): FontFamily = FontFamily(
        Font(Res.font.geist_regular, FontWeight.Normal, FontStyle.Normal),
        Font(Res.font.geist_medium, FontWeight.Medium, FontStyle.Normal),
        Font(Res.font.geist_semibold, FontWeight.SemiBold, FontStyle.Normal),
        Font(Res.font.geist_bold, FontWeight.Bold, FontStyle.Normal),
    )

    @Composable
    fun mono(): FontFamily = FontFamily(
        Font(Res.font.geist_mono_regular, FontWeight.Normal, FontStyle.Normal),
    )
}

/**
 * Styles that Material 3's [Typography] has no slot for.
 *
 * `Typography` carries exactly one family, so the mono face cannot live in it.
 * These ship alongside the tokens instead.
 */
@Immutable
data class TbcTextStyles(
    /** Timestamps, IDs, counts in a table-ish context. */
    val monoSmall: TextStyle,
    /** Tiny mono label — hex codes in the gallery, tracking codes. */
    val monoLabel: TextStyle,
)

val LocalTbcTextStyles = staticCompositionLocalOf<TbcTextStyles> {
    error("No TbcTextStyles found. Wrap your content in TbcTheme { }.")
}

/**
 * `org.jetbrains.compose.resources.Font(...)` is `@Composable`, which cascades:
 * a [FontFamily] cannot be a top-level `val`, so neither can a [Typography],
 * and neither can be built inside `remember { }` (no composable calls in a
 * remember lambda).
 *
 * The two-layer shape below is the standard escape: hoist the composable font
 * lookup, then hand the plain values to a non-composable builder that
 * `remember` can cache and a unit test can call directly.
 */
@Composable
internal fun rememberTbcTypography(): Typography {
    val sans = TbcFonts.sans()
    return remember(sans) { tbcTypography(sans) }
}

@Composable
internal fun rememberTbcTextStyles(): TbcTextStyles {
    val mono = TbcFonts.mono()
    return remember(mono) { tbcTextStyles(mono) }
}

/**
 * The scale, compressed from the web platform's desktop ramp.
 *
 * The source is a dashboard viewed at 1440px+; transcribing its sizes to a
 * 390pt phone would leave a "Dashboard" title 36px tall eating a fifth of the
 * viewport. Headlines come down hardest, body text barely moves.
 *
 * Two deliberate departures from stock M3:
 * - `labelLarge` is **15sp**, not 14. It is the button label — the loudest
 *   control on any screen — and the web sets 16px/500 there. M3's 14 reads
 *   cheap next to a 32px-tall pill.
 * - `labelSmall` carries 0.9sp tracking because it is only ever used uppercase
 *   (the field label), where default tracking makes letters collide.
 */
internal fun tbcTypography(sans: FontFamily): Typography {
    fun style(
        size: Int,
        lineHeight: Int,
        weight: FontWeight,
        tracking: Double = 0.0,
    ) = TextStyle(
        fontFamily = sans,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
    )

    return Typography(
        // Display — reserved for the numbers on stat tiles and hero figures.
        displayLarge = style(40, 46, FontWeight.Bold, -0.5),
        displayMedium = style(34, 40, FontWeight.Bold, -0.4),
        displaySmall = style(30, 36, FontWeight.Bold, -0.3),

        // Headline — page and section titles. Web "Dashboard" 36px -> 28sp.
        headlineLarge = style(28, 34, FontWeight.Bold, -0.3),
        headlineMedium = style(24, 30, FontWeight.SemiBold, -0.2),
        headlineSmall = style(20, 26, FontWeight.SemiBold, -0.1),

        // Title — card and group headers.
        titleLarge = style(18, 24, FontWeight.SemiBold),
        titleMedium = style(16, 22, FontWeight.Medium),
        titleSmall = style(14, 20, FontWeight.Medium),

        // Body — the reading sizes.
        bodyLarge = style(16, 24, FontWeight.Normal),
        bodyMedium = style(14, 20, FontWeight.Normal),
        bodySmall = style(12, 17, FontWeight.Normal),

        // Label — controls.
        labelLarge = style(15, 20, FontWeight.Medium),
        labelMedium = style(13, 18, FontWeight.Medium),
        labelSmall = style(11, 14, FontWeight.Medium, 0.9),
    )
}

internal fun tbcTextStyles(mono: FontFamily) = TbcTextStyles(
    monoSmall = TextStyle(
        fontFamily = mono,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
    ),
    monoLabel = TextStyle(
        fontFamily = mono,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.3.sp,
    ),
)

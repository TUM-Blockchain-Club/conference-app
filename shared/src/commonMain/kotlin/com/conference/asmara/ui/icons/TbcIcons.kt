package com.conference.asmara.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The icon set: Lucide geometry, hand-transcribed as [ImageVector]s.
 *
 * ### Why not Material icons
 *
 * The obvious move is `Icons.Filled.*` from `material-icons-core`, on the
 * assumption that `compose.material3` brings it transitively. It does not — not
 * in Compose Multiplatform 1.11.1 — and the artifact is **frozen at 1.7.3**:
 * JetBrains stopped publishing `material-icons-core` and `-extended` after that
 * release. Adding it would pin a 1.7.3 icon artifact against a 1.11.1 Compose
 * stack, which is the precise version skew that makes `materialIconsExtended`
 * a documented trap in this repo.
 *
 * Transcribing Lucide instead costs one file, no dependency, and no skew — and
 * it removes a divergence rather than adding one, since Lucide is what the web
 * platform actually draws. Material's filled glyphs would have been a visible
 * mismatch beside the reference screenshots.
 *
 * ### House style
 *
 * Every icon is a 24x24 outline on a 2px stroke with round caps and joins —
 * Lucide's defaults, and the reason the set looks coherent. Strokes carry no
 * fill, so [androidx.compose.material3.Icon]'s `tint` recolours them wholesale.
 *
 * ### Adding one
 *
 * Copy the `d` attributes out of the Lucide SVG (lucide.dev, ISC licensed) and
 * pass them to [lucide] in order. Do not redraw by hand and do not mix in a
 * filled glyph from elsewhere: the stroke weight is what holds the set together.
 * Circles are written as two half-arcs because SVG `<circle>` has no path form.
 */
object TbcIcons {

    // -- Navigation / identity ---------------------------------------------

    /** lucide `user` — profile, a single person. */
    val User: ImageVector by lazy {
        lucide(
            "User",
            "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2",
            circle(cx = 12f, cy = 7f, r = 4f),
        )
    }

    /** lucide `users` — a group, member lists, attendance counts. */
    val Users: ImageVector by lazy {
        lucide(
            "Users",
            "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
            circle(cx = 9f, cy = 7f, r = 4f),
            "M22 21v-2a4 4 0 0 0-3-3.87",
            "M16 3.13a4 4 0 0 1 0 7.75",
        )
    }

    /** lucide `calendar` — dates, schedule, events. */
    val Calendar: ImageVector by lazy {
        lucide(
            "Calendar",
            "M8 2v4",
            "M16 2v4",
            "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
            "M3 10h18",
        )
    }

    /** lucide `chart-column` — statistics. */
    val BarChart: ImageVector by lazy {
        lucide(
            "BarChart",
            "M3 3v16a2 2 0 0 0 2 2h16",
            "M18 17V9",
            "M13 17V5",
            "M8 17v-3",
        )
    }

    /** lucide `circle-check` — attendance, success, confirmation. */
    val CheckCircle: ImageVector by lazy {
        lucide(
            "CheckCircle",
            circle(cx = 12f, cy = 12f, r = 10f),
            "m9 12 2 2 4-4",
        )
    }

    /** lucide `hexagon` — NFT status, on-chain things. */
    val Hexagon: ImageVector by lazy {
        lucide(
            "Hexagon",
            "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z",
        )
    }

    /** lucide `log-out` — sign out. */
    val LogOut: ImageVector by lazy {
        lucide(
            "LogOut",
            "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4",
            "m16 17 5-5-5-5",
            "M21 12H9",
        )
    }

    /** lucide `building-2` — departments, organisations, venues. */
    val Building: ImageVector by lazy {
        lucide(
            "Building",
            "M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18Z",
            "M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2",
            "M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2",
            "M10 6h4",
            "M10 10h4",
            "M10 14h4",
            "M10 18h4",
        )
    }

    // -- Content -----------------------------------------------------------

    /** lucide `search`. */
    val Search: ImageVector by lazy {
        lucide(
            "Search",
            circle(cx = 11f, cy = 11f, r = 8f),
            "m21 21-4.35-4.35",
        )
    }

    /** lucide `map-pin` — locations, rooms. */
    val MapPin: ImageVector by lazy {
        lucide(
            "MapPin",
            "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0",
            circle(cx = 12f, cy = 10f, r = 3f),
        )
    }

    /** lucide `map` — the venue map. The folded plan, distinct from [MapPin]'s single place. */
    val Map: ImageVector by lazy {
        lucide(
            "Map",
            "M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894l4.553-2.277a2 2 0 0 1 1.788 0z",
            "M15 5.764v15",
            "M9 3.236v15",
        )
    }

    /** lucide `star` — favourites, featured sessions. */
    val Star: ImageVector by lazy {
        lucide(
            "Star",
            "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z",
        )
    }

    /** lucide `clock` — times, durations. */
    val Clock: ImageVector by lazy {
        lucide(
            "Clock",
            circle(cx = 12f, cy = 12f, r = 10f),
            "M12 6v6l4 2",
        )
    }

    /** lucide `lock` — admin-only fields. */
    val Lock: ImageVector by lazy {
        lucide(
            "Lock",
            "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z",
            "M7 11V7a5 5 0 0 1 10 0v4",
        )
    }

    // -- Status ------------------------------------------------------------

    /** lucide `info`. */
    val Info: ImageVector by lazy {
        lucide(
            "Info",
            circle(cx = 12f, cy = 12f, r = 10f),
            "M12 16v-4",
            "M12 8h.01",
        )
    }

    /** lucide `triangle-alert` — warnings. */
    val AlertTriangle: ImageVector by lazy {
        lucide(
            "AlertTriangle",
            "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3",
            "M12 9v4",
            "M12 17h.01",
        )
    }

    /** lucide `circle-x` — errors. A distinct glyph from [AlertTriangle] on purpose. */
    val ErrorCircle: ImageVector by lazy {
        lucide(
            "ErrorCircle",
            circle(cx = 12f, cy = 12f, r = 10f),
            "m15 9-6 6",
            "m9 9 6 6",
        )
    }

    /** lucide `x` — dismiss, close. */
    val Close: ImageVector by lazy {
        lucide(
            "Close",
            "M18 6 6 18",
            "m6 6 12 12",
        )
    }

    // -- Chevrons ----------------------------------------------------------

    /** lucide `chevron-right` — drill in. */
    val ChevronRight: ImageVector by lazy { lucide("ChevronRight", "m9 18 6-6-6-6") }

    /** lucide `chevron-down` — expand, select. */
    val ChevronDown: ImageVector by lazy { lucide("ChevronDown", "m6 9 6 6 6-6") }

    /** lucide `arrow-left` — back. */
    val ArrowLeft: ImageVector by lazy {
        lucide(
            "ArrowLeft",
            "m12 19-7-7 7-7",
            "M19 12H5",
        )
    }
}

/**
 * Builds a stroked 24dp icon from one or more SVG path strings.
 *
 * Paths are parsed rather than written in the `PathBuilder` DSL so that adding
 * an icon is a copy-paste from the Lucide source with no transcription step to
 * get wrong.
 */
private fun lucide(name: String, vararg pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = IconSize,
        defaultHeight = IconSize,
        viewportWidth = ViewportSize,
        viewportHeight = ViewportSize,
    ).apply {
        pathData.forEach { d ->
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                // No fill: these are outline icons. The colour is a placeholder
                // that Icon's tint replaces wholesale.
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = StrokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/**
 * A circle as a path.
 *
 * SVG's `<circle>` element has no `d` equivalent, so Lucide's circles are
 * rewritten here as two 180-degree arcs starting from the rightmost point.
 */
private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx + r} ${cy}a$r $r 0 1 1 ${-2 * r} 0a$r $r 0 1 1 ${2 * r} 0"

private val IconSize = 24.dp
private const val ViewportSize = 24f
private const val StrokeWidth = 2f

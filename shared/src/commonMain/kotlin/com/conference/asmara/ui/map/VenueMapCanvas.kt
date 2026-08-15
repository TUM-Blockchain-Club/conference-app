package com.conference.asmara.ui.map

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.conference.asmara.domain.model.FeatureShape
import com.conference.asmara.domain.model.MapCategory
import com.conference.asmara.domain.model.Point
import com.conference.asmara.domain.model.Polygon
import com.conference.asmara.domain.model.VenueLevel
import com.conference.asmara.ui.components.HairlineWidth
import com.conference.asmara.ui.theme.TbcTheme
import com.conference.asmara.ui.theme.mapCategoryColor
import com.conference.asmara.ui.theme.mapCategoryFill
import kotlin.math.max
import kotlin.math.min

/**
 * The floor plan itself: a pannable, pinchable vector drawing of one level.
 *
 * ### Why a Canvas
 *
 * The whole feature fits in `commonMain` this way — one implementation for both
 * platforms — and the part that is actually hard, the fit transform and the
 * point-in-polygon test, becomes plain Kotlin in `MapGeometry.kt` with unit
 * tests on it. A map library would have brought a basemap engine, an iOS build
 * restructure and a hand-written style JSON, to draw a dozen rooms that need no
 * tiles. See `docs/VENUE-MAP.md`.
 *
 * ### Accessibility
 *
 * A canvas is one node to a screen reader. There is no way to expose fifteen
 * rooms as fifteen focusable targets without hand-building a semantics tree,
 * and a tree of unlabelled shapes would not be an improvement. So the canvas
 * announces what it is and how much is on it, and the **search field above it
 * is the accessible route to selection** — it reaches every feature by name, on
 * any floor, with no gesture. What a selection then says is in the sheet below,
 * which is real text.
 */
@Composable
fun VenueMapCanvas(
    level: VenueLevel,
    selectedFeatureId: String?,
    onFeatureSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TbcTheme.tokens
    val spacing = TbcTheme.spacing
    val motion = TbcTheme.motion
    val density = LocalDensity.current
    val labelStyle = MaterialTheme.typography.labelMedium
    val textMeasurer = rememberTextMeasurer()

    // Reset on a floor change: carrying zoom across floors lands the attendee
    // on the next floor already scrolled into a corner of it.
    var zoom by remember(level.id) { mutableFloatStateOf(1f) }
    var pan by remember(level.id) { mutableStateOf(Offset.Zero) }

    val contentBounds = remember(level) { level.contentBounds() }
    val outlinePath = remember(level) { level.outline?.toVenuePath() }
    val areaPaths = remember(level) {
        level.features.mapNotNull { feature ->
            (feature.shape as? FeatureShape.Area)?.let { feature.id to it.polygon.toVenuePath() }
        }.toMap()
    }
    val areaWidths = remember(level) {
        level.features.mapNotNull { feature ->
            (feature.shape as? FeatureShape.Area)?.polygon
                ?.let { polygon -> bounds(listOf(polygon)) }
                ?.let { feature.id to it.width }
        }.toMap()
    }
    val labelPoints = remember(level) {
        level.features.mapNotNull { feature -> feature.labelPoint()?.let { feature.id to it } }.toMap()
    }
    // Measured once per floor, not per frame. Fifteen strings is not much, but
    // re-laying them out on every pan event is fifteen text layouts a frame for
    // a result that cannot have changed.
    val labels = remember(level, labelStyle, textMeasurer) {
        level.features.associate { it.id to textMeasurer.measure(it.name, labelStyle) }
    }

    val highlight by animateFloatAsState(
        targetValue = if (selectedFeatureId != null) 1f else 0f,
        animationSpec = tween(motion.d(motion.standard)),
        label = "mapHighlight",
    )

    val paddingPx = with(density) { spacing.lg.toPx() }
    val hairlinePx = with(density) { HairlineWidth.toPx() }
    val markerRadiusPx = with(density) { spacing.sm.toPx() }
    // Half a touch target: a pin should be as easy to hit as a button, and it
    // has no area of its own to be hit in.
    val touchSlopPx = with(density) { spacing.xxl.toPx() }
    val labelGapPx = with(density) { spacing.xs.toPx() }

    val description = remember(level) {
        "Floor plan of ${level.name}, ${level.features.size} places. " +
            "Use the search field above to select a place by name."
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = description }
            .pointerInput(level.id, contentBounds, paddingPx) {
                val venueBounds = contentBounds ?: return@pointerInput
                detectTransformGestures { touchCentroid, panChange, zoomChange, _ ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val base = fitTransform(venueBounds, width, height, paddingPx)
                    val nextZoom = (zoom * zoomChange).coerceIn(MinZoom, MaxZoom)
                    // The *effective* ratio, after clamping. Using zoomChange
                    // directly would keep moving the anchor once zoom has
                    // pinned at a limit, and the plan would crawl sideways
                    // under a pinch that is visibly doing nothing.
                    val ratio = nextZoom / zoom
                    val current = mapTransform(base, zoom, pan, width, height)

                    // Hold the venue point under the fingers still. The offset
                    // that does that is c(1 - k) + k·o, plus the drag.
                    val targetOffsetX = touchCentroid.x * (1f - ratio) + ratio * current.offsetX + panChange.x
                    val targetOffsetY = touchCentroid.y * (1f - ratio) + ratio * current.offsetY + panChange.y

                    zoom = nextZoom
                    pan = clampPan(
                        pan = Offset(
                            targetOffsetX - base.offsetX * nextZoom - width / 2f * (1f - nextZoom),
                            targetOffsetY - base.offsetY * nextZoom - height / 2f * (1f - nextZoom),
                        ),
                        base = base,
                        zoom = nextZoom,
                        bounds = venueBounds,
                        canvasWidth = width,
                        canvasHeight = height,
                    )
                }
            }
            .pointerInput(level.id, contentBounds, paddingPx) {
                val venueBounds = contentBounds ?: return@pointerInput
                detectTapGestures { tap ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val base = fitTransform(venueBounds, width, height, paddingPx)
                    val transform = mapTransform(base, zoom, pan, width, height)
                    val venuePoint = transform.toVenue(tap.x, tap.y)
                    // A pin's hit radius is a fixed number of *pixels*
                    // converted back to metres at the current zoom, so it stays
                    // finger-sized however far in the plan is.
                    val hit = hitTest(level.features, venuePoint, touchSlopPx / transform.scale)
                    // null is meaningful: tapping bare floor clears the selection.
                    onFeatureSelect(hit?.id)
                }
            },
    ) {
        val venueBounds = contentBounds ?: return@Canvas
        val base = fitTransform(venueBounds, size.width, size.height, paddingPx)
        val transform = mapTransform(base, zoom, pan, size.width, size.height)

        // Shapes are drawn inside a scaled transform so the paths can be built
        // once, in venue metres. Strokes divide the scale back out, so a
        // hairline is still a hairline at 8x and not an eight-pixel slab.
        withTransform({
            translate(transform.offsetX, transform.offsetY)
            scale(transform.scale, transform.scale, pivot = Offset.Zero)
        }) {
            val strokeWidth = hairlinePx / transform.scale

            outlinePath?.let { path ->
                drawPath(path, tokens.surfaceCard)
                drawPath(path, tokens.borderSubtle, style = Stroke(strokeWidth))
            }

            // Corridors first, and in the neutral: they are the negative space
            // the rooms sit in, and colouring them competes with the rooms.
            level.features.forEach { feature ->
                if (feature.category != MapCategory.CORRIDOR) return@forEach
                areaPaths[feature.id]?.let { drawPath(it, tokens.surfaceMuted) }
            }

            level.features.forEach { feature ->
                if (feature.category == MapCategory.CORRIDOR) return@forEach
                val path = areaPaths[feature.id] ?: return@forEach
                drawPath(path, mapCategoryFill(feature.category, tokens.surfaceCard))
                drawPath(path, tokens.borderSubtle, style = Stroke(strokeWidth))
            }

            // The highlight goes over every fill, so it reads on a corridor too.
            selectedFeatureId?.let { id ->
                areaPaths[id]?.let { path ->
                    drawPath(path, tokens.accentTint.copy(alpha = tokens.accentTint.alpha * highlight))
                    drawPath(
                        path,
                        tokens.accent.copy(alpha = highlight),
                        style = Stroke(strokeWidth * SelectedStrokeScale),
                    )
                }
            }
        }

        // Pins and text are drawn outside the transform: scaling them would
        // make a zoomed-in room's label the size of the room.
        level.features.forEach { feature ->
            val marker = (feature.shape as? FeatureShape.Marker)?.point ?: return@forEach
            val center = Offset(transform.toCanvasX(marker.x), transform.toCanvasY(marker.y))
            val selected = feature.id == selectedFeatureId
            drawCircle(mapCategoryColor(feature.category), markerRadiusPx, center)
            drawCircle(
                color = if (selected) tokens.accent.copy(alpha = highlight) else tokens.surfaceBase,
                radius = markerRadiusPx,
                center = center,
                style = Stroke(hairlinePx * if (selected) SelectedStrokeScale else 1f),
            )
        }

        level.features.forEach { feature ->
            val layout = labels[feature.id] ?: return@forEach
            val anchor = labelPoints[feature.id] ?: return@forEach
            val selected = feature.id == selectedFeatureId
            val isMarker = feature.shape is FeatureShape.Marker

            // The cheap stand-in for label-collision detection, and enough at
            // this venue size: an area keeps its label only while the label
            // fits inside it, so zooming in reveals the small rooms' names
            // instead of piling overlapping text at fit zoom.
            val fits = if (isMarker) {
                zoom >= MarkerLabelZoom
            } else {
                val onScreenWidth = (areaWidths[feature.id] ?: 0f) * transform.scale
                layout.size.width <= onScreenWidth - 2f * labelGapPx
            }
            // The selected feature is always labelled: its name is the answer
            // to the tap, and this is also what keeps colour from being the
            // only thing marking the selection.
            if (!selected && !fits) return@forEach

            val centerX = transform.toCanvasX(anchor.x)
            val centerY = transform.toCanvasY(anchor.y)
            val top = if (isMarker) {
                centerY + markerRadiusPx + labelGapPx
            } else {
                centerY - layout.size.height / 2f
            }
            drawText(
                textLayoutResult = layout,
                color = if (selected) tokens.textPrimary else tokens.textMuted,
                topLeft = Offset(centerX - layout.size.width / 2f, top),
            )
        }
    }
}

/**
 * Venue → canvas, with the user's zoom and pan folded in.
 *
 * Zoom is applied about the canvas centre and then corrected by [pan] rather
 * than kept as a free-floating matrix. That leaves the gesture state as two
 * plain numbers, which survive a rotation and make "reset" a matter of setting
 * them back to 1 and zero.
 *
 * Lives here and not in `MapGeometry.kt` because it touches Compose's `Offset`,
 * and keeping that file free of Compose types is what lets its tests run
 * without a composition.
 */
private fun mapTransform(
    base: MapTransform,
    zoom: Float,
    pan: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
): MapTransform = MapTransform(
    scale = base.scale * zoom,
    offsetX = base.offsetX * zoom + canvasWidth / 2f * (1f - zoom) + pan.x,
    offsetY = base.offsetY * zoom + canvasHeight / 2f * (1f - zoom) + pan.y,
)

/**
 * Keeps the plan on screen.
 *
 * The two ends of the allowed range are "content's leading edge at the
 * viewport's leading edge" and "content's trailing edge at the viewport's
 * trailing edge". Which one is the *minimum* flips depending on whether the
 * content is bigger than the viewport, which is why they are sorted rather than
 * assumed — at fit zoom the content is smaller, and assuming the wrong order
 * there pins the plan into a corner.
 */
private fun clampPan(
    pan: Offset,
    base: MapTransform,
    zoom: Float,
    bounds: MapBounds,
    canvasWidth: Float,
    canvasHeight: Float,
): Offset {
    val scale = base.scale * zoom
    val zeroPan = mapTransform(base, zoom, Offset.Zero, canvasWidth, canvasHeight)
    val left = zeroPan.toCanvasX(bounds.minX)
    val top = zeroPan.toCanvasY(bounds.minY)
    val width = bounds.width * scale
    val height = bounds.height * scale

    val xa = -left
    val xb = canvasWidth - width - left
    val ya = -top
    val yb = canvasHeight - height - top

    return Offset(
        pan.x.coerceIn(min(xa, xb), max(xa, xb)),
        pan.y.coerceIn(min(ya, yb), max(ya, yb)),
    )
}

/**
 * A path in venue metres, built once per floor.
 *
 * `EvenOdd` is what makes holes holes: each ring is its own closed sub-path,
 * and even-odd winding cuts the inner ones out regardless of the direction they
 * were traced in — which matters, because QGIS does not enforce a winding order
 * and a hand-edited ring certainly does not.
 */
private fun Polygon.toVenuePath(): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    addRing(ring)
    holes.forEach(::addRing)
}

private fun Path.addRing(points: List<Point>) {
    if (points.size < 3) return
    moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    close()
}

/** Fit-to-screen. Zooming out past the whole floor only shows more canvas. */
private const val MinZoom = 1f

/**
 * 8x — roughly one room filling the screen at this venue scale, which is as far
 * in as a plan with no interior detail is worth going.
 */
private const val MaxZoom = 8f

/** Pins get their names once the plan is no longer at fit zoom. */
private const val MarkerLabelZoom = 1.6f

private const val SelectedStrokeScale = 2f

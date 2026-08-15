package com.conference.asmara.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.conference.asmara.ui.theme.TbcTheme

/** The hairline that separates every step of the surface ladder. */
internal val HairlineWidth = 1.dp

/**
 * The house card: `#1C1C1C` on `#111111`, 8dp radius, one hairline border.
 *
 * Depth comes from the surface-value step plus that border — never a shadow.
 * Two reasons beyond taste: Compose draws shadows *behind* the shape, so any
 * translucency in a container shows the shadow through it as a grey smudge
 * (worse on iOS/Skia); and a shadow on `#111111` has almost nowhere to go
 * tonally, so it reads as dirt rather than lift.
 *
 * `tonalElevation` is pinned to zero. Left at M3's default, an elevated surface
 * is composited as `surfaceTint.copy(alpha = f(elevation))` over the container
 * — which would tint the card blue. `surfaceTint` is already neutralised in the
 * colour scheme; this is the belt to that pair of braces.
 */
@Composable
fun TbcCard(
    modifier: Modifier = Modifier,
    padding: Dp = TbcTheme.spacing.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = TbcTheme.tokens
    val colors = CardDefaults.cardColors(
        containerColor = tokens.surfaceCard,
        contentColor = tokens.textPrimary,
    )
    val border = BorderStroke(HairlineWidth, tokens.borderSubtle)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val shape = MaterialTheme.shapes.medium

    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
        ) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
        ) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

/**
 * A card one step higher on the ladder (`#252525`), for content that overlays
 * other content: menus, sheets, popovers.
 */
@Composable
fun TbcRaisedCard(
    modifier: Modifier = Modifier,
    padding: Dp = TbcTheme.spacing.cardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = TbcTheme.tokens
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = tokens.surfaceRaised,
            contentColor = tokens.textPrimary,
        ),
        border = BorderStroke(HairlineWidth, tokens.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

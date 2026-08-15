package com.conference.asmara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.conference.asmara.ui.theme.TbcTheme
import com.conference.asmara.ui.theme.glow
import com.conference.asmara.ui.theme.gridPattern

/**
 * The root of every screen.
 *
 * Deliberately not Material 3's `Scaffold`: this app has no app bar, no FAB and
 * no bottom bar, so all `Scaffold` would contribute is a container colour
 * (which it takes from `colorScheme.surface`) and a `PaddingValues` most
 * screens would then have to fight. What screens actually need is the canvas,
 * the optional decorative backdrop, and safe-area insets.
 *
 * Insets are applied here, once. Skipping them on iOS is not a cosmetic
 * problem — content slides under the notch and the home indicator.
 *
 * @param decorated draws the faint grid and radial glow behind the content.
 *   Reach for it on landing and hero screens; leave it off for dense lists,
 *   where it competes with the content.
 * @param applyInsets set to false only when the screen itself needs to bleed to
 *   the edges (a full-width image header) and takes over inset handling.
 */
@Composable
fun TbcScaffold(
    modifier: Modifier = Modifier,
    decorated: Boolean = false,
    applyInsets: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tokens = TbcTheme.tokens
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.surfaceBase)
            .then(
                if (decorated) {
                    Modifier
                        .gridPattern(tokens.gridLine)
                        .glow(inner = tokens.glowInner, mid = tokens.glowMid)
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = if (applyInsets) {
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            } else {
                Modifier.fillMaxSize()
            }
        ) {
            content()
        }
    }
}

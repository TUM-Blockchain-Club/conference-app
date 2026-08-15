package com.conference.asmara.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.conference.asmara.ui.components.ScreenFooter
import com.conference.asmara.ui.components.ScreenTitle
import com.conference.asmara.ui.components.TbcButton
import com.conference.asmara.ui.components.TbcButtonRow
import com.conference.asmara.ui.components.TbcButtonStyle
import com.conference.asmara.ui.components.TbcScaffold
import com.conference.asmara.ui.gallery.GalleryScreen
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.theme.TbcTheme

/**
 * Placeholder landing screen, restyled onto the design system.
 *
 * Kept rather than replaced: together with [DetailScreen] it is the only
 * runtime proof that navigation and the Koin graph are wired correctly.
 */
class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val spacing = TbcTheme.spacing

        TbcScaffold(decorated = true) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.screenH, vertical = spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(spacing.xxl),
            ) {
                ScreenTitle(
                    title = "TBC Conference",
                    subtitle = "TUM Blockchain Club",
                )

                TbcButtonRow {
                    TbcButton(
                        text = "Design System",
                        onClick = { navigator.push(GalleryScreen()) },
                        icon = TbcIcons.Star,
                    )
                    TbcButton(
                        text = "Detail",
                        onClick = { navigator.push(DetailScreen()) },
                        style = TbcButtonStyle.Secondary,
                        icon = TbcIcons.ChevronRight,
                    )
                }

                Spacer(Modifier.weight(1f))

                // The footer is the last item in the content rather than a
                // pinned bar: on a phone, a permanently docked footer costs
                // vertical space on every screen for information nobody reads twice.
                ScreenFooter(
                    text = "TUM Blockchain Club",
                    trailing = "Internal build",
                )
            }
        }
    }
}

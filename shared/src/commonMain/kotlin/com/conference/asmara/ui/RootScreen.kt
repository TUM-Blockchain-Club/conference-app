package com.conference.asmara.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.conference.asmara.ui.components.PillTab
import com.conference.asmara.ui.components.PillTabRow
import com.conference.asmara.ui.components.TabRowDivider
import com.conference.asmara.ui.components.TbcScaffold
import com.conference.asmara.ui.detail.EventDetailScreen
import com.conference.asmara.ui.icons.TbcIcons
import com.conference.asmara.ui.map.MapContent
import com.conference.asmara.ui.map.MapFocusRequests
import com.conference.asmara.ui.map.MapScreenModel
import com.conference.asmara.ui.schedule.ScheduleListContent
import com.conference.asmara.ui.schedule.ScheduleScreenModel
import com.conference.asmara.ui.theme.TbcTheme
import org.koin.compose.getKoin

private enum class RootTab { SCHEDULE, MAP }

/**
 * The tab shell: the app's two top-level destinations, and the one scaffold
 * they share.
 *
 * ### One scaffold, not three
 *
 * `TbcScaffold` applies `WindowInsets.safeDrawing`. Nesting a scaffold per tab
 * inside this one applies them twice, which on iOS shows up as a tab bar
 * hovering a home-indicator's height above the bottom edge — a bug that looks
 * like a padding typo and is not. So the shell owns the only scaffold, and each
 * tab contributes a `*Content` composable with no scaffold of its own. The
 * `Screen` wrappers (`ScheduleListScreen`, `MapScreen`) still exist for pushing
 * a tab on its own; they are simply not what the shell uses.
 *
 * ### Detail still covers the tabs
 *
 * `EventDetailScreen` is pushed onto the *same* navigator this screen sits in,
 * so it renders full-screen over the tab bar rather than inside a tab. That is
 * the behaviour the app already had, and it is the right one: a session detail
 * is a place you go and come back from, not a third tab.
 *
 * ### Tabs are content, not navigation history
 *
 * Switching tabs does not push, so back from the Map tab leaves the app rather
 * than returning to Schedule. That matches every phone tab bar; a back stack of
 * tab switches is the thing users complain about.
 */
class RootScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val koin = getKoin()
        val focusRequests = remember(koin) { koin.get<MapFocusRequests>() }
        val spacing = TbcTheme.spacing

        // rememberSaveable: an Android configuration change must not drop the
        // user back onto Schedule.
        var selectedTab by rememberSaveable { mutableIntStateOf(RootTab.SCHEDULE.ordinal) }

        // The shell is the only consumer of the focus request, and it consumes
        // it *after* handing it to the map — see MapFocusRequests. Reading it
        // here is also what makes the tab switch happen at all.
        val pendingFocus by focusRequests.pending.collectAsState()
        LaunchedEffect(pendingFocus) {
            if (pendingFocus != null) selectedTab = RootTab.MAP.ordinal
        }

        val tabs = remember {
            listOf(
                PillTab("Schedule", TbcIcons.Calendar),
                PillTab("Map", TbcIcons.Map),
            )
        }

        TbcScaffold {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    // A `when` rather than keeping both composed: each tab's
                    // ScreenModel lives in this Screen's ScreenModelStore, so
                    // it survives the switch anyway, and rendering the map's
                    // canvas behind the schedule would cost a full redraw per
                    // frame for something nobody can see.
                    when (RootTab.entries[selectedTab]) {
                        RootTab.SCHEDULE -> ScheduleListContent(
                            // Created on first visit and kept afterwards:
                            // ScreenModelStore keys on this Screen plus the
                            // tag, and this Screen outlives every tab switch.
                            // So the Map tab costs nothing until it is opened,
                            // and the Schedule tab's scroll and filters survive
                            // going to the map and back.
                            screenModel = rememberScreenModel(tag = "schedule") {
                                koin.get<ScheduleScreenModel>()
                            },
                            onEventClick = { navigator.push(EventDetailScreen(it)) },
                        )
                        RootTab.MAP -> MapContent(
                            screenModel = rememberScreenModel(tag = "map") {
                                koin.get<MapScreenModel>()
                            },
                            onEventClick = { navigator.push(EventDetailScreen(it)) },
                            focusLocationId = pendingFocus,
                            onFocusHandled = focusRequests::consume,
                        )
                    }
                }
                TabRowDivider()
                PillTabRow(
                    tabs = tabs,
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.padding(vertical = spacing.xs),
                )
            }
        }
    }
}

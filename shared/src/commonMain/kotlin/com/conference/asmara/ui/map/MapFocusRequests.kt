package com.conference.asmara.ui.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "Open the Map tab on this room."
 *
 * The cross-link from a session to the map crosses two boundaries at once: a
 * *screen* boundary, because `EventDetailScreen` is pushed over the tab shell
 * and has to pop back to it, and a *tab* boundary, because the shell owns which
 * tab is showing and the map's state holder owns what is highlighted. Neither
 * side can reach the other through Voyager's navigator, which only knows about
 * the back stack.
 *
 * So the request is a singleton the two ends share. It is a plain
 * [StateFlow<String?>][StateFlow] and not an event channel because the value is
 * genuinely state: a request made while the map is still loading must stay
 * pending until there is a map to apply it to, and a fire-and-forget event
 * would be dropped exactly then.
 *
 * **`RootScreen` is the only consumer.** It switches the tab and hands the id
 * down to `MapContent`, which passes it to the screen model and then calls
 * [consume]. Letting the model consume directly would race the shell: both
 * collect the same conflated flow, and if the model won, the tab would never
 * switch.
 */
class MapFocusRequests {
    private val _pending = MutableStateFlow<String?>(null)

    /** The `locations.id` to highlight, or null when there is nothing pending. */
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun request(locationId: String) {
        _pending.value = locationId
    }

    fun consume() {
        _pending.value = null
    }
}

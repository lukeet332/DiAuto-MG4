package com.allgo.app.androidauto

/** Turns DiAuto's session state into the callbacks AllGo's Android Auto app sends the hub. */
class BridgeSession {

    sealed class HubEvent {
        data class DeviceConnected(val device: HubDevice) : HubEvent()
        /** [launchedInProjection]: stock's launch status, 1 only when Android Auto is on screen. */
        data class SessionStarted(val device: HubDevice, val launchedInProjection: Boolean) : HubEvent()
        object ScreenStarted : HubEvent()
        object ScreenStopped : HubEvent()
        data class SessionClosed(val device: HubDevice) : HubEvent()
        data class DeviceDisconnected(val device: HubDevice) : HubEvent()
        /** AllGo call state: 0 idle, 1 active, 2 incoming, 3 on hold. */
        data class PhoneState(val state: Int) : HubEvent()
        /** AllGo playback state: 0 stopped, 1 playing, 2 paused. */
        data class Playback(val state: Int) : HubEvent()
        data class Navigation(val message: String) : HubEvent()
    }

    private var device: HubDevice? = null
    /** The hub has asked to start the session (stock order: connected, startSession, started). */
    private var started = false
    private var screenVisible = false
    private var callState = 0
    private var playbackState = 0
    private var navigationStatus = NAV_INACTIVE

    fun activeDevice(): HubDevice? = device

    fun phoneConnected(phone: Phone): List<HubEvent> {
        val next = deviceFor(phone)
        val current = device
        if (current != null && current.serialId == next.serialId) return emptyList()
        val events = phoneGone().toMutableList()
        device = next
        events += connected(next)
        return events
    }

    /** The hub's startSession, or DiAuto's fallback when the hub never asks. */
    fun startRequested(): List<HubEvent> {
        val current = device ?: return emptyList()
        if (started) return emptyList()
        started = true
        return sessionState(current)
    }

    fun isSessionStarted(): Boolean = device != null && started

    fun screen(visible: Boolean): List<HubEvent> {
        if (device == null || visible == screenVisible) return emptyList()
        screenVisible = visible
        if (!started) return emptyList()
        return listOf(if (visible) HubEvent.ScreenStarted else HubEvent.ScreenStopped)
    }

    fun callState(state: Int): List<HubEvent> {
        if (device == null || state == callState) return emptyList()
        callState = state
        if (!started) return emptyList()
        return listOf(HubEvent.PhoneState(state))
    }

    fun playback(state: Int): List<HubEvent> {
        if (device == null || state == playbackState) return emptyList()
        playbackState = state
        if (!started) return emptyList()
        return listOf(HubEvent.Playback(state))
    }

    /** Android Auto NavigationStatus: 0 unavailable, 1 active, 2 inactive, 3 rerouting. */
    fun navigation(status: Int): List<HubEvent> {
        if (device == null || status == navigationStatus) return emptyList()
        navigationStatus = status
        if (!started) return emptyList()
        return listOf(HubEvent.Navigation(navigationMessage(status)))
    }

    fun phoneGone(): List<HubEvent> {
        val current = device ?: return emptyList()
        val closed = current.copy(sessionStatus = HubDevice.SESSION_NOT_STARTED)
        val events = mutableListOf<HubEvent>()
        if (started && navigationStatus == NAV_ACTIVE) events += HubEvent.Navigation(navigationMessage(NAV_INACTIVE))
        navigationStatus = NAV_INACTIVE
        if (started && screenVisible) events += HubEvent.ScreenStopped
        events += HubEvent.SessionClosed(closed)
        events += HubEvent.DeviceDisconnected(closed)
        device = null
        started = false
        screenVisible = false
        // The hub resets its own call and playback state on close.
        callState = 0
        playbackState = 0
        return events
    }

    /** Everything a freshly registered hub needs to match the live session. */
    fun replay(): List<HubEvent> {
        val current = device ?: return emptyList()
        return connected(current) + if (started) sessionState(current) else emptyList()
    }

    private fun connected(device: HubDevice) = listOf(HubEvent.DeviceConnected(device.copy(sessionStatus = HubDevice.SESSION_NOT_STARTED)))

    private fun sessionState(device: HubDevice): List<HubEvent> {
        val events = mutableListOf<HubEvent>(HubEvent.SessionStarted(device, launchedInProjection = screenVisible))
        if (screenVisible) events += HubEvent.ScreenStarted
        if (callState != 0) events += HubEvent.PhoneState(callState)
        if (playbackState != 0) events += HubEvent.Playback(playbackState)
        if (navigationStatus == NAV_ACTIVE) events += HubEvent.Navigation(navigationMessage(NAV_ACTIVE))
        return events
    }

    private fun deviceFor(phone: Phone): HubDevice {
        val mac = phone.btMac?.takeIf { it.isNotBlank() } ?: HubDevice.UNKNOWN_MAC
        return HubDevice(
            sessionStatus = HubDevice.SESSION_STARTED,
            name = phone.name?.takeIf { it.isNotBlank() } ?: HubDevice.DEFAULT_NAME,
            serialId = "DIAUTO-" + mac.replace(":", ""),
            btMac = mac,
        )
    }

    private fun navigationMessage(status: Int) = when (status) {
        NAV_ACTIVE -> "Navigation Status:Active"
        NAV_INACTIVE -> "Navigation Status:In-active"
        else -> "Navigation Status:Un-available"
    }

    private companion object {
        const val NAV_ACTIVE = 1
        const val NAV_INACTIVE = 2
    }
}

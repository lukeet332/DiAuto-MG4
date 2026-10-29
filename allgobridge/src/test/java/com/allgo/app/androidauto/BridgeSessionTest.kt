package com.allgo.app.androidauto

import com.allgo.app.androidauto.BridgeSession.HubEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BridgeSessionTest {

    private val pixel = Phone(name = "Pixel", btMac = "AA:BB:CC:DD:EE:FF")

    @Test
    fun `a DiAuto session reaches the hub as a USB Android Auto device that connects then starts`() {
        val session = BridgeSession()
        val events = session.phoneConnected(pixel)

        val device = session.activeDevice()!!
        // The hub only matches Android Auto devices by USB serial; a wireless one is silently dropped.
        assertEquals(HubDevice.TRANSPORT_USB, device.transport)
        assertEquals(HubDevice.TYPE_ANDROID_AUTO, device.type)
        assertEquals("AA:BB:CC:DD:EE:FF", device.btMac)
        assertEquals(listOf(HubEvent.DeviceConnected(device.copy(sessionStatus = HubDevice.SESSION_NOT_STARTED))), events)
    }

    @Test
    fun `the session starts when the hub asks, as with stock AllGo, and only once`() {
        // Car map G5: reporting connected and started back to back let the hub call resumeSession,
        // which pulled DiAuto over whatever the driver had open.
        val session = BridgeSession()
        assertEquals(emptyList<HubEvent>(), session.startRequested())
        session.phoneConnected(pixel)
        val device = session.activeDevice()!!

        assertEquals(listOf(HubEvent.SessionStarted(device, launchedInProjection = false)), session.startRequested())
        assertEquals(emptyList<HubEvent>(), session.startRequested())
    }

    @Test
    fun `a session reports projection launch only when Android Auto is on screen`() {
        // Car map G6: launch status 1 makes SystemUI hide the dock, even over a native app.
        val session = BridgeSession()
        session.phoneConnected(pixel)
        assertEquals(emptyList<HubEvent>(), session.screen(visible = true))
        val device = session.activeDevice()!!

        assertEquals(listOf(HubEvent.SessionStarted(device, launchedInProjection = true), HubEvent.ScreenStarted), session.startRequested())
    }

    @Test
    fun `the same phone keeps the same serial so the hub recognises it next time`() {
        val first = BridgeSession().also { it.phoneConnected(pixel) }.activeDevice()!!.serialId
        val second = BridgeSession().also { it.phoneConnected(pixel) }.activeDevice()!!.serialId
        assertEquals(first, second)
    }

    @Test
    fun `an unknown phone identity never reaches the hub as null`() {
        // SystemUI reads the device's Bluetooth MAC on its main thread; a null crashes it.
        val device = BridgeSession().also { it.phoneConnected(Phone(name = null, btMac = null)) }.activeDevice()!!
        assertEquals(HubDevice.UNKNOWN_MAC, device.btMac)
        assertEquals(HubDevice.DEFAULT_NAME, device.name)
    }

    @Test
    fun `screen changes are reported only while a session is up and only when they change`() {
        val session = BridgeSession()
        assertEquals(emptyList<HubEvent>(), session.screen(visible = true))

        session.phoneConnected(pixel)
        session.startRequested()
        assertEquals(listOf(HubEvent.ScreenStarted), session.screen(visible = true))
        assertEquals(emptyList<HubEvent>(), session.screen(visible = true))
        assertEquals(listOf(HubEvent.ScreenStopped), session.screen(visible = false))
    }

    @Test
    fun `ending a session stops the screen, closes the session and disconnects the device, never fails it`() {
        // The hub's own session-failed handler dereferences a field that is null on a device that
        // came over binder, so the bridge only ever closes.
        val session = BridgeSession()
        session.phoneConnected(pixel)
        session.startRequested()
        session.screen(visible = true)
        val device = session.activeDevice()!!

        val events = session.phoneGone()

        val closed = device.copy(sessionStatus = HubDevice.SESSION_NOT_STARTED)
        assertEquals(listOf(HubEvent.ScreenStopped, HubEvent.SessionClosed(closed), HubEvent.DeviceDisconnected(closed)), events)
        assertNull(session.activeDevice())
        assertEquals(emptyList<HubEvent>(), session.phoneGone())
    }

    @Test
    fun `a different phone replaces the current one cleanly`() {
        val session = BridgeSession()
        session.phoneConnected(pixel)
        val old = session.activeDevice()!!.copy(sessionStatus = HubDevice.SESSION_NOT_STARTED)

        val events = session.phoneConnected(Phone(name = "OnePlus", btMac = "11:22:33:44:55:66"))

        assertEquals(HubEvent.SessionClosed(old), events[0])
        assertEquals(HubEvent.DeviceDisconnected(old), events[1])
        assertEquals("OnePlus", session.activeDevice()!!.name)
    }

    @Test
    fun `the same phone reported again does not restart the session`() {
        val session = BridgeSession()
        session.phoneConnected(pixel)
        assertEquals(emptyList<HubEvent>(), session.phoneConnected(pixel))
    }

    @Test
    fun `a hub that registers mid-session is brought up to date`() {
        // The hub keeps no state across a restart, and its clients only learn from its callbacks.
        val session = BridgeSession()
        session.phoneConnected(pixel)
        val device = session.activeDevice()!!
        val connected = HubEvent.DeviceConnected(device.copy(sessionStatus = HubDevice.SESSION_NOT_STARTED))
        assertEquals(listOf(connected), session.replay())

        session.startRequested()
        session.screen(visible = true)
        assertEquals(listOf(connected, HubEvent.SessionStarted(device, launchedInProjection = true), HubEvent.ScreenStarted), session.replay())
        assertEquals(emptyList<HubEvent>(), BridgeSession().replay())
    }

    @Test
    fun `call and playback changes reach the hub only during a session and only when they change`() {
        val session = BridgeSession()
        assertEquals(emptyList<HubEvent>(), session.callState(1))

        session.phoneConnected(pixel)
        session.startRequested()
        assertEquals(listOf(HubEvent.PhoneState(2)), session.callState(2))
        assertEquals(emptyList<HubEvent>(), session.callState(2))
        assertEquals(listOf(HubEvent.PhoneState(1)), session.callState(1))
        assertEquals(listOf(HubEvent.Playback(1)), session.playback(1))
        assertEquals(emptyList<HubEvent>(), session.playback(1))
    }

    @Test
    fun `a replay restores call and playback state, and a new session starts from idle`() {
        val session = BridgeSession()
        session.phoneConnected(pixel)
        session.startRequested()
        session.callState(1)
        session.playback(2)
        val replay = session.replay()
        assertEquals(listOf(HubEvent.PhoneState(1), HubEvent.Playback(2)), replay.takeLast(2))

        session.phoneGone()
        session.phoneConnected(pixel)
        session.startRequested()
        assertEquals(listOf(HubEvent.Playback(2)), session.playback(2))
    }

    @Test
    fun `Android Auto navigation reaches the car with the messages stock AllGo uses`() {
        val session = BridgeSession()
        assertEquals(emptyList<HubEvent>(), session.navigation(1))

        session.phoneConnected(pixel)
        session.startRequested()
        assertEquals(listOf(HubEvent.Navigation("Navigation Status:Active")), session.navigation(1))
        assertEquals(emptyList<HubEvent>(), session.navigation(1))
        assertEquals(listOf(HubEvent.Navigation("Navigation Status:Un-available")), session.navigation(3))
        assertEquals(listOf(HubEvent.Navigation("Navigation Status:In-active")), session.navigation(2))
    }

    @Test
    fun `ending a session mid-route tells the car navigation stopped first`() {
        val session = BridgeSession()
        session.phoneConnected(pixel)
        session.startRequested()
        session.navigation(1)
        assertEquals(listOf(HubEvent.Navigation("Navigation Status:Active")), session.replay().filterIsInstance<HubEvent.Navigation>())

        val events = session.phoneGone()
        assertEquals(HubEvent.Navigation("Navigation Status:In-active"), events.first())
    }
}

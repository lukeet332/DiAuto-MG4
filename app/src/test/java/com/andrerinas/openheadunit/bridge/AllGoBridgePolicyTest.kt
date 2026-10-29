package com.andrerinas.openheadunit.bridge

import com.andrerinas.openheadunit.connection.CommManager.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class AllGoBridgePolicyTest {

    @Test
    fun `the car hears about a session once Android Auto is actually running`() {
        assertEquals(AllGoBridgePolicy.PHONE_CONNECTED, AllGoBridgePolicy.messageFor(ConnectionState.TransportStarted))
        assertEquals(null, AllGoBridgePolicy.messageFor(ConnectionState.Connecting))
        assertEquals(null, AllGoBridgePolicy.messageFor(ConnectionState.HandshakeComplete))
    }

    @Test
    fun `any disconnect or error ends the session for the car`() {
        assertEquals(AllGoBridgePolicy.PHONE_GONE, AllGoBridgePolicy.messageFor(ConnectionState.Disconnected()))
        assertEquals(AllGoBridgePolicy.PHONE_GONE, AllGoBridgePolicy.messageFor(ConnectionState.Error("eof")))
    }

    @Test
    fun `the car call state follows the phone's call through the car`() {
        // AllGo values: 0 idle, 1 active, 2 incoming.
        assertEquals(2, AllGoBridgePolicy.callState(ringing = true, inCall = true))
        assertEquals(1, AllGoBridgePolicy.callState(ringing = false, inCall = true))
        assertEquals(0, AllGoBridgePolicy.callState(ringing = false, inCall = false))
    }

    @Test
    fun `Android Auto playback maps to the car's playing and paused states`() {
        assertEquals(1, AllGoBridgePolicy.playbackState(isPlaying = true))
        assertEquals(2, AllGoBridgePolicy.playbackState(isPlaying = false))
    }

    @Test
    fun `a car request to show Android Auto while it is already on screen is ignored`() {
        // MG4, 2026-10-19: the hub's auto-resume at every session start re-launched the visible
        // projection; the pause/resume it caused dropped and regained video focus and the picture
        // flashed green while the phone re-sent a keyframe.
        assertEquals(false, AllGoBridgePolicy.shouldShowProjection(projectionVisible = true))
        assertEquals(true, AllGoBridgePolicy.shouldShowProjection(projectionVisible = false))
    }

    @Test
    fun `media keys from the car reach Android Auto in the background, screen keys bring it up first`() {
        // Car map G9: next/prev on the launcher media card were held 4 s for a projection that never showed.
        assertEquals(false, AllGoBridgePolicy.keyNeedsProjection(85))
        assertEquals(false, AllGoBridgePolicy.keyNeedsProjection(87))
        assertEquals(false, AllGoBridgePolicy.keyNeedsProjection(88))
        assertEquals(false, AllGoBridgePolicy.keyNeedsProjection(126))
        assertEquals(false, AllGoBridgePolicy.keyNeedsProjection(127))
        assertEquals(true, AllGoBridgePolicy.keyNeedsProjection(3))
        assertEquals(true, AllGoBridgePolicy.keyNeedsProjection(65537))
        assertEquals(true, AllGoBridgePolicy.keyNeedsProjection(65540))
    }
}

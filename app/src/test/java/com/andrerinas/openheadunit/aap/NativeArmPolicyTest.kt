package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeArmPolicyTest {

    @Test
    fun `arms when Bluetooth is on and reachable`() {
        assertTrue(NativeArmPolicy.shouldArm(btEnabled = true, externalBtBlocked = false))
    }

    @Test
    fun `does not arm while Bluetooth is off`() {
        // Car standby wakes run with the adapter off: nobody can receive the credentials.
        assertFalse(NativeArmPolicy.shouldArm(btEnabled = false, externalBtBlocked = false))
    }

    @Test
    fun `does not arm behind an external Bluetooth module`() {
        assertFalse(NativeArmPolicy.shouldArm(btEnabled = true, externalBtBlocked = true))
        assertFalse(NativeArmPolicy.shouldArm(btEnabled = false, externalBtBlocked = true))
    }

    @Test
    fun `re-arms when Bluetooth comes on in Native AA mode with nothing running`() {
        assertTrue(rearm())
    }

    @Test
    fun `does not re-arm outside Native AA mode`() {
        assertFalse(rearm(mode = 1))
        assertFalse(rearm(mode = 2))
    }

    @Test
    fun `does not re-arm listeners that are already up`() {
        assertFalse(rearm(handshakeActive = true))
    }

    @Test
    fun `does not tear down a live or connecting session`() {
        assertFalse(rearm(sessionBusy = true))
    }

    private fun rearm(mode: Int = 3, handshakeActive: Boolean = false, sessionBusy: Boolean = false) =
        NativeArmPolicy.shouldRearmOnBtOn(mode, handshakeActive, sessionBusy)
}

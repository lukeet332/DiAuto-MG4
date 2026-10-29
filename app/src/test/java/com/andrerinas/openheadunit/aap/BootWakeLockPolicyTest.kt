package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootWakeLockPolicyTest {

    @Test
    fun `released once wireless is set up and nothing is connecting`() {
        // A car standby wake: wireless deliberately skipped (Bluetooth off), no USB phone.
        assertTrue(BootWakeLockPolicy.canRelease(wirelessInitialised = true, usbSwitching = false, connecting = false))
    }

    @Test
    fun `held while wireless start-up has not run yet`() {
        assertFalse(BootWakeLockPolicy.canRelease(wirelessInitialised = false, usbSwitching = false, connecting = false))
    }

    @Test
    fun `held through a USB switch or a connection in progress`() {
        assertFalse(BootWakeLockPolicy.canRelease(wirelessInitialised = true, usbSwitching = true, connecting = false))
        assertFalse(BootWakeLockPolicy.canRelease(wirelessInitialised = true, usbSwitching = false, connecting = true))
    }
}

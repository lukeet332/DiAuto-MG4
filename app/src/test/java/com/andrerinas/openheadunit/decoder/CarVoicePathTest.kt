package com.andrerinas.openheadunit.decoder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The strings MG's own Android Auto (AllGo AapAudioRecorder) sets around every recording. */
class CarVoicePathTest {
    @Test
    fun `wireless sessions use the Wi-Fi voice path`() {
        assertEquals("SSE_AA_VR_Wifi=true", CarVoicePath.parameter(wireless = true, enabled = true))
        assertEquals("SSE_AA_VR_Wifi=false", CarVoicePath.parameter(wireless = true, enabled = false))
    }

    @Test
    fun `usb sessions use the USB voice path`() {
        assertEquals("SSE_AA_VR=true", CarVoicePath.parameter(wireless = false, enabled = true))
        assertEquals("SSE_AA_VR=false", CarVoicePath.parameter(wireless = false, enabled = false))
    }

    interface OwnershipCallback {
        fun onAppFocusOwnershipLost(appType: Int)
    }

    @Test
    fun `the focus callback survives being stored in a map`() {
        // CarAppFocusManager keys its callbacks by hashCode; a null there was the MG4's NPE.
        val callback = CarVoicePath.newFocusCallback(OwnershipCallback::class.java) as OwnershipCallback
        val map = hashMapOf<Any, Int>(callback to 2)

        assertEquals(2, map[callback])
        assertTrue(callback == callback)
        assertFalse(callback.equals(Any()))
    }
}

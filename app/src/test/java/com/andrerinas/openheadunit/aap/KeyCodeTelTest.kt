package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyCodeTelTest {
    @Test
    fun `the car's phone button key reaches Android Auto and is advertised to the phone`() {
        // Stock AllGo sends AAP_KEYCODE_TEL (65540) for the dock's phone button.
        assertTrue(KeyCode.supported.contains(65540))
        assertEquals(65540, KeyCode.convert(65540))
    }
}

class KeyCodeAaExtendedTest {
    @Test
    fun `Android Auto's own screen keys are not mistaken for unmapped proprietary keys`() {
        // The car's dock buttons send MEDIA 65537, NAVIGATION 65538 and TEL 65540 through the AllGo
        // bridge; CommManager dropped every unmapped code >= 1000 (2026-10-19).
        assertTrue(KeyCode.isAndroidAutoExtendedKey(65537))
        assertTrue(KeyCode.isAndroidAutoExtendedKey(65538))
        assertTrue(KeyCode.isAndroidAutoExtendedKey(65540))
        assertTrue(!KeyCode.isAndroidAutoExtendedKey(1001))
        assertTrue(!KeyCode.isAndroidAutoExtendedKey(209))
    }
}

class KeyCodeUnknownTest {
    @Test
    fun `a car key Android Auto does not know is not sent at all`() {
        // MG4 steering keys 300/301 also reach the projection window as raw KeyEvents; sending them
        // as KEYCODE_UNKNOWN pulled Android Auto out of touch mode and its focus ring wandered
        // across the screen (2026-10-19).
        assertTrue(!KeyCode.supported.contains(300) && !KeyCode.supported.contains(301))
        assertTrue(!KeyCode.shouldSendToAndroidAuto(android.view.KeyEvent.KEYCODE_UNKNOWN))
        assertTrue(KeyCode.shouldSendToAndroidAuto(KeyCode.convert(85)))
        assertTrue(KeyCode.shouldSendToAndroidAuto(KeyCode.convert(65540)))
    }
}

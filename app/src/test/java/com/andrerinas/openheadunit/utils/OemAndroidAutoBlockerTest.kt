package com.andrerinas.openheadunit.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OemAndroidAutoBlockerTest {
    @Test
    fun `targets AllGo Android Auto and SAIC bridge not CarPlay or shared RUI`() {
        assertTrue(OemAndroidAutoBlocker.TARGET_PACKAGES.contains("com.allgo.app.androidauto"))
        assertTrue(OemAndroidAutoBlocker.TARGET_PACKAGES.contains("com.saicmotor.caradapter"))
        assertTrue(!OemAndroidAutoBlocker.TARGET_PACKAGES.contains("com.allgo.rui"))
        assertTrue(!OemAndroidAutoBlocker.TARGET_PACKAGES.contains("com.allgo.carplay.service"))
    }

    @Test
    fun `the DiAuto bridge installed over AllGo Android Auto is left running`() {
        // Force-stopping it would leave the car's hub with no Android Auto app to hear from.
        assertEquals(listOf("com.saicmotor.caradapter"), OemAndroidAutoBlocker.stopTargets { it == "com.allgo.app.androidauto" })
    }

    @Test
    fun `stock AllGo Android Auto is still stopped`() {
        assertEquals(OemAndroidAutoBlocker.TARGET_PACKAGES, OemAndroidAutoBlocker.stopTargets { false })
    }
}

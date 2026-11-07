package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationPausePolicyTest {

    @Test
    fun `pauses a car Wi-Fi that is on but not connected during a hotspot session`() {
        // 7 Nov: unconnected station scans stalled the AP for up to 11 s mid-drive.
        assertTrue(StationPausePolicy.shouldPauseDuringSession(stationEnabled = true, stationFrequencyMhz = -1, hotspotSession = true))
    }

    @Test
    fun `leaves a connected car Wi-Fi alone`() {
        assertFalse(StationPausePolicy.shouldPauseDuringSession(stationEnabled = true, stationFrequencyMhz = 5180, hotspotSession = true))
    }

    @Test
    fun `does nothing when the car Wi-Fi is already off`() {
        assertFalse(StationPausePolicy.shouldPauseDuringSession(stationEnabled = false, stationFrequencyMhz = -1, hotspotSession = true))
    }

    @Test
    fun `does nothing outside a hotspot session`() {
        assertFalse(StationPausePolicy.shouldPauseDuringSession(stationEnabled = true, stationFrequencyMhz = -1, hotspotSession = false))
    }
}

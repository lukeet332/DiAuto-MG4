package com.andrerinas.openheadunit.aap

/**
 * Whether to switch the car's own Wi-Fi off for an Android Auto session on its hotspot.
 *
 * The MT6630 has one radio. A station that is on but not connected keeps scanning, and each scan
 * takes the radio off the hotspot's channel: on 5–7 Nov that silenced the whole link for 1–11 s
 * at a time (frozen picture, cut audio). A connected station is left alone, so the car stays on
 * home Wi-Fi when parked.
 */
object StationPausePolicy {
    fun shouldPauseDuringSession(stationEnabled: Boolean, stationFrequencyMhz: Int, hotspotSession: Boolean): Boolean =
        hotspotSession && stationEnabled && stationFrequencyMhz <= 0
}

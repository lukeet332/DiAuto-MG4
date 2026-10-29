package com.andrerinas.openheadunit.aap

/**
 * When the Native AA route (credentials provider, hotspot, Bluetooth handshake servers) is worth
 * bringing up. It hands the Wi-Fi credentials over Bluetooth, so with the adapter off there is
 * nobody to give them to: on the MG4 that is every standby wake, and arming there only cycled the
 * hotspot for nothing (65 tethering starts in 33 wakes, none ever up).
 */
object NativeArmPolicy {

    fun shouldArm(btEnabled: Boolean, externalBtBlocked: Boolean): Boolean =
        btEnabled && !externalBtBlocked

    /** Bluetooth just came on: re-arm a Native AA route left down, but never under a session. */
    fun shouldRearmOnBtOn(wifiConnectionMode: Int, handshakeActive: Boolean, sessionBusy: Boolean): Boolean =
        wifiConnectionMode == 3 && !handshakeActive && !sessionBusy

    /** Bluetooth went off (drive over, car going to sleep): stop the route, but never under a session. */
    fun shouldStandDownOnBtOff(wifiConnectionMode: Int, sessionBusy: Boolean): Boolean =
        wifiConnectionMode == 3 && !sessionBusy
}

package com.andrerinas.openheadunit.aap

/**
 * When the boot/screen-on wake lock has done its job: the start-up work it protects (wireless
 * set-up, any USB switch, a connection coming up) is over. Until now only onDestroy released it,
 * so every standby wake held the CPU for the full 10-minute timeout (54 acquired, 0 released).
 */
object BootWakeLockPolicy {

    fun canRelease(wirelessInitialised: Boolean, usbSwitching: Boolean, connecting: Boolean): Boolean =
        wirelessInitialised && !usbSwitching && !connecting
}

package com.andrerinas.openheadunit.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.aap.AapService
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.utils.Settings
import android.os.UserManager
import android.os.Build
import android.os.SystemClock

class BootCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in BOOT_ACTIONS) return

        AppLog.i("Boot auto-start: received action=$action")

        if (isLocked(context)) {
            AppLog.w("BootCompleteReceiver: Device is locked. Cannot start AapService yet. Waiting for user unlock.")
            // USER_UNLOCKED never reaches manifest receivers, and on the MG4 BOOT_COMPLETED lands
            // ~12s after unlock, so watch for the unlock ourselves.
            if (action == Intent.ACTION_LOCKED_BOOT_COMPLETED) awaitUnlock(context.applicationContext)
            return
        }
        start(context, action)
    }

    private fun isLocked(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            !(context.getSystemService(Context.USER_SERVICE) as UserManager).isUserUnlocked

    private fun awaitUnlock(context: Context) {
        val pending = goAsync()
        Thread {
            try {
                val deadline = SystemClock.elapsedRealtime() + UNLOCK_WAIT_MS
                while (isLocked(context) && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(UNLOCK_POLL_MS)
                if (!isLocked(context)) start(context, Intent.ACTION_USER_UNLOCKED)
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun start(context: Context, action: String) {
        synchronized(BootCompleteReceiver::class.java) {
            if (!BootStartPolicy.shouldStart(action, startedThisBoot)) {
                AppLog.i("Boot auto-start: already started this boot, ignoring $action")
                return
            }
            if (BootStartPolicy.isOncePerBoot(action)) startedThisBoot = true
        }

        val bootEnabled = Settings.isAutoStartOnBootEnabled(context)
        val screenOnEnabled = Settings.isAutoStartOnScreenOnEnabled(context)
        val usbEnabled = Settings.isAutoStartOnUsbEnabled(context)
        val wifiEnabled = Settings.isAutoStartOnWifiEnabled(context)

        if (bootEnabled) {
            AppLog.i("Boot auto-start: starting AapService with BOOT_START (trigger=$action)")
            val serviceIntent = Intent(context, AapService::class.java).apply {
                putExtra(EXTRA_BOOT_START, true)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } else if (screenOnEnabled) {
            // "Start on screen on" needs the service alive to register its dynamic
            // SCREEN_ON receiver. On Quick Boot devices this is a real reboot, so
            // the service must be started after boot to listen for future SCREEN_ON.
            AppLog.i("Boot auto-start: screen-on auto-start enabled, starting AapService to register SCREEN_ON receiver (trigger=$action)")
            val serviceIntent = Intent(context, AapService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } else if (usbEnabled) {
            // On hibernating head units, USB_DEVICE_ATTACHED may not fire after wake.
            // Start the service in the background so it can register its UsbReceiver
            // and check for already-connected USB devices.
            AppLog.i("Boot auto-start: USB auto-start enabled, starting AapService to check USB (trigger=$action)")
            val serviceIntent = Intent(context, AapService::class.java).apply {
                this.action = AapService.ACTION_CHECK_USB
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } else if (wifiEnabled) {
            // Start the service to listen for WiFi connectivity changes dynamically.
            AppLog.i("Boot auto-start: WiFi auto-start enabled, starting AapService to listen for WiFi (trigger=$action)")
            val serviceIntent = Intent(context, AapService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            AppLog.i("Boot auto-start: disabled, skipping")
        }
    }

    companion object {
        const val EXTRA_BOOT_START = "com.andrerinas.openheadunit.EXTRA_BOOT_START"
        private const val UNLOCK_POLL_MS = 100L
        private const val UNLOCK_WAIT_MS = 30_000L

        /** Per process, and so per boot: the broadcasts of one boot all land in the same process. */
        @Volatile private var startedThisBoot = false

        fun handles(action: String): Boolean = action in BOOT_ACTIONS

        private val BOOT_ACTIONS = setOf(
            // Standard Android boot
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_USER_UNLOCKED,
            // An update force-stops the app; without this nothing brings Android Auto back.
            Intent.ACTION_MY_PACKAGE_REPLACED,
            // Generic / OEM quick boot
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            // MediaTek IPO (Instant Power On)
            "com.mediatek.intent.action.QUICKBOOT_POWERON",
            "com.mediatek.intent.action.BOOT_IPO",
            // FYT / GLSX head units (ACC ignition wake)
            "com.fyt.boot.ACCON",
            "com.glsx.boot.ACCON",
            "android.intent.action.ACTION_MT_COMMAND_SLEEP_OUT",
            // Microntek / MTCD / PX3 head units (ACC wake)
            "com.cayboy.action.ACC_ON",
            "com.carboy.action.ACC_ON"
        )
    }
}

package com.andrerinas.openheadunit.app

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootStartPolicyTest {

    @Test
    fun `the first boot broadcast starts the service`() {
        assertTrue(BootStartPolicy.shouldStart(Intent.ACTION_USER_UNLOCKED, startedThisBoot = false))
        assertTrue(BootStartPolicy.shouldStart(Intent.ACTION_BOOT_COMPLETED, startedThisBoot = false))
    }

    @Test
    fun `a later boot broadcast does not start it again or take another boot-loop strike`() {
        // On the MG4 BOOT_COMPLETED reaches us ~12s after unlock, long after we started.
        assertFalse(BootStartPolicy.shouldStart(Intent.ACTION_BOOT_COMPLETED, startedThisBoot = true))
        assertFalse(BootStartPolicy.shouldStart(Intent.ACTION_LOCKED_BOOT_COMPLETED, startedThisBoot = true))
    }

    @Test
    fun `OEM wake broadcasts still start the service after the boot start`() {
        assertTrue(BootStartPolicy.shouldStart("com.fyt.boot.ACCON", startedThisBoot = true))
    }

    @Test
    fun `an app update restarts the service every time`() {
        // MG4, 2026-10-19: after an update nothing restarted AapService and Android Auto stayed down.
        assertTrue(BootStartPolicy.shouldStart(Intent.ACTION_MY_PACKAGE_REPLACED, startedThisBoot = true))
        assertTrue(BootCompleteReceiver.handles(Intent.ACTION_MY_PACKAGE_REPLACED))
    }
}

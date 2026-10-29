package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoEnablePolicyTest {

    private val interval = AutoEnablePolicy.RETRY_INTERVAL_MS

    @Test
    fun `the first look with no access point tries straight away`() {
        assertTrue(AutoEnablePolicy.shouldTry(lastAttemptAtMs = null, nowMs = 1_000L, inFlight = false))
    }

    @Test
    fun `a recent attempt is not repeated inside the interval`() {
        assertFalse(AutoEnablePolicy.shouldTry(lastAttemptAtMs = 10_000L, nowMs = 10_000L + interval - 1, inFlight = false))
    }

    @Test
    fun `an attempt is tried again once the interval has passed`() {
        // The 29 Oct drive: one lost attempt left the hotspot dark for the remaining 8 minutes.
        assertTrue(AutoEnablePolicy.shouldTry(lastAttemptAtMs = 10_000L, nowMs = 10_000L + interval, inFlight = false))
        assertTrue(AutoEnablePolicy.shouldTry(lastAttemptAtMs = 10_000L, nowMs = 10_000L + 10 * interval, inFlight = false))
    }

    @Test
    fun `nothing is tried while a start is already running`() {
        assertFalse(AutoEnablePolicy.shouldTry(lastAttemptAtMs = null, nowMs = 1_000L, inFlight = true))
        assertFalse(AutoEnablePolicy.shouldTry(lastAttemptAtMs = 0L, nowMs = 10 * interval, inFlight = true))
    }

    @Test
    fun `a sweep someone else ran does not use up our attempt`() {
        // Skipped while in flight, so nothing is stamped; the next look after it ends still tries.
        assertFalse(AutoEnablePolicy.shouldTry(lastAttemptAtMs = null, nowMs = 1_000L, inFlight = true))
        assertTrue(AutoEnablePolicy.shouldTry(lastAttemptAtMs = null, nowMs = 2_000L, inFlight = false))
    }

    @Test
    fun `the interval is about fifteen seconds`() {
        assertTrue(interval in 10_000L..20_000L)
    }
}

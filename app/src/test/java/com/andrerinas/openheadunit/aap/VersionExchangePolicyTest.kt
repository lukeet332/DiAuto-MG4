package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionExchangePolicyTest {

    @Test
    fun `a socket sends the request once and waits the whole budget for the answer`() {
        assertEquals(1, VersionExchangePolicy.attempts(reliableStream = true))
        assertEquals(10_000L, VersionExchangePolicy.responseWaitMs(reliableStream = true, budgetMs = 10_000L))
    }

    @Test
    fun `usb keeps its short resend cadence for a request a stale accessory swallowed`() {
        assertEquals(3, VersionExchangePolicy.attempts(reliableStream = false))
        assertEquals(2_000L, VersionExchangePolicy.responseWaitMs(reliableStream = false, budgetMs = 10_000L))
    }

    @Test
    fun `a phone answering after 2s over wifi is still inside the wait`() {
        // 20 Oct 21:01: the phone answered 2.2 s after the request while still settling on the hotspot.
        val answeredAfterMs = 2_200L
        assert(answeredAfterMs < VersionExchangePolicy.responseWaitMs(reliableStream = true, budgetMs = 10_000L))
    }
}

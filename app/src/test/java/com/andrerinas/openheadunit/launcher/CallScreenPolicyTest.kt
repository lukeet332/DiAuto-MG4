package com.andrerinas.openheadunit.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MG4, 2026-10-19: SAIC's btcall service only hides its call screen when AllGo's RemoteUIService
 * reports an Android Auto session, which DiAuto's sessions never do; its BtCallActivity opened
 * over Android Auto's own call card on every call.
 */
class CallScreenPolicyTest {
    private val btCall = "com.saicmotor.hmi.btcall.BtCallActivity"

    @Test
    fun `the car's call screen covering Android Auto during a call sends us back to Android Auto`() {
        assertTrue(CallScreenPolicy.shouldReturnToProjection(btCall, sessionActive = true, inCall = true, msSinceProjectionPaused = 300))
    }

    @Test
    fun `opening the car's phone app yourself is left alone`() {
        assertFalse(CallScreenPolicy.shouldReturnToProjection(btCall, sessionActive = true, inCall = true, msSinceProjectionPaused = 30_000))
        assertFalse(CallScreenPolicy.shouldReturnToProjection(btCall, sessionActive = true, inCall = false, msSinceProjectionPaused = 300))
        assertFalse(CallScreenPolicy.shouldReturnToProjection(btCall, sessionActive = false, inCall = true, msSinceProjectionPaused = 300))
    }

    @Test
    fun `other car screens are never touched`() {
        assertFalse(CallScreenPolicy.shouldReturnToProjection("com.saicmotor.hmi.btcall.ECallActivity", true, true, 300))
        assertFalse(CallScreenPolicy.shouldReturnToProjection("com.saicmotor.hmi.aroundview.MainActivity", true, true, 300))
    }
}

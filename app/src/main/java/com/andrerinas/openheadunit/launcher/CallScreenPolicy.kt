package com.andrerinas.openheadunit.launcher

/**
 * When the car's own Bluetooth call screen should give way to Android Auto's. SAIC's btcall only
 * hides it for sessions AllGo's RemoteUIService reports, never DiAuto's, so it opened over
 * Android Auto's call card on every call. Only BtCallActivity, only during a call and a live
 * session, and only when it has just covered projection: the eCall screen is never touched, and
 * a phone app the driver opened deliberately is left alone.
 */
object CallScreenPolicy {
    const val BT_CALL_ACTIVITY = "com.saicmotor.hmi.btcall.BtCallActivity"
    private const val JUST_COVERED_MS = 2_000L

    fun shouldReturnToProjection(
        activityClass: String,
        sessionActive: Boolean,
        inCall: Boolean,
        msSinceProjectionPaused: Long,
    ): Boolean = activityClass == BT_CALL_ACTIVITY && sessionActive && inCall &&
        msSinceProjectionPaused in 0..JUST_COVERED_MS
}

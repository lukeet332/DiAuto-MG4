package com.andrerinas.openheadunit.aap

/**
 * When to (re)try switching this head unit's hotspot on while no access point is up.
 *
 * A one-shot latch lost its only attempt to a sweep already running, and the hotspot stayed dark
 * for a whole drive; a start that worked within 1.5 s every other time. So retry on an interval,
 * and never while a start is in flight, so a sweep someone else is running costs no attempt.
 */
object AutoEnablePolicy {

    const val RETRY_INTERVAL_MS = 15_000L

    /** @param lastAttemptAtMs elapsedRealtime of our last attempt this run, or null if none yet. */
    fun shouldTry(lastAttemptAtMs: Long?, nowMs: Long, inFlight: Boolean): Boolean = when {
        inFlight -> false
        lastAttemptAtMs == null -> true
        else -> nowMs - lastAttemptAtMs >= RETRY_INTERVAL_MS
    }
}

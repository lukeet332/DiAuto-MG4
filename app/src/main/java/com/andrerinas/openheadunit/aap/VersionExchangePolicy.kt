package com.andrerinas.openheadunit.aap

/**
 * How the AAP version request is sent and awaited. A socket cannot lose the request, so resending
 * it only makes the phone answer twice, and the second answer lands where TLS is expected
 * ("Unable to parse TLS packet header", seen when the phone took 2.2 s while joining the hotspot).
 */
object VersionExchangePolicy {
    private const val USB_ATTEMPTS = 3
    private const val USB_RESPONSE_WAIT_MS = 2_000L

    fun attempts(reliableStream: Boolean): Int = if (reliableStream) 1 else USB_ATTEMPTS

    fun responseWaitMs(reliableStream: Boolean, budgetMs: Long): Long =
        if (reliableStream) budgetMs else USB_RESPONSE_WAIT_MS
}

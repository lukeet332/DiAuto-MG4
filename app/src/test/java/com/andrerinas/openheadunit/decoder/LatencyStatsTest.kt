package com.andrerinas.openheadunit.decoder

import org.junit.Assert.assertEquals
import org.junit.Test

class LatencyStatsTest {
    @Test
    fun `reports median, 95th percentile and max, then starts a fresh window`() {
        val stats = LatencyStats(capacity = 100)
        for (ms in 1..100) stats.record(ms)
        assertEquals("p50=50ms p95=95ms max=100ms n=100", stats.summaryAndReset())
        assertEquals("p50=- p95=- max=- n=0", stats.summaryAndReset())
    }

    @Test
    fun `keeps the most recent samples when the window overflows`() {
        val stats = LatencyStats(capacity = 4)
        for (ms in listOf(500, 500, 10, 20, 30, 40)) stats.record(ms)
        assertEquals("p50=20ms p95=40ms max=40ms n=4", stats.summaryAndReset())
    }
}

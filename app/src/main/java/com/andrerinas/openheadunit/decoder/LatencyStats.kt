package com.andrerinas.openheadunit.decoder

import kotlin.math.ceil

/** Frame arrival-to-display latency over one log window (nearest-rank percentiles). */
internal class LatencyStats(private val capacity: Int = 512) {
    private val samples = IntArray(capacity)
    private var count = 0
    private var next = 0

    @Synchronized
    fun record(ms: Int) {
        samples[next] = ms
        next = (next + 1) % capacity
        if (count < capacity) count++
    }

    @Synchronized
    fun summaryAndReset(): String {
        if (count == 0) return "p50=- p95=- max=- n=0"
        val sorted = samples.copyOf(count).also { it.sort() }
        fun pct(p: Double) = sorted[(ceil(p * count).toInt() - 1).coerceIn(0, count - 1)]
        val out = "p50=${pct(0.50)}ms p95=${pct(0.95)}ms max=${sorted[count - 1]}ms n=$count"
        count = 0
        next = 0
        return out
    }
}

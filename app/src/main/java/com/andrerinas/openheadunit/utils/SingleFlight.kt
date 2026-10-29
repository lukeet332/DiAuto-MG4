package com.andrerinas.openheadunit.utils

import java.util.concurrent.CountDownLatch

/** Runs one call at a time; a caller arriving mid-flight waits for that call's result instead. */
class SingleFlight<T> {

    private class Call<T> {
        val landed = CountDownLatch(1)
        @Volatile var result: Result<T>? = null
    }

    private var current: Call<T>? = null

    val isRunning: Boolean
        get() = synchronized(this) { current != null }

    /** @param onJoin called when this caller is joining a call already running. */
    fun run(onJoin: () -> Unit = {}, block: () -> T): T {
        var joined = true
        val call = synchronized(this) {
            current ?: Call<T>().also { current = it; joined = false }
        }
        if (joined) {
            onJoin()
            call.landed.await()
            return checkNotNull(call.result).getOrThrow()
        }
        try {
            return runCatching(block).also { call.result = it }.getOrThrow()
        } finally {
            synchronized(this) { current = null }
            call.landed.countDown()
        }
    }
}

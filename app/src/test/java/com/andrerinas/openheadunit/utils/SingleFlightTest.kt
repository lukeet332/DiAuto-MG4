package com.andrerinas.openheadunit.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SingleFlightTest {

    @Test
    fun `a lone call runs its block and returns its result`() {
        val flight = SingleFlight<Boolean>()
        assertTrue(flight.run { true })
        assertFalse(flight.isRunning)
    }

    @Test
    fun `a caller arriving mid-flight gets the running call's result instead of false`() {
        val flight = SingleFlight<Boolean>()
        val runs = AtomicInteger()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        var first: Boolean? = null
        val owner = Thread {
            first = flight.run {
                runs.incrementAndGet()
                started.countDown()
                release.await(5, TimeUnit.SECONDS)
                true
            }
        }
        owner.start()
        assertTrue(started.await(5, TimeUnit.SECONDS))
        assertTrue(flight.isRunning)

        var joined: Boolean? = null
        val joiner = Thread { joined = flight.run { runs.incrementAndGet(); false } }
        joiner.start()
        Thread.sleep(100)
        release.countDown()
        owner.join(5_000)
        joiner.join(5_000)

        assertEquals(true, first)
        assertEquals(true, joined)
        assertEquals("the joiner must not start a second sweep", 1, runs.get())
        assertFalse(flight.isRunning)
    }

    @Test
    fun `a call after the flight has landed runs again`() {
        val flight = SingleFlight<Int>()
        assertEquals(1, flight.run { 1 })
        assertEquals(2, flight.run { 2 })
    }

    @Test
    fun `a block that throws still lands the flight`() {
        val flight = SingleFlight<Int>()
        try {
            flight.run { error("boom") }
        } catch (e: IllegalStateException) {
            // expected
        }
        assertFalse(flight.isRunning)
        assertEquals(3, flight.run { 3 })
    }
}

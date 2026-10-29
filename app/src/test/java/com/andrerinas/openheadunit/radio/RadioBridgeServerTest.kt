package com.andrerinas.openheadunit.radio

import com.andrerinas.openheadunit.utils.AppLog
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Socket
import java.net.SocketTimeoutException

class RadioBridgeServerTest {
    @Test
    fun `a phone that stops mid-request only drops its own connection, not DiAuto`() {
        // MG4, 2026-10-20 10:15: a read timeout on one MG4 Radio request was uncaught and killed DiAuto.
        AppLog.LOGGER = object : AppLog.Logger { override fun println(priority: Int, tag: String, msg: String) {} }
        val client = Socket()
        RadioBridgeServer.serve(client) { throw SocketTimeoutException("Read timed out") }
        assertTrue(client.isClosed)
    }
}

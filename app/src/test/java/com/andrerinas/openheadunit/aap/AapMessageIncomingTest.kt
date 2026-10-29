package com.andrerinas.openheadunit.aap

import com.andrerinas.openheadunit.aap.VideoFragmentAssembler.Action
import com.andrerinas.openheadunit.aap.protocol.Channel
import com.andrerinas.openheadunit.connection.AccessoryConnection
import com.andrerinas.openheadunit.utils.AppLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Only a first or unfragmented frame carries the 2-byte message type, so a continuation fragment
 * of a single byte is legal. Dropping it truncated the frame it belonged to.
 */
class AapMessageIncomingTest {

    /** Hands back [next] as the decrypted payload, like AapSslContext's reused buffer. */
    private class FakeSsl : AapSsl {
        var next: ByteArrayWithLimit? = null
        override fun decrypt(start: Int, length: Int, buffer: ByteArray) = next
        override fun encrypt(offset: Int, length: Int, buffer: ByteArray): ByteArrayWithLimit? = null
        override fun postHandshakeReset() {}
        override fun performHandshake(connection: AccessoryConnection) = true
        override fun release() {}
    }

    private val ssl = FakeSsl()

    @Before
    fun quietLog() {
        AppLog.LOGGER = object : AppLog.Logger { override fun println(priority: Int, tag: String, msg: String) {} }
    }

    private fun decrypt(flags: Int, payload: ByteArray): AapMessage? {
        ssl.next = ByteArrayWithLimit(payload.copyOf(1024), payload.size)
        val header = AapMessageIncoming.EncryptedHeader().apply {
            chan = Channel.ID_VID
            this.flags = flags
            enc_len = payload.size
        }
        return AapMessageIncoming.decrypt(header, 0, ByteArray(1024), ssl)
    }

    @Test
    fun `a one-byte last fragment completes the frame`() {
        val assembler = VideoFragmentAssembler()

        val first = decrypt(VideoFragmentAssembler.FLAG_FIRST, byteArrayOf(0, 1, 0, 0, 0, 1, 0x65, 1, 2, 3))
        assertNotNull(first)
        assertEquals(
            Action.BeginAssembly(VideoFragmentAssembler.OFFSET_MEDIA_INDICATION),
            assembler.onMessage(first!!.flags.toInt(), payloadStartsAt10 = false, payloadStartsAt2 = true).action
        )

        val last = decrypt(VideoFragmentAssembler.FLAG_LAST, byteArrayOf(0x42))
        assertNotNull("a 1-byte continuation must not be dropped", last)
        assertEquals(1, last!!.size)
        assertEquals(
            Action.AppendAndDecode,
            assembler.onMessage(last.flags.toInt(), payloadStartsAt10 = false, payloadStartsAt2 = false).action
        )
    }

    @Test
    fun `a one-byte middle fragment is kept`() {
        assertNotNull(decrypt(VideoFragmentAssembler.FLAG_MIDDLE, byteArrayOf(0x42)))
    }

    @Test
    fun `a first or unfragmented frame still needs its two-byte type`() {
        assertNull(decrypt(VideoFragmentAssembler.FLAG_FIRST, byteArrayOf(0)))
        assertNull(decrypt(VideoFragmentAssembler.FLAG_SINGLE, byteArrayOf(0)))
    }

    @Test
    fun `an empty continuation is still rejected`() {
        assertNull(decrypt(VideoFragmentAssembler.FLAG_LAST, ByteArray(0)))
    }
}

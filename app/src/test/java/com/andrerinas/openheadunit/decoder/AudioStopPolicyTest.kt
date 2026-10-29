package com.andrerinas.openheadunit.decoder

import com.andrerinas.openheadunit.aap.protocol.Channel
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioStopPolicyTest {
    @Test
    fun `pausing music drops what is buffered, prompts still finish`() {
        // MG4, 2026-10-19: with music over Wi-Fi, pause took over a second because stop() played out
        // the ~1.2 s jitter buffer first. A nav prompt or the assistant must not be cut off mid-word.
        assertEquals(true, AudioStopPolicy.discardBufferedOnStop(Channel.ID_AUD))
        assertEquals(false, AudioStopPolicy.discardBufferedOnStop(Channel.ID_AU1))
        assertEquals(false, AudioStopPolicy.discardBufferedOnStop(Channel.ID_AU2))
    }

    @Test
    fun `the phone reporting pause or stop drops the music buffer, playing keeps it`() {
        // MG4, 2026-10-20: YouTube Music paused 0.38 s after the OK press but the car played its
        // 1.2 s buffer out first; the phone's own "audio stop" came 3.3 s later, too late to help.
        // Android Auto MediaPlaybackStatus: 1 stopped, 2 playing, 3 paused.
        assertEquals(true, AudioStopPolicy.discardOnPlaybackStatus(3))
        assertEquals(true, AudioStopPolicy.discardOnPlaybackStatus(1))
        assertEquals(false, AudioStopPolicy.discardOnPlaybackStatus(2))
    }

    @Test
    fun `music the phone sends after its pause stays silent until it reports playing again`() {
        // MG4, 2026-10-20 11:04: after the discard the phone kept streaming ~3 s of tail, which re-buffered and played.
        assertEquals(true, AudioStopPolicy.resumeOnPlaybackStatus(2))
        assertEquals(false, AudioStopPolicy.resumeOnPlaybackStatus(3))
        assertEquals(false, AudioStopPolicy.resumeOnPlaybackStatus(1))
    }
}

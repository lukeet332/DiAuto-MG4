package com.andrerinas.openheadunit.aap

import android.media.AudioAttributes
import com.andrerinas.openheadunit.aap.protocol.Channel
import com.andrerinas.openheadunit.aap.protocol.proto.Control.AudioFocusRequestNotification.AudioFocusRequestType
import org.junit.Assert.assertEquals
import org.junit.Test

/** As stock AllGo Android Auto (AAP_EH32 AndroidAutoMainService): only music is the car's Android Auto source. */
class AaAudioUsagePolicyTest {

    @Test
    fun `only music claims the Android Auto car source, speech is guidance unless the assistant is talking`() {
        // MG4, 2026-10-20 13:03: a Maps prompt arrived on AUDIO1 (speech) and went out as the assistant,
        // so the car moved its source off the radio for the prompt. Stock uses guidance (12) unless VR is on.
        val music = AaAudioUsagePolicy.Attrs(AudioAttributes.USAGE_MEDIA, AudioAttributes.CONTENT_TYPE_MUSIC, 70)
        val guidance = AaAudioUsagePolicy.Attrs(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE, AudioAttributes.CONTENT_TYPE_SPEECH, null)
        val assistant = AaAudioUsagePolicy.Attrs(AudioAttributes.USAGE_ASSISTANT, AudioAttributes.CONTENT_TYPE_SPEECH, null)
        assertEquals(music, AaAudioUsagePolicy.forChannel(Channel.ID_AUD, assistantActive = false))
        assertEquals(guidance, AaAudioUsagePolicy.forChannel(Channel.ID_AU1, assistantActive = false))
        assertEquals(assistant, AaAudioUsagePolicy.forChannel(Channel.ID_AU1, assistantActive = true))
        assertEquals(guidance, AaAudioUsagePolicy.forChannel(Channel.ID_AU2, assistantActive = true))
    }

    @Test
    fun `the phone's short focus requests are a nav prompt or the assistant, never music`() {
        assertEquals(AaAudioUsagePolicy.forChannel(Channel.ID_AU1, assistantActive = false), AaAudioUsagePolicy.forFocusRequest(AudioFocusRequestType.GAIN_TRANSIENT_MAY_DUCK_VALUE))
        assertEquals(AaAudioUsagePolicy.forChannel(Channel.ID_AU1, assistantActive = true), AaAudioUsagePolicy.forFocusRequest(AudioFocusRequestType.GAIN_TRANSIENT_VALUE))
        assertEquals(AaAudioUsagePolicy.forChannel(Channel.ID_AUD, assistantActive = false), AaAudioUsagePolicy.forFocusRequest(AudioFocusRequestType.GAIN_VALUE))
    }

    @Test
    fun `a prompt the phone already took focus for does not ask the car a second time`() {
        // MG4, 2026-10-20 13:08: one Maps prompt made two focus requests, the second taking focus from
        // the first, so the car changed source twice. Stock skips the stream's request when it holds focus.
        assertEquals(false, AaAudioUsagePolicy.speechStreamNeedsFocus(phoneFocusHeld = true))
        assertEquals(true, AaAudioUsagePolicy.speechStreamNeedsFocus(phoneFocusHeld = false))
    }
}

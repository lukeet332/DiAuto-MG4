package com.andrerinas.openheadunit.aap

import android.media.AudioAttributes
import com.andrerinas.openheadunit.aap.protocol.Channel
import com.andrerinas.openheadunit.aap.protocol.proto.Control.AudioFocusRequestNotification.AudioFocusRequestType
import com.evsuite.hardware.saic.SaicCarAudioSource

/**
 * Audio attributes for Android Auto's streams and focus, as stock AllGo Android Auto sets them: music
 * is the car's Android Auto source (70); speech (AUDIO1, prompts and assistant replies) is guidance
 * unless a voice session is running, and carries no source, so it ducks the radio.
 */
object AaAudioUsagePolicy {
    data class Attrs(val usage: Int, val contentType: Int, val carSourceType: Int?)

    private val MUSIC = Attrs(AudioAttributes.USAGE_MEDIA, AudioAttributes.CONTENT_TYPE_MUSIC, SaicCarAudioSource.SRC_ANDROID_AUTO)
    private val NAVIGATION = Attrs(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE, AudioAttributes.CONTENT_TYPE_SPEECH, null)
    private val ASSISTANT = Attrs(AudioAttributes.USAGE_ASSISTANT, AudioAttributes.CONTENT_TYPE_SPEECH, null)

    fun forChannel(channel: Int, assistantActive: Boolean): Attrs = when (channel) {
        Channel.ID_AU1 -> if (assistantActive) ASSISTANT else NAVIGATION
        Channel.ID_AU2 -> NAVIGATION
        else -> MUSIC
    }

    fun forFocusRequest(requestType: Int): Attrs = when (requestType) {
        AudioFocusRequestType.GAIN_TRANSIENT_MAY_DUCK_VALUE -> NAVIGATION
        AudioFocusRequestType.GAIN_TRANSIENT_VALUE -> ASSISTANT
        else -> MUSIC
    }

    /** A speech stream asks for focus only if the phone has not already taken it for the same prompt. */
    fun speechStreamNeedsFocus(phoneFocusHeld: Boolean): Boolean = !phoneFocusHeld

    fun audioAttributes(attrs: Attrs): AudioAttributes {
        val builder = AudioAttributes.Builder().setUsage(attrs.usage).setContentType(attrs.contentType)
        attrs.carSourceType?.let { SaicCarAudioSource.attachSourceType(builder, it) }
        return builder.build()
    }
}

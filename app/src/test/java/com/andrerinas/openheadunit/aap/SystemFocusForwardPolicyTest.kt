package com.andrerinas.openheadunit.aap

import android.media.AudioManager
import com.andrerinas.openheadunit.aap.protocol.proto.Control.AudioFocusNotification.AudioFocusStateType
import org.junit.Assert.assertEquals
import org.junit.Test

/** Mirrors stock AllGo Android Auto's afChangeListenerMedia (AAP_EH32, decompiled 2026-10-20). */
class SystemFocusForwardPolicyTest {

    private fun state(change: Int, mediaPlaying: Boolean = true, phoneTransient: Boolean = false, lossForwarded: Boolean = false, bluetooth: Boolean = false) =
        SystemFocusForwardPolicy.stateFor(musicViaBluetooth = bluetooth, mediaPlaying = mediaPlaying, phoneHoldsTransientFocus = phoneTransient, lossForwarded = lossForwarded, systemFocusChange = change)

    @Test
    fun `the car taking the music focus pauses or ducks the phone`() {
        // MG4, 2026-10-19: the car radio took focus but the phone was never told, so both played.
        assertEquals(AudioFocusStateType.STATE_LOSS, state(AudioManager.AUDIOFOCUS_LOSS))
        assertEquals(AudioFocusStateType.STATE_LOSS, state(AudioManager.AUDIOFOCUS_LOSS, mediaPlaying = false))
        assertEquals(AudioFocusStateType.STATE_LOSS_TRANSIENT, state(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT))
        assertEquals(AudioFocusStateType.STATE_LOSS_TRANSIENT_CAN_DUCK, state(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK))
        assertEquals(null, state(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK, mediaPlaying = false))
        assertEquals(null, state(12345))
    }

    @Test
    fun `the phone's own nav prompt or Assistant is not sent back to it as a pause or duck`() {
        // MG4, 2026-10-20 10:46: DiAuto's own focus requests were forwarded and paused YouTube.
        assertEquals(null, state(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, phoneTransient = true))
        assertEquals(null, state(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK, phoneTransient = true))
        assertEquals(AudioFocusStateType.STATE_LOSS, state(AudioManager.AUDIOFOCUS_LOSS, phoneTransient = true))
    }

    @Test
    fun `focus coming back is forwarded only after a loss we forwarded`() {
        assertEquals(AudioFocusStateType.STATE_GAIN, state(AudioManager.AUDIOFOCUS_GAIN, lossForwarded = true))
        assertEquals(null, state(AudioManager.AUDIOFOCUS_GAIN, lossForwarded = false))
    }

    @Test
    fun `with music over Bluetooth the car arbitrates radio and phone itself, so nothing is forwarded`() {
        // MG4, 2026-10-19 evening: forwarding the radio's grab paused the MG4 Radio app, which paused the radio.
        assertEquals(null, state(AudioManager.AUDIOFOCUS_LOSS, bluetooth = true))
        assertEquals(null, state(AudioManager.AUDIOFOCUS_GAIN, lossForwarded = true, bluetooth = true))
    }
}

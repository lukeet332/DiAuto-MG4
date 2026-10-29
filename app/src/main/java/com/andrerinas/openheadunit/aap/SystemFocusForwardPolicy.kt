package com.andrerinas.openheadunit.aap

import android.media.AudioManager
import com.andrerinas.openheadunit.aap.protocol.proto.Control.AudioFocusNotification.AudioFocusStateType

/**
 * What to tell the phone when the car takes or returns the music focus DiAuto holds for it, as stock
 * AllGo Android Auto does: a transient loss or duck while the phone holds transient focus (nav
 * prompt, Assistant) is its own; a regain only after a loss we forwarded. Over Bluetooth the car
 * arbitrates itself.
 */
object SystemFocusForwardPolicy {
    fun stateFor(musicViaBluetooth: Boolean, mediaPlaying: Boolean, phoneHoldsTransientFocus: Boolean, lossForwarded: Boolean, systemFocusChange: Int): AudioFocusStateType? {
        if (musicViaBluetooth) return null
        return when (systemFocusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> AudioFocusStateType.STATE_LOSS
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> AudioFocusStateType.STATE_LOSS_TRANSIENT.takeUnless { phoneHoldsTransientFocus }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> AudioFocusStateType.STATE_LOSS_TRANSIENT_CAN_DUCK.takeIf { mediaPlaying && !phoneHoldsTransientFocus }
            AudioManager.AUDIOFOCUS_GAIN -> AudioFocusStateType.STATE_GAIN.takeIf { lossForwarded }
            else -> null
        }
    }
}

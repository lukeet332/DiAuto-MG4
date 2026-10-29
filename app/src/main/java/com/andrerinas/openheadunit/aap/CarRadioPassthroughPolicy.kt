package com.andrerinas.openheadunit.aap

/**
 * Music rides Android Auto over Wi-Fi, except for the MG4 Radio app: it plays no audio and drives
 * the car tuner through DiAuto's radio bridge, so the car radio must stay the source for it.
 */
object CarRadioPassthroughPolicy {
    private const val CAR_RADIO_APP = "MG4 Radio"

    fun isCarRadioApp(mediaSource: String?): Boolean = mediaSource?.contains(CAR_RADIO_APP, ignoreCase = true) == true

    /** Stock AllGo holds the car's music focus while the phone asks for it; the radio app keeps the car radio. */
    fun holdsCarMediaFocus(phoneWantsMusicFocus: Boolean, mediaSource: String?): Boolean =
        phoneWantsMusicFocus && !isCarRadioApp(mediaSource)

    /** A music request made before or for the radio app is the radio's; the next app must ask again. */
    fun phoneWantsMusicAfterSourceChange(wanted: Boolean, previousSource: String?): Boolean =
        wanted && !isCarRadioApp(previousSource)

    /**
     * The Android Auto key for a right-cluster wheel key, or null to leave it to the car. SystemUI maps
     * these per SWC mode (0 head unit, 1 cluster, 2 climate) and the radio, BT music and phone services
     * act on them themselves; Android Auto gets them only while its music is the car's source.
     */
    fun wheelKeyForAndroidAuto(saicCode: Int, carMusicFocusHeld: Boolean, swcMode: Int?, callActive: Boolean): Int? {
        if (!carMusicFocusHeld || callActive || (swcMode != null && swcMode != SWC_MODE_HEAD_UNIT)) return null
        return when (saicCode) {
            SAIC_OK -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            SAIC_LEFT -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
            SAIC_RIGHT -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
            else -> null
        }
    }

    const val SAIC_LEFT = 299
    const val SAIC_RIGHT = 300
    const val SAIC_OK = 301
    private const val SWC_MODE_HEAD_UNIT = 0
}

/** What the phone is projecting right now, shared by the audio, control and service layers. */
object ProjectedMedia {
    /** Android Auto's media_source for the playing app, e.g. "YouTube Music" or "MG4 Radio". */
    @Volatile var source: String? = null
    @Volatile var wifiMusicPlaying = false
    /** We told the phone it lost focus because the car took it; only then is a regain forwarded. */
    @Volatile var lossForwarded = false
    /** The phone's last focus request was transient (nav prompt, Assistant) and not yet released. */
    @Volatile var phoneTransientFocus = false
    /** The phone reported a voice session (VoiceSessionNotification START until STOP). */
    @Volatile var assistantActive = false
    /** The phone asked for music focus (GAIN) and has not released it. */
    @Volatile var phoneWantsMusicFocus = false
    /** Re-checks the car's music focus; set by the control channel for the session. */
    @Volatile var onMusicFocusInputsChanged: (() -> Unit)? = null

    fun reset() {
        source = null
        wifiMusicPlaying = false
        lossForwarded = false
        phoneTransientFocus = false
        phoneWantsMusicFocus = false
    }
}

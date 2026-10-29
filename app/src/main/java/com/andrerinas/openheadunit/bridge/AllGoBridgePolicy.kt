package com.andrerinas.openheadunit.bridge

import com.andrerinas.openheadunit.connection.CommManager.ConnectionState

/** Messenger contract with the AllGo bridge app (allgobridge module, LinkProtocol). */
object AllGoBridgePolicy {
    const val HELLO = 1
    const val PHONE_CONNECTED = 2
    const val PHONE_GONE = 3
    const val SCREEN = 4
    const val CALL_STATE = 5
    const val PLAYBACK = 6
    const val NAVIGATION = 7
    const val NOW_PLAYING = 8
    const val PLAY_STATE = 9
    const val NAV_FOCUS = 10
    /** Album art over this is dropped rather than risk the binder transaction limit. */
    const val MAX_ART_BYTES = 512 * 1024
    const val KEY_NAME = "name"
    const val KEY_BT_MAC = "btMac"
    const val KEY_SONG = "song"
    const val KEY_ALBUM = "album"
    const val KEY_ARTIST = "artist"
    const val KEY_ART = "art"
    const val KEY_PLAYLIST = "playlist"
    const val KEY_DURATION_MS = "durationMs"
    const val KEY_RATING = "rating"
    const val KEY_STATE = "state"
    const val KEY_MEDIA_SOURCE = "mediaSource"
    const val KEY_POSITION_MS = "positionMs"
    const val KEY_SHUFFLE = "shuffle"
    const val KEY_REPEAT = "repeat"
    const val KEY_REPEAT_ONE = "repeatOne"

    const val CMD_SHOW = 101
    const val CMD_HIDE = 102
    const val CMD_KEY = 103

    /** AllGo call state from Telecom (calls run over the car's Bluetooth hands-free). */
    fun callState(ringing: Boolean, inCall: Boolean): Int = when {
        ringing -> 2
        inCall -> 1
        else -> 0
    }

    /** Home and Android Auto's screen keys act on the projection; media keys work with it in the background. */
    fun keyNeedsProjection(key: Int): Boolean = key == android.view.KeyEvent.KEYCODE_HOME || key >= 65536

    /** Re-launching a visible projection pauses and resumes it, dropping video focus for a moment. */
    fun shouldShowProjection(projectionVisible: Boolean): Boolean = !projectionVisible

    fun playbackState(isPlaying: Boolean): Int = if (isPlaying) 1 else 2

    fun messageFor(state: ConnectionState): Int? = when (state) {
        is ConnectionState.TransportStarted -> PHONE_CONNECTED
        is ConnectionState.Disconnected, is ConnectionState.Error -> PHONE_GONE
        else -> null
    }
}

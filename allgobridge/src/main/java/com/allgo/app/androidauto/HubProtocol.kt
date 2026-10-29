package com.allgo.app.androidauto

/** Binder contract between the AllGo hub (com.allgo.rui) and AllGo's Android Auto app, from the hub's AIDL proxies. */
object HubProtocol {
    const val MAIN_SERVICE = "com.allgo.app.androidauto.IAndroidAutoMainService"
    const val DEVICE_CALLBACK = "com.allgo.mirroring.deviceservice.IAndroidAutoDeviceCallback"
    const val SESSION_CALLBACK = "com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback"

    // IAndroidAutoMainService
    const val REGISTER = 1
    const val START_SESSION = 2
    const val RESUME_SESSION = 3
    const val PAUSE_SESSION = 4
    const val STOP_SESSION = 5
    const val GET_ACTIVE_DEVICE = 6
    const val IS_SESSION_ACTIVE = 7
    const val UNREGISTER = 8
    const val LAUNCH_GOOGLE_ASSISTANT = 10
    const val LAUNCH_APP = 11
    const val GET_PLUGIN_VERSION = 12
    const val GET_VR_STATUS = 24
    const val SEND_MEDIA_PLAY_CONTROL = 25
    /** Sensor data and setAttributes: int replies, parcels left unread. */
    val INT_REPLY_IGNORED = 13..18
    /** disableTouchInput, disableVideoFocus, disableAAP, registerCallback, surfaceDestroyed/Changed. */
    val VOID_IGNORED = setOf(9, 19, 20, 21, 22, 23)

    // IAndroidAutoDeviceCallback
    const val ON_DEVICE_CONNECTED = 1
    const val ON_DEVICE_DISCONNECTED = 2

    // IAndroidAutoSessionCallback
    const val ON_SESSION_STARTED = 1
    const val ON_SESSION_CLOSED = 2
    const val ON_PHONE_STATE_UPDATE = 5
    const val ON_PLAYBACK_UPDATE = 6
    const val ON_SCREEN_STARTED = 8
    const val ON_SCREEN_STOPPED = 9
    /** (message, type 0 = status, Bitmap icon), as AllGo Android Auto sends navigation status. */
    const val ON_AAP_NAVIGATION_STATUS = 16
}

/** Messenger contract with DiAuto; mirrored in DiAuto's AllGoBridgeLink. */
object LinkProtocol {
    const val MSG_HELLO = 1
    const val MSG_PHONE_CONNECTED = 2
    const val MSG_PHONE_GONE = 3
    const val MSG_SCREEN = 4
    const val MSG_CALL_STATE = 5
    const val MSG_PLAYBACK = 6
    const val MSG_NAVIGATION = 7
    const val MSG_NOW_PLAYING = 8
    const val MSG_PLAY_STATE = 9
    /** arg1 1 = the phone took navigation focus, 0 = it gave it up. */
    const val MSG_NAV_FOCUS = 10
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
}

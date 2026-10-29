package com.allgo.app.androidauto

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log

/** DiAuto binds this to report its session and to receive the car's requests. */
class DiAutoLinkService : Service() {

    override fun onCreate() {
        super.onCreate()
        Bridge.init(this)
    }

    override fun onBind(intent: Intent?): IBinder = Messenger(LinkHandler()).binder

    private class LinkHandler : Handler(Bridge.handler.looper) {
        override fun handleMessage(msg: Message) {
            try {
                when (msg.what) {
                    LinkProtocol.MSG_HELLO -> msg.replyTo?.let { Bridge.diAutoLinked(it) }
                    LinkProtocol.MSG_PHONE_CONNECTED -> {
                        val data = msg.data
                        Bridge.phoneConnected(Phone(data.getString(LinkProtocol.KEY_NAME), data.getString(LinkProtocol.KEY_BT_MAC)))
                    }
                    LinkProtocol.MSG_PHONE_GONE -> Bridge.phoneGone()
                    LinkProtocol.MSG_SCREEN -> Bridge.screen(visible = msg.arg1 != 0)
                    LinkProtocol.MSG_CALL_STATE -> Bridge.callState(msg.arg1)
                    LinkProtocol.MSG_PLAYBACK -> Bridge.playback(msg.arg1)
                    LinkProtocol.MSG_NAVIGATION -> Bridge.navigation(msg.arg1)
                    LinkProtocol.MSG_NAV_FOCUS -> Bridge.navigationFocus(gain = msg.arg1 != 0)
                    LinkProtocol.MSG_NOW_PLAYING -> msg.data.let { d ->
                        MediaFeed.nowPlaying(NowPlaying(
                            song = d.getString(LinkProtocol.KEY_SONG),
                            album = d.getString(LinkProtocol.KEY_ALBUM),
                            artist = d.getString(LinkProtocol.KEY_ARTIST),
                            art = d.getByteArray(LinkProtocol.KEY_ART),
                            playlist = d.getString(LinkProtocol.KEY_PLAYLIST),
                            durationMs = d.getInt(LinkProtocol.KEY_DURATION_MS),
                            rating = d.getInt(LinkProtocol.KEY_RATING),
                        ))
                    }
                    LinkProtocol.MSG_PLAY_STATE -> msg.data.let { d ->
                        MediaFeed.playState(PlayState(
                            state = d.getInt(LinkProtocol.KEY_STATE),
                            mediaSource = d.getString(LinkProtocol.KEY_MEDIA_SOURCE),
                            positionMs = d.getInt(LinkProtocol.KEY_POSITION_MS),
                            shuffle = d.getInt(LinkProtocol.KEY_SHUFFLE),
                            repeat = d.getInt(LinkProtocol.KEY_REPEAT),
                            repeatOne = d.getInt(LinkProtocol.KEY_REPEAT_ONE),
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.w(Bridge.TAG, "link message ${msg.what} failed: $e")
            }
        }
    }
}

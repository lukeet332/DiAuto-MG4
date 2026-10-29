package com.allgo.app.androidauto

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import android.util.Log

data class NowPlaying(
    val song: String?,
    val album: String?,
    val artist: String?,
    val art: ByteArray?,
    val playlist: String?,
    val durationMs: Int,
    val rating: Int,
)

/** [state] is Android Auto's: 1 stopped, 2 playing, 3 paused (AllGo's media browser takes it as is). */
data class PlayState(val state: Int, val mediaSource: String?, val positionMs: Int, val shuffle: Int, val repeat: Int, val repeatOne: Int)

/** RemoteUIMediaPlaybackMetadata / RemoteUIMediaPlaybackInfo, as stock AllGo Android Auto writes them. */
object MediaFeedParcels {
    fun writeMetadata(parcel: Parcel, now: NowPlaying) {
        val art = now.art ?: ByteArray(0)
        parcel.writeInt(1)
        parcel.writeString(now.song ?: "")
        parcel.writeString(now.album ?: "")
        parcel.writeString(now.artist ?: "")
        parcel.writeInt(art.size)
        parcel.writeInt(art.size)
        parcel.writeByteArray(art)
        parcel.writeString(now.playlist ?: "")
        parcel.writeInt(now.durationMs)
        parcel.writeInt(now.rating)
    }

    fun writePlaybackInfo(parcel: Parcel, play: PlayState) {
        parcel.writeInt(1)
        parcel.writeInt(play.state)
        parcel.writeString(play.mediaSource ?: "")
        parcel.writeInt(play.positionMs)
        parcel.writeInt(play.shuffle)
        parcel.writeInt(play.repeat)
        parcel.writeInt(play.repeatOne)
        parcel.writeInt(1)
    }
}

/**
 * Feeds Android Auto's now-playing into AllGo's own media browser service, which the car's home
 * card, cluster, status bar and voice control all read. Binds it the way AllGo's Android Auto app did.
 */
object MediaFeed {
    private const val DESCRIPTOR = "com.allgo.remoteui.mediabrowserservice.RemoteUIMediaBrowserServiceInterface"
    private const val ON_METADATA = 1
    private const val ON_PLAYBACK = 2
    private const val ON_SESSION_STATUS = 3
    private val SERVICE = ComponentName(
        "com.allgo.remoteui.mediabrowserservice",
        "com.allgo.remoteui.mediabrowserservice.RemoteUIMediaBrowserService",
    )

    private var service: IBinder? = null
    private var bound = false
    private var sessionActive = false
    private var lastNowPlaying: NowPlaying? = null
    private var lastPlayState: PlayState? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            Bridge.handler.post {
                service = binder
                Log.i(Bridge.TAG, "media browser linked")
                if (sessionActive) {
                    call(ON_SESSION_STATUS) { it.writeInt(1) }
                    lastNowPlaying?.let { now -> call(ON_METADATA) { MediaFeedParcels.writeMetadata(it, now) } }
                    lastPlayState?.let { play -> call(ON_PLAYBACK) { MediaFeedParcels.writePlaybackInfo(it, play) } }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Bridge.handler.post { service = null }
        }
    }

    /** Call on the bridge thread. */
    fun bind(context: Context) {
        if (bound) return
        bound = try {
            context.bindService(Intent("com.allgo.action.aapservice").setComponent(SERVICE), connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.w(Bridge.TAG, "media browser bind refused: $e")
            false
        }
    }

    fun session(active: Boolean) {
        if (active == sessionActive) return
        sessionActive = active
        if (!active) {
            lastNowPlaying = null
            lastPlayState = null
        }
        call(ON_SESSION_STATUS) { it.writeInt(if (active) 1 else 0) }
    }

    fun nowPlaying(now: NowPlaying) {
        if (!sessionActive) return
        lastNowPlaying = now
        call(ON_METADATA) { MediaFeedParcels.writeMetadata(it, now) }
    }

    fun playState(play: PlayState) {
        if (!sessionActive) return
        lastPlayState = play
        call(ON_PLAYBACK) { MediaFeedParcels.writePlaybackInfo(it, play) }
    }

    private fun call(code: Int, write: (Parcel) -> Unit) {
        val target = service ?: return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(DESCRIPTOR)
            write(data)
            target.transact(code, data, reply, 0)
            reply.readException()
        } catch (e: Exception) {
            Log.w(Bridge.TAG, "media browser call $code failed: $e")
        } finally {
            data.recycle()
            reply.recycle()
        }
    }
}

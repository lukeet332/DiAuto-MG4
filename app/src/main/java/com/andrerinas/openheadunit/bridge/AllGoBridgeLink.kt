package com.andrerinas.openheadunit.bridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.aap.AapProjectionActivity
import com.andrerinas.openheadunit.connection.CommManager
import com.andrerinas.openheadunit.utils.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Reports DiAuto sessions to the AllGo bridge (installed over com.allgo.app.androidauto) so the
 * car's own apps treat them as native Android Auto, and carries out the car's requests.
 * Does nothing when the bridge is not installed.
 */
object AllGoBridgeLink {
    private val BRIDGE = ComponentName("com.allgo.app.androidauto", "com.allgo.app.androidauto.DiAutoLinkService")

    /** Phone of the current wireless session, from the Bluetooth handshake. */
    @Volatile var phoneName: String? = null
    @Volatile var phoneBtMac: String? = null

    private val main = Handler(Looper.getMainLooper())
    private var scope: CoroutineScope? = null
    private var bridge: Messenger? = null
    private var appContext: Context? = null
    @Volatile private var sessionUp = false
    @Volatile private var screenVisible = false
    @Volatile private var lastCallState = 0
    /** A call is ringing or in progress on the car's Bluetooth hands-free. */
    val callActive: Boolean get() = lastCallState != 0
    @Volatile private var lastPlayback = 0
    @Volatile private var lastNavigation = -1
    @Volatile private var navigationFocus = false
    @Volatile private var lastNowPlaying: Bundle? = null
    @Volatile private var lastPlayState: Bundle? = null
    /** A key for Android Auto that arrived while the projection was still coming to the front. */
    private var pendingKey: Int? = null

    private val replies by lazy { Messenger(CommandHandler()) }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            AppLog.i("AllGoBridge: linked")
            bridge = Messenger(service)
            send(AllGoBridgePolicy.HELLO)
            // A restarted bridge knows nothing; bring it up to date.
            if (sessionUp) sendPhoneConnected()
            if (sessionUp) send(AllGoBridgePolicy.SCREEN, if (screenVisible) 1 else 0)
            if (sessionUp && lastCallState != 0) send(AllGoBridgePolicy.CALL_STATE, lastCallState)
            if (sessionUp && lastPlayback != 0) send(AllGoBridgePolicy.PLAYBACK, lastPlayback)
            if (sessionUp) replayMedia()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            AppLog.w("AllGoBridge: link lost")
            bridge = null
        }
    }

    fun start(context: Context) {
        if (scope != null) return
        val app = context.applicationContext
        appContext = app
        val bound = try {
            app.bindService(Intent().setComponent(BRIDGE), connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            AppLog.w("AllGoBridge: bind refused: ${e.message}")
            false
        }
        if (!bound) {
            AppLog.i("AllGoBridge: bridge not installed")
            appContext = null
            return
        }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main).also { s ->
            s.launch {
                App.provide(app).commManager.connectionState.collect { state ->
                    when (AllGoBridgePolicy.messageFor(state)) {
                        AllGoBridgePolicy.PHONE_CONNECTED -> {
                            sessionUp = true
                            sendPhoneConnected()
                            send(AllGoBridgePolicy.SCREEN, if (screenVisible) 1 else 0)
                            replayMedia()
                        }
                        AllGoBridgePolicy.PHONE_GONE -> if (sessionUp) {
                            sessionUp = false
                            lastCallState = 0
                            lastPlayback = 0
                            lastNavigation = -1
                            navigationFocus = false
                            lastNowPlaying = null
                            lastPlayState = null
                            send(AllGoBridgePolicy.PHONE_GONE)
                        }
                    }
                }
            }
            app.registerReceiver(callReceiver, android.content.IntentFilter(ACTION_HFP_CALL_CHANGED))
        }
    }

    fun stop(context: Context) {
        val s = scope ?: return
        if (sessionUp) send(AllGoBridgePolicy.PHONE_GONE)
        sessionUp = false
        s.cancel()
        scope = null
        try {
            context.applicationContext.unregisterReceiver(callReceiver)
        } catch (_: Exception) {
        }
        try {
            context.applicationContext.unbindService(connection)
        } catch (_: Exception) {
        }
        bridge = null
        appContext = null
    }

    /** Android Auto playback, from the phone's media status. */
    fun playback(isPlaying: Boolean) {
        val state = AllGoBridgePolicy.playbackState(isPlaying)
        if (state == lastPlayback) return
        lastPlayback = state
        if (sessionUp) send(AllGoBridgePolicy.PLAYBACK, state)
    }

    /** Android Auto NavigationStatus (0 unavailable, 1 active, 2 inactive, 3 rerouting). */
    fun navigation(status: Int) {
        if (status == lastNavigation) return
        lastNavigation = status
        if (sessionUp) send(AllGoBridgePolicy.NAVIGATION, status)
    }

    /** The phone's navigation focus; the bridge holds the car's NAVIGATION app focus for it, as stock AllGo. */
    fun navigationFocus(gain: Boolean) {
        if (gain == navigationFocus) return
        navigationFocus = gain
        if (sessionUp) send(AllGoBridgePolicy.NAV_FOCUS, if (gain) 1 else 0)
    }

    /** Now-playing for the car's own media screens (home card, cluster, status bar). */
    fun nowPlaying(meta: com.andrerinas.openheadunit.aap.protocol.proto.MediaPlayback.MediaMetaData) {
        val art = if (meta.hasAlbumArt()) meta.albumArt.toByteArray().takeIf { it.size <= AllGoBridgePolicy.MAX_ART_BYTES } else null
        val data = Bundle().apply {
            putString(AllGoBridgePolicy.KEY_SONG, meta.song)
            putString(AllGoBridgePolicy.KEY_ALBUM, meta.album)
            putString(AllGoBridgePolicy.KEY_ARTIST, meta.artist)
            putByteArray(AllGoBridgePolicy.KEY_ART, art)
            putString(AllGoBridgePolicy.KEY_PLAYLIST, meta.playlist)
            putInt(AllGoBridgePolicy.KEY_DURATION_MS, meta.durationSeconds * 1000)
            putInt(AllGoBridgePolicy.KEY_RATING, meta.rating)
        }
        lastNowPlaying = data
        if (sessionUp) sendBundle(AllGoBridgePolicy.NOW_PLAYING, data)
    }

    fun playState(status: com.andrerinas.openheadunit.aap.protocol.proto.MediaPlayback.MediaPlaybackStatus) {
        val data = Bundle().apply {
            putInt(AllGoBridgePolicy.KEY_STATE, status.state.number)
            putString(AllGoBridgePolicy.KEY_MEDIA_SOURCE, status.mediaSource)
            putInt(AllGoBridgePolicy.KEY_POSITION_MS, status.playbackSeconds * 1000)
            putInt(AllGoBridgePolicy.KEY_SHUFFLE, if (status.shuffle) 1 else 0)
            putInt(AllGoBridgePolicy.KEY_REPEAT, if (status.repeat) 1 else 0)
            putInt(AllGoBridgePolicy.KEY_REPEAT_ONE, if (status.repeatOne) 1 else 0)
        }
        lastPlayState = data
        if (sessionUp) sendBundle(AllGoBridgePolicy.PLAY_STATE, data)
    }

    private fun replayMedia() {
        if (lastNavigation >= 0) send(AllGoBridgePolicy.NAVIGATION, lastNavigation)
        if (navigationFocus) send(AllGoBridgePolicy.NAV_FOCUS, 1)
        lastNowPlaying?.let { sendBundle(AllGoBridgePolicy.NOW_PLAYING, it) }
        lastPlayState?.let { sendBundle(AllGoBridgePolicy.PLAY_STATE, it) }
    }

    private fun sendBundle(what: Int, data: Bundle) {
        val message = Message.obtain(null, what)
        message.data = Bundle(data)
        deliver(message)
    }

    /** Calls go over the car's Bluetooth hands-free: its call-changed broadcast drives the update. */
    private val callReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!sessionUp) return
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            val state = try {
                // isRinging is a system API; the car build runs as the system uid.
                val ringing = android.telecom.TelecomManager::class.java.getMethod("isRinging").invoke(telecom) == true
                AllGoBridgePolicy.callState(ringing, telecom.isInCall)
            } catch (_: Exception) {
                return
            }
            if (state != lastCallState) {
                lastCallState = state
                send(AllGoBridgePolicy.CALL_STATE, state)
            }
        }
    }

    /** Projection activity foreground state: the car hides its own call screen while it is up. */
    fun screen(visible: Boolean) {
        screenVisible = visible
        if (visible) pendingKey?.let { key ->
            pendingKey = null
            // Android Auto ignores keys until the projection has video focus again.
            main.postDelayed({ tapKey(key) }, KEY_AFTER_SHOW_MS)
        }
        if (sessionUp) send(AllGoBridgePolicy.SCREEN, if (visible) 1 else 0)
    }

    private fun sendPhoneConnected() {
        val message = Message.obtain(null, AllGoBridgePolicy.PHONE_CONNECTED)
        message.data = Bundle().apply {
            putString(AllGoBridgePolicy.KEY_NAME, phoneName)
            putString(AllGoBridgePolicy.KEY_BT_MAC, phoneBtMac)
        }
        deliver(message)
    }

    private fun send(what: Int, arg1: Int = 0) = deliver(Message.obtain(null, what, arg1, 0))

    private fun deliver(message: Message) {
        val target = bridge ?: return
        message.replyTo = replies
        try {
            target.send(message)
        } catch (e: Exception) {
            AppLog.w("AllGoBridge: send ${message.what} failed: ${e.message}")
        }
    }

    private class CommandHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val context = appContext ?: return
            try {
                when (msg.what) {
                    AllGoBridgePolicy.CMD_SHOW -> if (AllGoBridgePolicy.shouldShowProjection(screenVisible)) {
                        showProjection(context)
                    } else {
                        AppLog.i("AllGoBridge: Android Auto already on screen; not re-launching it")
                    }
                    // Stock goes native and lets the car show what it opened; launching HOME covered it.
                    AllGoBridgePolicy.CMD_HIDE -> AapProjectionActivity.moveToBack()
                    AllGoBridgePolicy.CMD_KEY -> if (screenVisible || !AllGoBridgePolicy.keyNeedsProjection(msg.arg1)) tapKey(msg.arg1) else {
                        pendingKey = msg.arg1
                        main.postDelayed({ pendingKey = null }, PENDING_KEY_TIMEOUT_MS)
                    }
                }
                AppLog.i("AllGoBridge: car request ${msg.what} ${msg.arg1}")
            } catch (e: Exception) {
                AppLog.w("AllGoBridge: car request ${msg.what} failed: ${e.message}")
            }
        }

        private fun showProjection(context: Context) {
            val connected = App.provide(context).commManager.connectionState.value is CommManager.ConnectionState.TransportStarted
            val intent = if (connected) {
                Intent(context, AapProjectionActivity::class.java).putExtra(AapProjectionActivity.EXTRA_FOCUS, true)
            } else {
                context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
            }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    private fun tapKey(key: Int) {
        val commManager = App.provide(appContext ?: return).commManager
        commManager.sendKey(key, true, null, "allgo-bridge")
        main.postDelayed({ commManager.sendKey(key, false, null, "allgo-bridge") }, KEY_TAP_MS)
        AppLog.i("AllGoBridge: sent car key $key to Android Auto")
    }

    private const val KEY_TAP_MS = 80L
    private const val KEY_AFTER_SHOW_MS = 500L
    private const val PENDING_KEY_TIMEOUT_MS = 4_000L
    private const val ACTION_HFP_CALL_CHANGED = "android.bluetooth.headsetclient.profile.action.AG_CALL_CHANGED"
}

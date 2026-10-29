package com.allgo.app.androidauto

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.Parcel
import android.util.Log
import com.allgo.app.androidauto.BridgeSession.HubEvent
import com.allgo.app.androidauto.HubCommands.Command

/**
 * Joins the stock AllGo hub (which binds this package as its Android Auto app) to DiAuto.
 * Everything runs on one thread so hub callbacks keep the order the hub expects.
 */
object Bridge {
    const val TAG = "DiAutoBridge"
    private const val DIAUTO_PACKAGE = "com.drivehub.diauto.mg4"
    /** Stock's hub calls startSession right after the device connects; this covers a hub that doesn't. */
    private const val START_FALLBACK_MS = 3_000L
    private const val ACTION_SCREEN_ON = "com.saicmotor.screen.on"

    val handler: Handler by lazy { Handler(HandlerThread("diauto-bridge").apply { start() }.looper) }

    private val session = BridgeSession()
    private var deviceCallback: IBinder? = null
    private var sessionCallback: IBinder? = null
    private var diAuto: Messenger? = null
    private lateinit var appContext: Context

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        handler.post {
            MediaFeed.bind(appContext)
            NavigationFocus.connect(appContext)
        }
    }

    fun activeDevice(): HubDevice? = session.activeDevice()
    fun isSessionActive(): Boolean = session.isSessionStarted()

    /** The hub's startSession. */
    fun startSession() = handler.post { dispatch(session.startRequested()) }

    /** The phone's Android Auto navigation focus (gain on route start, loss at the end). */
    fun navigationFocus(gain: Boolean) = handler.post { NavigationFocus.set(gain && session.activeDevice() != null) }

    /** The hub registers device and session callbacks in two separate calls, one null each. */
    fun hubRegistered(device: IBinder?, sessionCb: IBinder?) = handler.post {
        val gained = (device != null && device != deviceCallback) || (sessionCb != null && sessionCb != sessionCallback)
        device?.let { deviceCallback = it }
        sessionCb?.let { sessionCallback = it }
        Log.i(TAG, "hub registered device=${device != null} session=${sessionCb != null}")
        if (gained && deviceCallback != null && sessionCallback != null) dispatch(session.replay())
    }

    fun hubUnregistered(device: IBinder?, sessionCb: IBinder?) = handler.post {
        if (device != null && device == deviceCallback) deviceCallback = null
        if (sessionCb != null && sessionCb == sessionCallback) sessionCallback = null
    }

    fun diAutoLinked(messenger: Messenger) {
        diAuto = messenger
        Log.i(TAG, "DiAuto linked")
        try {
            messenger.binder.linkToDeath({ handler.post { diAutoGone(messenger) } }, 0)
        } catch (e: Exception) {
            diAutoGone(messenger)
        }
    }

    /** DiAuto died without saying goodbye: never leave the car believing a session is live. */
    private fun diAutoGone(messenger: Messenger) {
        if (diAuto != messenger) return
        Log.w(TAG, "DiAuto link lost; closing any session")
        diAuto = null
        phoneGone()
    }

    fun phoneConnected(phone: Phone) {
        dispatch(session.phoneConnected(phone))
        MediaFeed.session(active = session.activeDevice() != null)
        handler.postDelayed({
            val events = session.startRequested()
            if (events.isNotEmpty()) Log.w(TAG, "hub never called startSession; starting the session anyway")
            dispatch(events)
        }, START_FALLBACK_MS)
    }

    fun phoneGone() {
        dispatch(session.phoneGone())
        MediaFeed.session(active = false)
        NavigationFocus.set(false)
    }

    fun navigation(status: Int) = dispatch(session.navigation(status))
    fun screen(visible: Boolean) = dispatch(session.screen(visible))
    fun callState(state: Int) = dispatch(session.callState(state))
    fun playback(state: Int) = dispatch(session.playback(state))

    fun command(command: Command) = handler.post {
        val target = diAuto
        if (target == null) {
            if (command == Command.ShowProjection) launchDiAuto()
            return@post
        }
        val message = when (command) {
            Command.ShowProjection -> Message.obtain(null, LinkProtocol.CMD_SHOW)
            Command.HideProjection -> Message.obtain(null, LinkProtocol.CMD_HIDE)
            is Command.Key -> Message.obtain(null, LinkProtocol.CMD_KEY, command.keyCode, 0)
        }
        try {
            target.send(message)
        } catch (e: Exception) {
            Log.w(TAG, "DiAuto command failed: $e")
        }
    }

    private fun launchDiAuto() {
        val intent = appContext.packageManager.getLaunchIntentForPackage(DIAUTO_PACKAGE) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "cannot open DiAuto: $e")
        }
    }

    private fun dispatch(events: List<HubEvent>) {
        for (event in events) {
            Log.i(TAG, "hub <- $event")
            when (event) {
                is HubEvent.DeviceConnected -> call(deviceCallback, HubProtocol.DEVICE_CALLBACK, HubProtocol.ON_DEVICE_CONNECTED) { writeDevice(it, event.device) }
                is HubEvent.DeviceDisconnected -> call(deviceCallback, HubProtocol.DEVICE_CALLBACK, HubProtocol.ON_DEVICE_DISCONNECTED) { writeDevice(it, event.device) }
                is HubEvent.SessionStarted -> call(sessionCallback, HubProtocol.SESSION_CALLBACK, HubProtocol.ON_SESSION_STARTED) {
                    writeDevice(it, event.device)
                    it.writeInt(if (event.launchedInProjection) 1 else 0)
                }
                is HubEvent.SessionClosed -> call(sessionCallback, HubProtocol.SESSION_CALLBACK, HubProtocol.ON_SESSION_CLOSED) { writeDevice(it, event.device) }
                HubEvent.ScreenStarted -> call(sessionCallback, HubProtocol.SESSION_CALLBACK, HubProtocol.ON_SCREEN_STARTED) {}
                HubEvent.ScreenStopped -> call(sessionCallback, HubProtocol.SESSION_CALLBACK, HubProtocol.ON_SCREEN_STOPPED) {}
                is HubEvent.PhoneState -> {
                    // Stock wakes a dimmed screen on every phone status update.
                    try { appContext.sendBroadcast(Intent(ACTION_SCREEN_ON)) } catch (e: Exception) { Log.w(TAG, "screen on: $e") }
                    call(sessionCallback, HubProtocol.SESSION_CALLBACK, HubProtocol.ON_PHONE_STATE_UPDATE) { it.writeInt(event.state) }
                }
                is HubEvent.Playback -> call(sessionCallback, HubProtocol.SESSION_CALLBACK, HubProtocol.ON_PLAYBACK_UPDATE) { it.writeInt(event.state) }
                is HubEvent.Navigation -> call(sessionCallback, HubProtocol.SESSION_CALLBACK, HubProtocol.ON_AAP_NAVIGATION_STATUS) {
                    it.writeString(event.message)
                    it.writeInt(0)
                    it.writeInt(0) // no icon
                }
            }
        }
    }

    private fun writeDevice(parcel: Parcel, device: HubDevice) {
        parcel.writeInt(1)
        device.writeTo(parcel)
    }

    private fun call(target: IBinder?, descriptor: String, code: Int, write: (Parcel) -> Unit) {
        if (target == null) return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(descriptor)
            write(data)
            target.transact(code, data, reply, 0)
            reply.readException()
        } catch (e: Exception) {
            // The hub dying drops its callbacks; it re-registers when it rebinds.
            Log.w(TAG, "hub callback $code failed: $e")
            if (!target.isBinderAlive) {
                if (target == deviceCallback) deviceCallback = null
                if (target == sessionCallback) sessionCallback = null
            }
        } finally {
            data.recycle()
            reply.recycle()
        }
    }
}

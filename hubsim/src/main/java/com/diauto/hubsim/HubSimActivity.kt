package com.diauto.hubsim

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.Parcel
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Plays the MG4 AllGo hub (com.allgo.rui) and DiAuto against the bridge, the way the car does, and logs
 * PASS/FAIL per step under tag HUBSIM. Run: am start -n com.diauto.hubsim/.HubSimActivity
 */
class HubSimActivity : Activity() {

    private val events = LinkedBlockingQueue<String>()
    private val thread = HandlerThread("hubsim").apply { start() }
    private var failures = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread { runCatching { scenario() }.onFailure { fail("crashed: $it") }; finish() }.start()
    }

    private fun scenario() {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { events.add("broadcast:screen.on") }
        }, IntentFilter("com.saicmotor.screen.on"))

        val hub = bind(Intent("com.allgo.app.androidauto.AndroidAutoMainService").setPackage(BRIDGE)) ?: return fail("hub service bind")
        val link = bind(Intent().setComponent(ComponentName(BRIDGE, "$BRIDGE.DiAutoLinkService"))) ?: return fail("link service bind")
        val diAuto = Messenger(link)
        val replies = Messenger(Handler(thread.looper) { msg -> events.add("cmd:${msg.what}:${msg.arg1}"); true })

        hubCall(hub, REGISTER) { it.writeStrongBinder(callback("dev")); it.writeStrongBinder(null) }
        hubCall(hub, REGISTER) { it.writeStrongBinder(null); it.writeStrongBinder(callback("ses")) }
        diAuto.send(Message.obtain(null, MSG_HELLO).apply { replyTo = replies })
        drain()

        log("1. phone connects: the hub hears connected, not started")
        diAuto.send(Message.obtain(null, MSG_PHONE_CONNECTED).apply { data = Bundle().apply { putString("name", "Pixel"); putString("btMac", "AA:BB:CC:DD:EE:FF") } })
        expect("dev:1")
        expectNone("ses:1", 800)
        check(!hubBool(hub, IS_SESSION_ACTIVE), "session not active before startSession")

        log("2. hub startSession: started, launch status 0 off screen")
        hubCall(hub, START_SESSION) {}
        expect("ses:1:launch=0")
        check(hubBool(hub, IS_SESSION_ACTIVE), "session active after startSession")

        log("3. screen, call (wakes the display), playback, navigation focus")
        diAuto.send(Message.obtain(null, MSG_SCREEN, 1, 0)); expect("ses:8")
        diAuto.send(Message.obtain(null, MSG_CALL_STATE, 2, 0)); expectAll("broadcast:screen.on", "ses:5")
        diAuto.send(Message.obtain(null, MSG_PLAYBACK, 1, 0)); expect("ses:6")
        diAuto.send(Message.obtain(null, MSG_NAV_FOCUS, 1, 0)); expectNone("ses:", 500)

        log("4. hub keys: assistant close toggles, media next, pause goes native")
        hubCall(hub, LAUNCH_GOOGLE_ASSISTANT) { it.writeInt(0) }; expect("cmd:103:84")
        hubCall(hub, SEND_MEDIA_PLAY_CONTROL) { it.writeInt(7) }; expect("cmd:103:87")
        hubCall(hub, PAUSE_SESSION) {}; expect("cmd:102:0")

        log("5. phone gone: closed then disconnected")
        diAuto.send(Message.obtain(null, MSG_PHONE_GONE)); expectAll("ses:9", "ses:2", "dev:2")

        log("6. a hub that never calls startSession still gets the session after the fallback")
        diAuto.send(Message.obtain(null, MSG_PHONE_CONNECTED).apply { data = Bundle().apply { putString("name", "Pixel"); putString("btMac", "AA:BB:CC:DD:EE:FF") } })
        expect("dev:1")
        expect("ses:1:launch=0", 5_000)
        diAuto.send(Message.obtain(null, MSG_PHONE_GONE)); drain(1_000)

        log(if (failures == 0) "RESULT PASS" else "RESULT FAIL ($failures)")
    }

    private fun callback(name: String): IBinder = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) return super.onTransact(code, data, reply, flags)
            val launch = if (name == "ses" && code == 1) ":launch=" + data.run { setDataPosition(dataSize() - 4); readInt() } else ""
            events.add("$name:$code$launch")
            reply?.writeNoException()
            return true
        }
    }

    private fun hubCall(hub: IBinder, code: Int, write: (Parcel) -> Unit): Parcel {
        val data = Parcel.obtain(); val reply = Parcel.obtain()
        data.writeInterfaceToken(MAIN_SERVICE); write(data)
        hub.transact(code, data, reply, 0)
        reply.readException()
        data.recycle()
        return reply
    }

    private fun hubBool(hub: IBinder, code: Int): Boolean = hubCall(hub, code) {}.let { r -> (r.readInt() != 0).also { r.recycle() } }

    private fun bind(intent: Intent): IBinder? {
        var binder: IBinder? = null
        val latch = CountDownLatch(1)
        bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(n: ComponentName, b: IBinder) { binder = b; latch.countDown() }
            override fun onServiceDisconnected(n: ComponentName) {}
        }, Context.BIND_AUTO_CREATE)
        latch.await(5, TimeUnit.SECONDS)
        return binder
    }

    private fun expect(event: String, timeoutMs: Long = 1_500) = expectAll(event, timeoutMs = timeoutMs)

    private fun expectAll(vararg wanted: String, timeoutMs: Long = 1_500) {
        val missing = wanted.toMutableList()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (missing.isNotEmpty()) {
            val left = deadline - System.currentTimeMillis()
            val event = if (left > 0) events.poll(left, TimeUnit.MILLISECONDS) else null ?: break
            if (event == null) break
            if (missing.firstOrNull() == event) missing.removeAt(0) else if (event in missing) {
                fail("$event arrived out of order (waiting for ${missing.first()})"); missing.remove(event)
            } else log("   (also: $event)")
        }
        if (missing.isEmpty()) log("   PASS ${wanted.joinToString()}") else fail("missing ${missing.joinToString()}")
    }

    private fun expectNone(prefix: String, windowMs: Long) {
        val deadline = System.currentTimeMillis() + windowMs
        while (true) {
            val left = deadline - System.currentTimeMillis()
            if (left <= 0) break
            val event = events.poll(left, TimeUnit.MILLISECONDS) ?: break
            if (event.startsWith(prefix)) return fail("unexpected $event")
        }
        log("   PASS nothing starting $prefix")
    }

    private fun drain(ms: Long = 500) { Thread.sleep(ms); events.clear() }
    private fun check(ok: Boolean, what: String) = if (ok) log("   PASS $what") else fail(what)
    private fun fail(what: String) { failures++; Log.e(TAG, "   FAIL $what") }
    private fun log(line: String) { Log.i(TAG, line) }

    private companion object {
        const val TAG = "HUBSIM"
        const val BRIDGE = "com.allgo.app.androidauto"
        const val MAIN_SERVICE = "com.allgo.app.androidauto.IAndroidAutoMainService"
        const val REGISTER = 1; const val START_SESSION = 2; const val PAUSE_SESSION = 4; const val IS_SESSION_ACTIVE = 7
        const val LAUNCH_GOOGLE_ASSISTANT = 10; const val SEND_MEDIA_PLAY_CONTROL = 25
        const val MSG_HELLO = 1; const val MSG_PHONE_CONNECTED = 2; const val MSG_PHONE_GONE = 3; const val MSG_SCREEN = 4
        const val MSG_CALL_STATE = 5; const val MSG_PLAYBACK = 6; const val MSG_NAV_FOCUS = 10
    }
}

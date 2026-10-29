package com.allgo.app.androidauto

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import java.lang.reflect.Proxy

/**
 * The car's NAVIGATION app focus, held while the phone's Android Auto has navigation focus, as stock
 * AllGo does (requestAppFocus(1) on the AA navigation focus request). The native map yields the
 * cluster and HUD to the owner. android.car is on the head unit's boot classpath.
 */
object NavigationFocus {
    private const val APP_FOCUS_TYPE_NAVIGATION = 1
    private const val CALLBACK_CLASS = "android.car.CarAppFocusManager\$OnAppFocusOwnershipCallback"

    private var car: Any? = null
    private var manager: Any? = null
    private var callback: Any? = null
    private var wanted = false
    private var held = false

    /** Call on the bridge thread. */
    fun connect(context: Context) {
        if (car != null) return
        try {
            val carClass = Class.forName("android.car.Car")
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    Bridge.handler.post {
                        manager = try { carClass.getMethod("getCarManager", String::class.java).invoke(car, "app_focus") } catch (e: Exception) { null }
                        if (wanted) set(true)
                    }
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                    Bridge.handler.post { manager = null; held = false }
                }
            }
            car = carClass.getMethod("createCar", Context::class.java, ServiceConnection::class.java).invoke(null, context, connection)
            carClass.getMethod("connect").invoke(car)
        } catch (e: Exception) {
            Log.w(Bridge.TAG, "no car app-focus service: ${e.cause ?: e}")
            car = null
        }
    }

    /** Call on the bridge thread. */
    fun set(want: Boolean) {
        wanted = want
        val focus = manager ?: return
        if (want == held) return
        try {
            val callbackType = Class.forName(CALLBACK_CLASS)
            val cb = callback ?: Proxy.newProxyInstance(callbackType.classLoader, arrayOf(callbackType)) { proxy, method, args ->
                when (method.name) {
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.getOrNull(0)
                    "toString" -> "NavigationFocus.callback"
                    else -> { Log.i(Bridge.TAG, "navigation focus ${method.name}(${args?.joinToString()})"); null }
                }
            }.also { callback = it }
            if (want) {
                val result = focus.javaClass.getMethod("requestAppFocus", Int::class.javaPrimitiveType, callbackType).invoke(focus, APP_FOCUS_TYPE_NAVIGATION, cb)
                held = result == 1
                Log.i(Bridge.TAG, "navigation focus requested: $result (1 = granted)")
            } else {
                focus.javaClass.getMethod("abandonAppFocus", callbackType, Int::class.javaPrimitiveType).invoke(focus, cb, APP_FOCUS_TYPE_NAVIGATION)
                held = false
                Log.i(Bridge.TAG, "navigation focus released")
            }
        } catch (e: Exception) {
            Log.w(Bridge.TAG, "navigation focus failed: ${e.cause ?: e}")
        }
    }
}

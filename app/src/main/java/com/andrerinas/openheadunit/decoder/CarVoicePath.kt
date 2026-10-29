package com.andrerinas.openheadunit.decoder

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import com.andrerinas.openheadunit.utils.AppLog
import java.lang.reflect.Proxy

/**
 * Hands the MG4's cabin mic to Android Auto the way MG's own Android Auto (AllGo) does when the
 * phone opens the microphone for the assistant:
 *
 * 1. Voice-command app focus (CarAppFocusManager type 2). SAIC's VrSpeechService listens for it and
 *    stops its always-on wake-word listener, which otherwise holds the mic and leaves us silence.
 * 2. Transient audio focus with the assistant usage.
 * 3. The HAL speech path, `SSE_AA_VR_Wifi` / `SSE_AA_VR`.
 *
 * android.car is on this head unit's boot classpath (AllGo ships only stubs), so it is reached by
 * reflection rather than bundled.
 */
object CarVoicePath {
    private const val APP_FOCUS_TYPE_VOICE_COMMAND = 2
    private const val USAGE_ASSISTANT = 16
    /** How long SAIC's wake-word engine gets to let go of the mic before we open it. */
    private const val MIC_RELEASE_WAIT_MS = 400L

    private var enabledKey: String? = null
    private var car: Any? = null
    @Volatile private var focusManager: Any? = null
    private var focusCallback: Any? = null
    private var audioFocus: AudioFocusRequest? = null

    fun parameter(wireless: Boolean, enabled: Boolean) =
        "${if (wireless) "SSE_AA_VR_Wifi" else "SSE_AA_VR"}=$enabled"

    /** Connects to the car service ahead of time so focus can be taken the moment the mic opens. */
    @Synchronized
    fun prepare(context: Context) {
        if (car != null) return
        try {
            val carClass = Class.forName("android.car.Car")
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    try {
                        focusManager = carClass.getMethod("getCarManager", String::class.java).invoke(car, "app_focus")
                        AppLog.i("CarVoicePath: car app-focus service connected")
                    } catch (e: Exception) {
                        AppLog.w("CarVoicePath: no app-focus manager: ${e.cause ?: e}")
                    }
                }
                override fun onServiceDisconnected(name: ComponentName?) { focusManager = null }
            }
            car = carClass.getMethod("createCar", Context::class.java, ServiceConnection::class.java)
                .invoke(null, context.applicationContext, connection)
            carClass.getMethod("connect").invoke(car)
        } catch (e: Exception) {
            AppLog.w("CarVoicePath: android.car unavailable, voice focus skipped: ${e.cause ?: e}")
            car = null
        }
    }

    @Synchronized
    fun enable(context: Context, wireless: Boolean) {
        if (enabledKey != null) return
        logSaicVoiceState(context)
        prepare(context)
        requestVoiceFocus()
        requestAudioFocus(context)
        val param = parameter(wireless, enabled = true)
        try {
            (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).setParameters(param)
            AppLog.i("CarVoicePath: set $param")
        } catch (e: Exception) {
            AppLog.w("CarVoicePath: could not set $param: ${e.message}")
        }
        enabledKey = param.substringBefore("=")
        SystemClock.sleep(MIC_RELEASE_WAIT_MS)
    }

    @Synchronized
    fun disable(context: Context) {
        val key = enabledKey ?: return
        enabledKey = null
        try {
            (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).setParameters("$key=false")
            AppLog.i("CarVoicePath: set $key=false")
        } catch (e: Exception) {
            AppLog.w("CarVoicePath: could not clear $key: ${e.message}")
        }
        audioFocus?.let { request ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(request)
            }
        }
        audioFocus = null
        abandonVoiceFocus()
    }

    /** Whether SAIC's own wake-word engine is even running (and so holding the mic). */
    private fun logSaicVoiceState(context: Context) {
        val resolver = context.contentResolver
        val wake = android.provider.Settings.System.getString(resolver, "SAICMOTOR_VOICE_WAKE_SWITCH")
        val consent = android.provider.Settings.System.getString(resolver, "VR_CONSENT_CONFIG")
        AppLog.i("CarVoicePath: SAIC wake word switch=${wake ?: "unset (on)"} consent=${consent ?: "unset"}")
    }

    private fun requestVoiceFocus() {
        val manager = focusManager ?: return AppLog.w("CarVoicePath: app-focus service not connected yet")
        try {
            val callbackType = Class.forName("android.car.CarAppFocusManager\$OnAppFocusOwnershipCallback")
            val callback = focusCallback ?: newFocusCallback(callbackType).also { focusCallback = it }
            val result = manager.javaClass.getMethod("requestAppFocus", Int::class.javaPrimitiveType, callbackType)
                .invoke(manager, APP_FOCUS_TYPE_VOICE_COMMAND, callback)
            AppLog.i("CarVoicePath: voice-command focus requested, result=$result (1 = granted)")
        } catch (e: Exception) {
            AppLog.w("CarVoicePath: voice-command focus failed: ${e.cause ?: e}")
        }
    }

    /** A logging callback that also answers Object's methods: the car service hashes it. */
    fun newFocusCallback(callbackType: Class<*>): Any =
        Proxy.newProxyInstance(callbackType.classLoader, arrayOf(callbackType)) { proxy, method, args ->
            when (method.name) {
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.getOrNull(0)
                "toString" -> "CarVoicePath.focusCallback"
                else -> {
                    AppLog.i("CarVoicePath: ${method.name}(${args?.joinToString()})")
                    null
                }
            }
        }

    private fun abandonVoiceFocus() {
        val manager = focusManager ?: return
        val callback = focusCallback ?: return
        try {
            val callbackType = Class.forName("android.car.CarAppFocusManager\$OnAppFocusOwnershipCallback")
            manager.javaClass.getMethod("abandonAppFocus", callbackType, Int::class.javaPrimitiveType)
                .invoke(manager, callback, APP_FOCUS_TYPE_VOICE_COMMAND)
            AppLog.i("CarVoicePath: voice-command focus released")
        } catch (e: Exception) {
            AppLog.w("CarVoicePath: releasing voice-command focus failed: ${e.cause ?: e}")
        }
    }

    private fun requestAudioFocus(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(USAGE_ASSISTANT).build())
            .setOnAudioFocusChangeListener { }
            .build()
        val result = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).requestAudioFocus(request)
        AppLog.i("CarVoicePath: assistant audio focus result=$result (1 = granted)")
        audioFocus = request
    }
}

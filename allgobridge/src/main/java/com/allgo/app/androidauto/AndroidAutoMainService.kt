package com.allgo.app.androidauto

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import com.allgo.app.androidauto.HubCommands.Command

/** The service the AllGo hub binds by name. It must never throw back into the hub. */
class AndroidAutoMainService : Service() {

    override fun onCreate() {
        super.onCreate()
        Bridge.init(this)
    }

    override fun onBind(intent: Intent?): IBinder = HubBinder()

    /** The hub starts this with AAP_DEVICE_CONNECTED for a USB phone; DiAuto owns USB itself. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(Bridge.TAG, "ignoring start ${intent?.action}")
        return START_NOT_STICKY
    }

    private class HubBinder : Binder() {

        override fun getInterfaceDescriptor(): String = HubProtocol.MAIN_SERVICE

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) {
                reply?.writeString(HubProtocol.MAIN_SERVICE)
                return true
            }
            if (code !in FIRST_CALL_TRANSACTION..HubProtocol.SEND_MEDIA_PLAY_CONTROL) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(HubProtocol.MAIN_SERVICE)
            val result = try {
                handle(code, data)
            } catch (e: Exception) {
                Log.w(Bridge.TAG, "hub call $code failed: $e")
                Reply.Int(-1)
            }
            reply ?: return true
            reply.writeNoException()
            when (result) {
                Reply.None -> Unit
                is Reply.Int -> reply.writeInt(result.value)
                is Reply.Bool -> reply.writeInt(if (result.value) 1 else 0)
                is Reply.Text -> reply.writeString(result.value)
                is Reply.Device -> if (result.device == null) reply.writeInt(0) else {
                    reply.writeInt(1)
                    result.device.writeTo(reply)
                }
            }
            return true
        }

        private fun handle(code: Int, data: Parcel): Reply {
            Log.d(Bridge.TAG, "hub -> $code")
            return when (code) {
                HubProtocol.REGISTER -> {
                    Bridge.hubRegistered(data.readStrongBinder(), data.readStrongBinder())
                    Reply.None
                }
                HubProtocol.UNREGISTER -> {
                    Bridge.hubUnregistered(data.readStrongBinder(), data.readStrongBinder())
                    Reply.None
                }
                HubProtocol.START_SESSION -> {
                    Bridge.startSession()
                    Reply.Int(0)
                }
                HubProtocol.STOP_SESSION -> Reply.Int(0)
                HubProtocol.RESUME_SESSION -> {
                    Bridge.command(Command.ShowProjection)
                    Reply.Int(0)
                }
                HubProtocol.PAUSE_SESSION -> {
                    Bridge.command(Command.HideProjection)
                    Reply.Int(0)
                }
                HubProtocol.GET_ACTIVE_DEVICE -> Reply.Device(Bridge.activeDevice())
                HubProtocol.IS_SESSION_ACTIVE -> Reply.Bool(Bridge.isSessionActive())
                HubProtocol.LAUNCH_GOOGLE_ASSISTANT -> {
                    Bridge.command(HubCommands.assistant(start = data.readInt() != 0))
                    Reply.Int(0)
                }
                HubProtocol.LAUNCH_APP -> {
                    HubCommands.launchApp(data.readInt()).forEach { Bridge.command(it) }
                    Reply.Int(0)
                }
                HubProtocol.GET_PLUGIN_VERSION -> Reply.Text("DiAuto bridge")
                HubProtocol.GET_VR_STATUS -> Reply.Bool(false)
                HubProtocol.SEND_MEDIA_PLAY_CONTROL -> {
                    HubCommands.mediaKey(data.readInt())?.let { Bridge.command(it) }
                    Reply.Int(0)
                }
                in HubProtocol.INT_REPLY_IGNORED -> Reply.Int(0)
                in HubProtocol.VOID_IGNORED -> Reply.None
                else -> Reply.None
            }
        }
    }

    private sealed class Reply {
        object None : Reply()
        data class Int(val value: kotlin.Int) : Reply()
        data class Bool(val value: Boolean) : Reply()
        data class Text(val value: String) : Reply()
        data class Device(val device: HubDevice?) : Reply()
    }
}

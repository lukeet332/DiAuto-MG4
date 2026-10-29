package com.allgo.app.androidauto

import android.os.Parcel

/** The phone as the AllGo hub sees it: its `MirroringDevice` parcel. */
data class HubDevice(
    val sessionStatus: Int,
    val name: String,
    val serialId: String,
    val btMac: String,
    val type: Int = TYPE_ANDROID_AUTO,
    // The hub's lookup only matches Android Auto devices on USB, keyed by serial.
    val transport: Int = TRANSPORT_USB,
) {
    /** Field order of `com.allgo.mirroring.deviceservice.MirroringDevice(Parcel)`. */
    fun writeTo(parcel: Parcel) {
        parcel.writeInt(sessionStatus)
        parcel.writeString(name)
        parcel.writeString(serialId)
        parcel.writeString(btMac)
        parcel.writeInt(type)
        parcel.writeInt(transport)
    }

    companion object {
        const val TYPE_ANDROID_AUTO = 2
        const val TRANSPORT_USB = 1
        const val SESSION_NOT_STARTED = 0
        const val SESSION_STARTED = 2
        const val UNKNOWN_MAC = "00:00:00:00:00:00"
        const val DEFAULT_NAME = "Android Auto"
    }
}

data class Phone(val name: String?, val btMac: String?)

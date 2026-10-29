package com.allgo.app.androidauto

import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HubDeviceParcelTest {

    @Test
    fun `writes the field order the hub's MirroringDevice reads`() {
        val device = HubDevice(
            sessionStatus = HubDevice.SESSION_STARTED,
            name = "Pixel",
            serialId = "DIAUTO-AABBCCDDEEFF",
            btMac = "AA:BB:CC:DD:EE:FF",
        )
        val parcel = Parcel.obtain()
        try {
            device.writeTo(parcel)
            parcel.setDataPosition(0)

            assertEquals(HubDevice.SESSION_STARTED, parcel.readInt())
            assertEquals("Pixel", parcel.readString())
            assertEquals("DIAUTO-AABBCCDDEEFF", parcel.readString())
            assertEquals("AA:BB:CC:DD:EE:FF", parcel.readString())
            assertEquals(HubDevice.TYPE_ANDROID_AUTO, parcel.readInt())
            assertEquals(HubDevice.TRANSPORT_USB, parcel.readInt())
            assertEquals(0, parcel.dataAvail())
        } finally {
            parcel.recycle()
        }
    }
}

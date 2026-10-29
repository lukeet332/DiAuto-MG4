package com.andrerinas.openheadunit.aap

import com.andrerinas.openheadunit.aap.UsbTargetPolicy.UsbCandidate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbTargetPolicyTest {

    private val tboxModem = UsbCandidate(vendorId = 0x05C6, productId = 0x902D, inAccessoryMode = false, allowed = false)
    private val phone = UsbCandidate(vendorId = 0x04E8, productId = 0x6860, inAccessoryMode = false, allowed = false)
    private val allowedPhone = phone.copy(allowed = true)
    private val googlePhone = UsbCandidate(vendorId = 0x18D1, productId = 0x4EE1, inAccessoryMode = false, allowed = false)
    private val accessory = UsbCandidate(vendorId = 0x18D1, productId = 0x2D00, inAccessoryMode = true, allowed = false)

    @Test
    fun `the car's T-Box modem is not a phone`() {
        assertTrue(UsbTargetPolicy.isCarModem(0x05C6, 0x902D))
        assertFalse(UsbTargetPolicy.isCarModem(0x05C6, 0x9025))
        assertFalse(UsbTargetPolicy.isCarModem(0x18D1, 0x902D))
    }

    @Test
    fun `nothing attached is no target`() {
        assertFalse(hasTarget(emptyList(), force = true, usbAutoStart = true, singleUsb = true, lastSession = true))
    }

    @Test
    fun `the T-Box modem alone is no target under any setting`() {
        // 29 Oct 16:13: its re-enumeration preempted a working wireless projection for 16 s.
        assertFalse(hasTarget(listOf(tboxModem), force = true, usbAutoStart = true, singleUsb = true, lastSession = true))
    }

    @Test
    fun `a device in accessory mode is always a target`() {
        assertTrue(hasTarget(listOf(accessory)))
    }

    @Test
    fun `a known device is a target in last-session mode only`() {
        assertTrue(hasTarget(listOf(allowedPhone), lastSession = true))
        assertFalse(hasTarget(listOf(allowedPhone)))
    }

    @Test
    fun `USB auto-start targets a single device and ignores the modem when counting`() {
        assertTrue(hasTarget(listOf(phone), usbAutoStart = true))
        assertTrue(hasTarget(listOf(phone, tboxModem), usbAutoStart = true))
        assertFalse(hasTarget(listOf(phone, phone.copy(productId = 1)), usbAutoStart = true))
    }

    @Test
    fun `single-USB mode counts only allowed devices once any are allowed`() {
        assertTrue(hasTarget(listOf(phone), singleUsb = true))
        assertTrue(hasTarget(listOf(allowedPhone, googlePhone), singleUsb = true, anyAllowed = true))
        assertFalse(hasTarget(listOf(phone), singleUsb = true, anyAllowed = true))
    }

    @Test
    fun `a forced check falls back to a single Google device`() {
        assertTrue(hasTarget(listOf(googlePhone), force = true))
        assertFalse(hasTarget(listOf(googlePhone)))
        assertFalse(hasTarget(listOf(phone), force = true))
    }

    private fun hasTarget(
        devices: List<UsbCandidate>,
        lastSession: Boolean = false,
        usbAutoStart: Boolean = false,
        singleUsb: Boolean = false,
        anyAllowed: Boolean = false,
        force: Boolean = false
    ) = UsbTargetPolicy.hasTarget(devices, lastSession, usbAutoStart, singleUsb, anyAllowed, force)
}

package com.andrerinas.openheadunit.aap

/**
 * Whether the attached USB devices include one [AapService.checkAlreadyConnectedUsb] would act on.
 * Asked before a live wireless session is preempted for USB: the MG4's own T-Box modem
 * re-enumerating looked like a phone and cost a working projection 16 s (29 Oct 2026).
 * Mirrors the selection order in checkAlreadyConnectedUsb; keep the two in step.
 */
object UsbTargetPolicy {

    data class UsbCandidate(val vendorId: Int, val productId: Int, val inAccessoryMode: Boolean, val allowed: Boolean)

    private const val GOOGLE_VID = 0x18D1

    /** The MG4's internal Qualcomm MDM9628 modem (the `tbox0` interface); it exposes MTP. */
    fun isCarModem(vendorId: Int, productId: Int): Boolean = vendorId == 0x05C6 && productId == 0x902D

    fun hasTarget(
        devices: List<UsbCandidate>,
        lastSession: Boolean,
        usbAutoStart: Boolean,
        singleUsb: Boolean,
        anyAllowed: Boolean,
        force: Boolean
    ): Boolean {
        val phones = devices.filter { !isCarModem(it.vendorId, it.productId) }
        if (phones.any { it.inAccessoryMode }) return true
        if (lastSession && phones.any { it.allowed }) return true
        val normal = phones.filter { !it.inAccessoryMode }
        if (usbAutoStart && normal.size == 1) return true
        if (singleUsb && (if (anyAllowed) normal.filter { it.allowed } else normal).size == 1) return true
        return force && normal.count { it.vendorId == GOOGLE_VID } == 1
    }
}

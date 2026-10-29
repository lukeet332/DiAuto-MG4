package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MG4, 2026-10-19 evening: with music over Wi-Fi DiAuto paused the car radio at every connect and
 * every 8 s, while the MG4 Radio app (which plays no audio, it drives the car tuner) kept telling
 * the radio to play. The radio stopped every few seconds.
 */
class CarRadioPassthroughPolicyTest {

    @Test
    fun `the MG4 Radio app is recognised as the car's own radio`() {
        assertEquals(true, CarRadioPassthroughPolicy.isCarRadioApp("MG4 Radio"))
        assertEquals(true, CarRadioPassthroughPolicy.isCarRadioApp("mg4 radio"))
        assertEquals(false, CarRadioPassthroughPolicy.isCarRadioApp("YouTube Music"))
        assertEquals(false, CarRadioPassthroughPolicy.isCarRadioApp(null))
    }

    @Test
    fun `the car's music focus is held while the phone wants it, except for the car radio app`() {
        // MG4, 2026-10-20: stock AllGo holds one music focus for as long as the phone asks for it.
        // The MG4 Radio app plays no audio (it drives the car tuner), so the radio keeps the car audio.
        assertEquals(true, CarRadioPassthroughPolicy.holdsCarMediaFocus(phoneWantsMusicFocus = true, mediaSource = "YouTube"))
        assertEquals(true, CarRadioPassthroughPolicy.holdsCarMediaFocus(phoneWantsMusicFocus = true, mediaSource = null))
        assertEquals(false, CarRadioPassthroughPolicy.holdsCarMediaFocus(phoneWantsMusicFocus = true, mediaSource = "MG4 Radio"))
        assertEquals(false, CarRadioPassthroughPolicy.holdsCarMediaFocus(phoneWantsMusicFocus = false, mediaSource = "YouTube"))
    }

    @Test
    fun `the wheel's OK and left-right go to Android Auto only while its music is what the car plays`() {
        // MG4, 2026-10-20: the car pauses its own radio on OK (SystemUI key 3) and skips stations on
        // left/right (keys 4/5); Android Auto music is its source only while DiAuto holds the car music focus.
        assertEquals(85, CarRadioPassthroughPolicy.wheelKeyForAndroidAuto(301, carMusicFocusHeld = true, swcMode = 0, callActive = false))
        assertEquals(88, CarRadioPassthroughPolicy.wheelKeyForAndroidAuto(299, carMusicFocusHeld = true, swcMode = 0, callActive = false))
        assertEquals(87, CarRadioPassthroughPolicy.wheelKeyForAndroidAuto(300, carMusicFocusHeld = true, swcMode = null, callActive = false))
        assertEquals(null, CarRadioPassthroughPolicy.wheelKeyForAndroidAuto(301, carMusicFocusHeld = false, swcMode = 0, callActive = false))
        assertEquals(null, CarRadioPassthroughPolicy.wheelKeyForAndroidAuto(300, carMusicFocusHeld = false, swcMode = 0, callActive = false))
    }

    @Test
    fun `the wheel stays with the car in cluster or climate mode, during calls, and for volume`() {
        // SystemUI maps the right cluster per the SWC mode (0 head unit, 1 cluster, 2 climate); in a call
        // OK answers or hangs up through the car's phone service.
        assertEquals(null, CarRadioPassthroughPolicy.wheelKeyForAndroidAuto(301, carMusicFocusHeld = true, swcMode = 1, callActive = false))
        assertEquals(null, CarRadioPassthroughPolicy.wheelKeyForAndroidAuto(299, carMusicFocusHeld = true, swcMode = 2, callActive = false))
        assertEquals(null, CarRadioPassthroughPolicy.wheelKeyForAndroidAuto(301, carMusicFocusHeld = true, swcMode = 0, callActive = true))
        assertEquals(null, CarRadioPassthroughPolicy.wheelKeyForAndroidAuto(297, carMusicFocusHeld = true, swcMode = 0, callActive = false))
        assertEquals(null, CarRadioPassthroughPolicy.wheelKeyForAndroidAuto(298, carMusicFocusHeld = true, swcMode = 0, callActive = false))
    }

    @Test
    fun `leaving the MG4 Radio app does not take the car audio until the phone asks again`() {
        // MG4, 2026-10-20 11:20: MG4 Radio closed, Android Auto fell back to a paused YouTube, and the
        // old music request silenced the playing radio.
        assertEquals(false, CarRadioPassthroughPolicy.phoneWantsMusicAfterSourceChange(wanted = true, previousSource = "MG4 Radio"))
        assertEquals(true, CarRadioPassthroughPolicy.phoneWantsMusicAfterSourceChange(wanted = true, previousSource = "YouTube Music"))
        assertEquals(false, CarRadioPassthroughPolicy.phoneWantsMusicAfterSourceChange(wanted = false, previousSource = "YouTube Music"))
    }
}

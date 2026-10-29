package com.andrerinas.openheadunit.decoder

import com.andrerinas.openheadunit.aap.protocol.Channel

object AudioStopPolicy {
    /** Music stops at once on pause; speech and navigation play out what they already have. */
    fun discardBufferedOnStop(channelId: Int): Boolean = channelId == Channel.ID_AUD

    /** Android Auto's MediaPlaybackStatus state: 1 stopped, 2 playing, 3 paused. */
    fun discardOnPlaybackStatus(state: Int): Boolean = state == 1 || state == 3

    fun resumeOnPlaybackStatus(state: Int): Boolean = state == 2
}

package com.andrerinas.openheadunit.decoder

import org.junit.Assert.assertEquals
import org.junit.Test

class DecoderLatencyPolicyTest {
    @Test
    fun `MediaTek decoders get no-reorder mode so a frame is shown without waiting for the next`() {
        // MG4: on a still screen (20 fps) p95 arrival-to-display was ~100 ms; MTK's ACodec maps
        // enableDecNoReorder=1 onto OMX.MTK.index.param.video.DecNoReorderMode.
        assertEquals(mapOf("enableDecNoReorder" to 1), DecoderLatencyPolicy.extraIntegerKeys("OMX.MTK.VIDEO.DECODER.AVC"))
        assertEquals(mapOf("enableDecNoReorder" to 1), DecoderLatencyPolicy.extraIntegerKeys("OMX.MTK.VIDEO.DECODER.HEVC"))
    }

    @Test
    fun `other decoders are left alone`() {
        assertEquals(emptyMap<String, Int>(), DecoderLatencyPolicy.extraIntegerKeys("c2.android.avc.decoder"))
        assertEquals(emptyMap<String, Int>(), DecoderLatencyPolicy.extraIntegerKeys("OMX.qcom.video.decoder.avc"))
    }
}

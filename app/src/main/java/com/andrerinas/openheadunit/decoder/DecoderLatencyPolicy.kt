package com.andrerinas.openheadunit.decoder

/**
 * Vendor format keys that cut decoder latency, per codec. MediaTek's ACodec maps
 * `enableDecNoReorder=1` onto OMX.MTK.index.param.video.DecNoReorderMode, which outputs each
 * frame in decode order instead of holding it for the next; Android Auto streams carry no
 * B-frames, so nothing needs reordering. Unknown keys are ignored by other codecs.
 */
object DecoderLatencyPolicy {
    fun extraIntegerKeys(codecName: String): Map<String, Int> =
        if (codecName.startsWith("OMX.MTK.VIDEO.DECODER")) mapOf("enableDecNoReorder" to 1) else emptyMap()
}

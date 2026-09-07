package org.sublaky.player.playback

import androidx.media3.common.C
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import org.sublaky.player.domain.SpectrumAnalyzer
import kotlin.math.abs

/** In-process read-only visualizer feed; contains no recorded audio or microphone input. */
object AudioSpectrum {
    val observers = AtomicInteger(0)
    @Volatile var levels: FloatArray = FloatArray(32)
        private set
    fun publish(values: FloatArray) { levels = values }
    fun clear() { levels = FloatArray(32) }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SpectrumSink : TeeAudioProcessor.AudioBufferSink {
    private val analyzer = SpectrumAnalyzer()
    private val samples = FloatArray(2048)
    private var count = 0
    private var rate = 44100
    private var channels = 2
    private var encoding = C.ENCODING_PCM_16BIT
    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        rate = sampleRateHz; channels = channelCount; this.encoding = encoding
        count = 0; AudioSpectrum.clear()
    }
    override fun handleBuffer(buffer: ByteBuffer) {
        if (AudioSpectrum.observers.get() == 0 || channels <= 0 || rate <= 0) { count = 0; return }
        val bytes = when (encoding) { C.ENCODING_PCM_16BIT -> 2; C.ENCODING_PCM_FLOAT -> 4; else -> return }
        // TeeAudioProcessor forwards the original bytes unchanged to the speaker.
        val copy = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        while (copy.remaining() >= bytes * channels) {
            var sample = 0f
            repeat(channels) {
                val value = if (bytes == 2) copy.short / 32768f else copy.float
                // Retain the strongest channel instead of cancelling opposite-phase stereo.
                if (value.isFinite() && abs(value) > abs(sample)) sample = value
            }
            samples[count++] = sample
            if (count == samples.size) {
                AudioSpectrum.publish(analyzer.analyze(samples, rate))
                count = 0
            }
        }
    }
}

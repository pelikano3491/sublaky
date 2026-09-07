package org.sublaky.player.domain

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import org.sublaky.player.playback.AudioSpectrum
import org.sublaky.player.playback.SpectrumSink
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SpectrumSinkTest {
    @Before fun setup() { AudioSpectrum.observers.set(1); AudioSpectrum.clear() }
    @After fun cleanup() { AudioSpectrum.observers.set(0); AudioSpectrum.clear() }
    private fun pcm(): ByteBuffer = ByteBuffer.allocateDirect(2048 * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
        repeat(2048) { i -> val s = (sin(2 * PI * 1000 * i / 48000) * 16000).toInt().toShort(); putShort(s); putShort((-s).toShort()) }
        flip()
    }
    @Test fun oppositePhaseStereoStillHasSignal() {
        val sink = SpectrumSink(); sink.flush(48000, 2, C.ENCODING_PCM_16BIT)
        val buffer = pcm(); sink.handleBuffer(buffer)
        assertTrue(AudioSpectrum.levels.max() > .8f)
        assertEquals(0, buffer.position())
    }
    @Test fun teeKeepsEveryAudioByteUnchanged() {
        val tee = TeeAudioProcessor(SpectrumSink())
        tee.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT)); tee.flush()
        val input = pcm(); val expected = ByteArray(input.remaining()); input.duplicate().get(expected)
        tee.queueInput(input)
        val output = tee.output; val actual = ByteArray(output.remaining()); output.get(actual)
        assertArrayEquals(expected, actual)
        assertTrue(AudioSpectrum.levels.max() > .8f)
    }
    @Test fun doesNotAnalyzeWithoutVisibleVisualizer() {
        AudioSpectrum.observers.set(0)
        val sink = SpectrumSink(); sink.flush(48000, 2, C.ENCODING_PCM_16BIT); sink.handleBuffer(pcm())
        assertTrue(AudioSpectrum.levels.all { it == 0f })
    }
    @Test fun flushClearsOldSong() {
        val sink = SpectrumSink(); sink.flush(48000, 2, C.ENCODING_PCM_16BIT); sink.handleBuffer(pcm())
        sink.flush(44100, 2, C.ENCODING_PCM_16BIT)
        assertTrue(AudioSpectrum.levels.all { it == 0f })
    }
    @Test fun floatPcmIsSupported() {
        val sink = SpectrumSink(); sink.flush(48000, 1, C.ENCODING_PCM_FLOAT)
        val buffer = ByteBuffer.allocateDirect(2048 * 4).order(ByteOrder.LITTLE_ENDIAN)
        repeat(2048) { buffer.putFloat((sin(2 * PI * 1000 * it / 48000) * .5).toFloat()) }; buffer.flip()
        sink.handleBuffer(buffer)
        assertTrue(AudioSpectrum.levels.max() > .8f)
    }
}

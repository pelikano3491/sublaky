package org.sublaky.player.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpectrumAnalyzerTest {
    private fun tone(hz: Double, amplitude: Double = .5, rate: Int = 48000) = FloatArray(2048) { (amplitude * sin(2 * PI * hz * it / rate)).toFloat() }
    @Test fun silenceHasNoBars() { assertTrue(SpectrumAnalyzer().analyze(FloatArray(2048), 48000).all { it == 0f }) }
    @Test fun toneAppearsInItsFrequencyBand() {
        val result = SpectrumAnalyzer().analyze(tone(1000.0), 48000)
        val peak = result.indices.maxBy { result[it] }
        val expected = (ln(1000.0 / 60) / ln(16000.0 / 60) * 32).toInt()
        assertTrue(abs(peak - expected) <= 1)
        assertTrue(result[peak] > .8f)
    }
    @Test fun highAndLowNotesHaveDifferentPeaks() {
        val low = SpectrumAnalyzer().analyze(tone(220.0), 48000)
        val high = SpectrumAnalyzer().analyze(tone(8000.0), 48000)
        assertTrue(high.indices.maxBy { high[it] } - low.indices.maxBy { low[it] } > 15)
    }
    @Test fun lowerVolumeHasLowerBars() {
        val loud = SpectrumAnalyzer().analyze(tone(1000.0, .5), 48000).max()
        val quiet = SpectrumAnalyzer().analyze(tone(1000.0, .05), 48000).max()
        assertTrue(loud - quiet > .2f)
    }
    @Test fun sampleRateDoesNotMoveTheNote() {
        val a = SpectrumAnalyzer().analyze(tone(2000.0, rate = 44100), 44100)
        val b = SpectrumAnalyzer().analyze(tone(2000.0), 48000)
        assertTrue(abs(a.indices.maxBy { a[it] } - b.indices.maxBy { b[it] }) <= 1)
    }
    @Test fun doesNotModifyAudioSamples() {
        val samples = tone(440.0); val original = samples.copyOf()
        SpectrumAnalyzer().analyze(samples, 48000)
        assertArrayEquals(original, samples, 0f)
    }
}

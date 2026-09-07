package org.sublaky.player.domain

import kotlin.math.*

/** Hann-windowed FFT. Input samples are never modified; output is logarithmic dBFS bands. */
class SpectrumAnalyzer(private val size: Int = 2048, private val bands: Int = 32) {
    init { require(size > 1 && size and (size - 1) == 0); require(bands > 0) }
    fun analyze(samples: FloatArray, sampleRate: Int): FloatArray {
        require(samples.size == size && sampleRate > 0)
        val real = DoubleArray(size) { i -> samples[i].toDouble() * (.5 - .5 * cos(2 * PI * i / (size - 1))) }
        val imag = DoubleArray(size)
        var j = 0
        for (i in 1 until size) {
            var bit = size shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { val temp = real[i]; real[i] = real[j]; real[j] = temp }
        }
        var length = 2
        while (length <= size) {
            val phase = -2 * PI / length
            val stepReal = cos(phase); val stepImag = sin(phase)
            for (start in 0 until size step length) {
                var wr = 1.0; var wi = 0.0
                for (offset in 0 until length / 2) {
                    val a = start + offset; val b = a + length / 2
                    val tr = real[b] * wr - imag[b] * wi
                    val ti = real[b] * wi + imag[b] * wr
                    real[b] = real[a] - tr; imag[b] = imag[a] - ti
                    real[a] += tr; imag[a] += ti
                    val next = wr * stepReal - wi * stepImag
                    wi = wr * stepImag + wi * stepReal; wr = next
                }
            }
            length *= 2
        }
        val high = min(16000.0, sampleRate / 2.0)
        val low = min(60.0, high / 2)
        return FloatArray(bands) { band ->
            val begin = max(1, floor(low * (high / low).pow(band.toDouble() / bands) * size / sampleRate).toInt())
            val end = max(begin + 1, ceil(low * (high / low).pow((band + 1.0) / bands) * size / sampleRate).toInt()).coerceAtMost(size / 2)
            var magnitude = 0.0
            for (bin in begin until end) magnitude = max(magnitude, hypot(real[bin], imag[bin]) * 4 / size)
            ((20 * log10(max(magnitude, 1e-8)) + 72) / 72).toFloat().coerceIn(0f, 1f)
        }
    }
}

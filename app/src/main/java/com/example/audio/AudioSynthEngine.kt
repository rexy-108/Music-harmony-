package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioSynthEngine {
    private var synthJob: Job? = null
    private val sampleRate = 22050
    @Volatile
    private var isPlaying = false

    fun startPlaying(pattern: String, coroutineScope: CoroutineScope) {
        stopPlaying()
        isPlaying = true

        synthJob = coroutineScope.launch(Dispatchers.Default) {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            var localTrack: AudioTrack? = null
            try {
                localTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize.coerceAtLeast(4096))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                localTrack.play()
            } catch (e: Exception) {
                Log.e("AudioSynthEngine", "Failed to initialize AudioTrack", e)
                return@launch
            }

            try {
                var step = 0
                // Choose musical scales for each pattern (pentatonic & harmonic intervals for melodic richness)
                val scale = when (pattern) {
                    "lullaby" -> doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25) // Pentatonic C Root (C4-C5)
                    "synthwave" -> doubleArrayOf(110.0, 130.81, 146.83, 164.81, 196.00, 220.00) // Bass A-minor scale
                    "ambient" -> doubleArrayOf(293.66, 349.23, 440.00, 523.25, 587.33, 698.46) // Dreamy D-minor 9
                    "ballad_chords" -> doubleArrayOf(261.63, 329.63, 392.00, 493.88, 523.25) // C Major 7 notes
                    else -> doubleArrayOf(261.63, 293.66, 329.63, 349.23, 392.00, 440.00) // Standard Pop C Major
                }

                val bufferSize = 1024
                val buffer = ShortArray(bufferSize)

                while (isPlaying) {
                    val tempoTicks = when (pattern) {
                        "synthwave" -> 6      // energetic tempo
                        "lullaby" -> 20      // slow bell sounds
                        "ambient" -> 36      // very long pads
                        "ballad_chords" -> 12 // steady slow melody
                        else -> 9            // pop groove
                    }

                    val tickIndex = (step / tempoTicks) % scale.size
                    val currentFreq = scale[tickIndex]

                    for (i in 0 until bufferSize) {
                        val sampleIndexInSession = step * bufferSize + i
                        val t = sampleIndexInSession.toDouble() / sampleRate
                        
                        // Modulate based on selected theme
                        val angle = 2.0 * Math.PI * currentFreq * t
                        val sample = when (pattern) {
                            "synthwave" -> {
                                // Retro pulse wave with sharp, aggressive filter decay
                                val wave = if (sin(angle) > 0.0) 0.15 else -0.15
                                val lfo = 0.7 + 0.3 * sin(2.0 * Math.PI * 4.0 * t) // 4Hz Tremolo
                                wave * lfo * 0.4
                            }
                            "ambient" -> {
                                // Layered lush detuned waves
                                val primary = sin(angle)
                                val third = sin(angle * 1.5 + 0.5)
                                val envelope = 0.5 * (1.0 + sin(2.0 * Math.PI * 0.15 * t)) // slow wave swell
                                (primary + third * 0.4) * 0.18 * envelope
                            }
                            "lullaby" -> {
                                // Sweet, warm chime sound with physical key-decay envelope
                                val tickSampleCount = tempoTicks * bufferSize
                                val progressInTick = (sampleIndexInSession % tickSampleCount).toDouble() / tickSampleCount
                                val envelope = Math.max(0.0, 1.0 - progressInTick) // linear volume drop
                                sin(angle) * 0.3 * envelope
                            }
                            "ballad_chords" -> {
                                // Warm piano-like layered chords
                                val base = sin(angle)
                                val third = sin(angle * 1.25) // Major 3rd
                                val fifth = sin(angle * 1.50) // Perfect 5th
                                val melodySwell = 0.5 * (0.8 + 0.2 * sin(Math.PI * t))
                                (base + third * 0.5 + fifth * 0.3) * 0.18 * melodySwell
                            }
                            else -> {
                                // Pop custom groove: alternates pitch and synthesizes a basic low kick drum
                                val beatQuarter = (step / 3) % 4
                                val isKickQuarter = (beatQuarter == 0 || beatQuarter == 2)
                                val kick = if (isKickQuarter) {
                                    0.35 * sin(2.0 * Math.PI * 65.0 * t) * Math.max(0.0, 1.0 - (sampleIndexInSession % (3 * bufferSize)).toDouble() / (3 * bufferSize))
                                } else {
                                    0.0
                                }
                                val melody = sin(angle) * 0.12
                                (melody + kick) * 0.5
                            }
                        }

                        buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }

                    val writeResult = localTrack.write(buffer, 0, bufferSize)
                    if (writeResult == AudioTrack.ERROR_INVALID_OPERATION || writeResult == AudioTrack.ERROR_BAD_VALUE) {
                        break
                    }
                    step++
                    
                    // Allow other coroutines to run
                    delay(12)
                }
            } catch (e: Exception) {
                Log.e("AudioSynthEngine", "Error in synthesis loop", e)
            } finally {
                try {
                    localTrack.stop()
                    localTrack.release()
                } catch (e: Exception) {
                    // Ignore track errors during active state transitions
                }
            }
        }
    }

    fun stopPlaying() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
    }
}

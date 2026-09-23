package com.clinicalscribe.app.asr

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlin.math.sqrt

/**
 * Captures mono 16kHz PCM audio into memory for a single dictation utterance
 * and hands back normalized float samples ([-1, 1]), which is what
 * sherpa-onnx expects. Utterances are short (seconds), so buffering the
 * whole thing in memory instead of streaming keeps this simple and matches
 * how the offline (non-streaming) recognizer wants its input anyway.
 */
class AudioRecorder(private val onLevel: (rms: Float) -> Unit = {}) {

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val MAX_DURATION_SECONDS = 120 // safety cap against a stuck recording
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    @Volatile
    private var recording = false

    private val chunks = mutableListOf<ShortArray>()
    private var totalSamples = 0

    /** Caller must have already checked/requested RECORD_AUDIO before calling this. */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        ).let { if (it > 0) it else SAMPLE_RATE / 2 }

        chunks.clear()
        totalSamples = 0

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2,
        )
        audioRecord = record
        record.startRecording()
        recording = true

        recordingThread = Thread {
            val buffer = ShortArray(minBufferSize)
            val maxSamples = SAMPLE_RATE * MAX_DURATION_SECONDS
            while (recording && totalSamples < maxSamples) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val chunk = buffer.copyOf(read)
                    synchronized(chunks) {
                        chunks.add(chunk)
                        totalSamples += read
                    }
                    onLevel(rms(chunk))
                }
            }
        }.also { it.start() }
    }

    /** Stops recording and returns everything captured, as float samples in [-1, 1]. */
    fun stop(): FloatArray {
        recording = false
        recordingThread?.join(500)
        recordingThread = null

        audioRecord?.let {
            runCatching { it.stop() }
            it.release()
        }
        audioRecord = null

        val result = FloatArray(totalSamples)
        var offset = 0
        synchronized(chunks) {
            for (chunk in chunks) {
                for (s in chunk) {
                    result[offset++] = s / 32768.0f
                }
            }
            chunks.clear()
        }
        return result
    }

    /** Stops and discards whatever was captured (e.g. user backed out mid-recording). */
    fun cancel() {
        recording = false
        recordingThread?.join(500)
        recordingThread = null
        audioRecord?.let {
            runCatching { it.stop() }
            it.release()
        }
        audioRecord = null
        synchronized(chunks) { chunks.clear() }
        totalSamples = 0
    }

    private fun rms(chunk: ShortArray): Float {
        if (chunk.isEmpty()) return 0f
        var sum = 0.0
        for (s in chunk) sum += s.toDouble() * s.toDouble()
        return (sqrt(sum / chunk.size) / 32768.0).toFloat()
    }
}

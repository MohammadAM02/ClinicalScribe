package com.clinicalscribe.app.asr

import android.content.Context
import android.util.Log
import com.clinicalscribe.app.data.VocabularyEntry
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig

private const val TAG = "SherpaAsrEngine"

/**
 * Thin, app-specific wrapper around sherpa-onnx's OfflineRecognizer for the
 * bundled Parakeet TDT model. Owns the (fairly expensive) native recognizer
 * instance and rebuilds it only when the vocabulary list or the "Enhanced
 * Recognition" setting actually changes, since construction loads the ~650MB
 * of ONNX weights.
 */
class SherpaAsrEngine(private val context: Context) {

    @Volatile
    private var recognizer: OfflineRecognizer? = null
    private var builtWithHotwordsSignature: String? = null
    private var builtWithEnhanced: Boolean? = null
    private val lock = Any()

    /**
     * Ensures a recognizer matching the current vocabulary + settings exists,
     * (re)building it if needed. Safe to call before every transcription --
     * it's a no-op when nothing changed. Must be called off the main thread;
     * building the recognizer can take a few seconds.
     */
    fun ensureReady(vocabulary: List<VocabularyEntry>, enhancedRecognition: Boolean) {
        synchronized(lock) {
            val signature = vocabulary.joinToString("|") { "${it.word}:${it.boost}" }
            if (recognizer != null && signature == builtWithHotwordsSignature && enhancedRecognition == builtWithEnhanced) {
                return
            }

            if (!ModelFiles.isModelReady(context)) {
                throw IllegalStateException("Model not downloaded yet")
            }

            recognizer?.release()
            recognizer = null

            var hotwordsFile: java.io.File? = null
            if (enhancedRecognition && vocabulary.isNotEmpty()) {
                HotwordsFileBuilder.ensureBpeVocab(context)
                hotwordsFile = HotwordsFileBuilder.writeHotwordsFile(context, vocabulary)
            }

            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    transducer = OfflineTransducerModelConfig(
                        encoder = ModelFiles.encoder(context).absolutePath,
                        decoder = ModelFiles.decoder(context).absolutePath,
                        joiner = ModelFiles.joiner(context).absolutePath,
                    ),
                    tokens = ModelFiles.tokens(context).absolutePath,
                    modelType = "nemo_transducer",
                    numThreads = 2,
                    modelingUnit = if (hotwordsFile != null) "bpe" else "",
                    bpeVocab = if (hotwordsFile != null) ModelFiles.bpeVocab(context).absolutePath else "",
                ),
                decodingMethod = if (enhancedRecognition) "modified_beam_search" else "greedy_search",
                hotwordsFile = hotwordsFile?.absolutePath ?: "",
                hotwordsScore = 3.0f,
            )

            Log.i(TAG, "Building recognizer (enhanced=$enhancedRecognition, hotwords=${vocabulary.size})")
            recognizer = OfflineRecognizer(config = config)
            builtWithHotwordsSignature = signature
            builtWithEnhanced = enhancedRecognition
        }
    }

    /** Blocking; call from a background thread/coroutine. */
    fun transcribe(samples: FloatArray, sampleRate: Int = 16000): String {
        val r = recognizer ?: throw IllegalStateException("Recognizer not ready -- call ensureReady() first")
        val stream = r.createStream()
        try {
            stream.acceptWaveform(samples, sampleRate)
            r.decode(stream)
            return r.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }

    fun release() {
        synchronized(lock) {
            recognizer?.release()
            recognizer = null
            builtWithHotwordsSignature = null
            builtWithEnhanced = null
        }
    }
}

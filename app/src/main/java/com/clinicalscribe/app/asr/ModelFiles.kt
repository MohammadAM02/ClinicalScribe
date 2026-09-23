package com.clinicalscribe.app.asr

import android.content.Context
import java.io.File

/**
 * The on-device ASR model: same Parakeet TDT 0.6B v3 (int8) model Chirp uses
 * on desktop, via sherpa-onnx. Downloaded on first run rather than bundled in
 * the APK (it's ~650MB).
 */
object ModelFiles {
    const val MODEL_DIR_NAME = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8"
    private const val HF_BASE =
        "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8/resolve/main"

    /** filename -> download URL, in download order (small files first so progress feels quick to start). */
    val DOWNLOAD_FILES: List<Pair<String, String>> = listOf(
        "tokens.txt" to "$HF_BASE/tokens.txt",
        "joiner.int8.onnx" to "$HF_BASE/joiner.int8.onnx",
        "decoder.int8.onnx" to "$HF_BASE/decoder.int8.onnx",
        "encoder.int8.onnx" to "$HF_BASE/encoder.int8.onnx",
    )

    fun modelDir(context: Context): File = File(context.filesDir, "models/$MODEL_DIR_NAME")

    fun encoder(context: Context) = File(modelDir(context), "encoder.int8.onnx")
    fun decoder(context: Context) = File(modelDir(context), "decoder.int8.onnx")
    fun joiner(context: Context) = File(modelDir(context), "joiner.int8.onnx")
    fun tokens(context: Context) = File(modelDir(context), "tokens.txt")

    /** Generated on-device (the HF release doesn't ship one) -- see HotwordsFileBuilder. */
    fun bpeVocab(context: Context) = File(modelDir(context), "bpe.vocab")

    fun hotwordsFile(context: Context) = File(context.filesDir, "models/hotwords.txt")

    fun isModelReady(context: Context): Boolean =
        encoder(context).length() > 0 && decoder(context).length() > 0 &&
            joiner(context).length() > 0 && tokens(context).length() > 0
}

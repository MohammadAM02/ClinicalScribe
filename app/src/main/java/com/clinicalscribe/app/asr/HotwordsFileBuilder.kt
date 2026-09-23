package com.clinicalscribe.app.asr

import android.content.Context
import com.clinicalscribe.app.data.VocabularyEntry
import java.io.File

/**
 * Two small pieces of prep sherpa-onnx needs for vocabulary boosting to work
 * with this NeMo Parakeet model:
 *
 * 1. A bpe.vocab file. The HF release for this model doesn't ship one, so we
 *    derive an equivalent from tokens.txt on first use -- the same
 *    workaround the sherpa-onnx project itself documents for this model
 *    family: giving every token an equal score makes hotword encoding behave
 *    as longest-match.
 * 2. A hotwords file: one "word :score" line per vocabulary entry, same
 *    format Chirp's desktop hotwords generation uses.
 */
object HotwordsFileBuilder {

    fun ensureBpeVocab(context: Context) {
        val bpeVocab = ModelFiles.bpeVocab(context)
        if (bpeVocab.exists() && bpeVocab.length() > 0) return

        val tokensFile = ModelFiles.tokens(context)
        if (!tokensFile.exists()) return

        val sb = StringBuilder()
        tokensFile.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val token = line.trim().substringBefore(' ')
            if (token.isNotEmpty()) {
                sb.append(token).append("\t-1.0\n")
            }
        }
        bpeVocab.parentFile?.mkdirs()
        bpeVocab.writeText(sb.toString())
    }

    /** Rewrites the hotwords file from the current vocabulary list. Returns the file, or null if empty. */
    fun writeHotwordsFile(context: Context, vocabulary: List<VocabularyEntry>): File? {
        val file = ModelFiles.hotwordsFile(context)
        if (vocabulary.isEmpty()) {
            file.delete()
            return null
        }
        file.parentFile?.mkdirs()
        file.bufferedWriter().use { writer ->
            for (entry in vocabulary) {
                if (entry.word.isBlank()) continue
                writer.write("${entry.word} :${entry.boost}")
                writer.newLine()
            }
        }
        return file
    }
}

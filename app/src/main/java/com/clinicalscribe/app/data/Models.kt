package com.clinicalscribe.app.data

/**
 * Field names deliberately match Chirp's (the Windows/Mac desktop dictation
 * app) vocabulary.json / snippets.json / dictionary.json exactly, so a file
 * exported there can be imported here without any conversion.
 */
data class VocabularyEntry(
    val word: String,
    val boost: Float = 3.0f,
)

data class SnippetEntry(
    val trigger: String,
    val expansion: String,
)

data class DictionaryEntry(
    val from: String,
    val to: String,
)

package com.clinicalscribe.app.data

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Applies the dictionary (plain find/replace) and snippets (trigger phrase ->
 * full expansion) passes to a raw transcript, in that order -- mirroring the
 * pipeline Chirp's Rust backend uses (dictionary::apply_dictionary() then
 * snippets::apply_snippets()), including its exact matching rules:
 * case-insensitive, whole-word/phrase boundaries, punctuation stripped from
 * the trigger before matching, longest trigger wins when triggers overlap.
 */
object TextPostProcessor {

    fun apply(text: String, dictionary: List<DictionaryEntry>, snippets: List<SnippetEntry>): String {
        var result = applyDictionary(text, dictionary)
        result = applySnippets(result, snippets)
        return result
    }

    private fun applyDictionary(text: String, entries: List<DictionaryEntry>): String {
        var result = text
        for (entry in entries) {
            if (entry.from.isBlank()) continue
            val pattern = wordBoundaryPattern(entry.from) ?: continue
            result = pattern.matcher(result).replaceAll(Matcher.quoteReplacement(entry.to))
        }
        return result
    }

    private fun applySnippets(text: String, entries: List<SnippetEntry>): String {
        if (entries.isEmpty()) return text
        val sorted = entries.sortedByDescending { it.trigger.length }
        var result = text
        for (entry in sorted) {
            if (entry.trigger.isBlank()) continue
            val pattern = wordBoundaryPattern(entry.trigger) ?: continue
            result = pattern.matcher(result).replaceAll(Matcher.quoteReplacement(entry.expansion))
        }
        return result
    }

    /** Strip punctuation from the trigger (as Chirp does) and build a case-insensitive \b...\b pattern. */
    private fun wordBoundaryPattern(trigger: String): Pattern? {
        val cleaned = trigger.filter { it.isLetterOrDigit() || it.isWhitespace() }
        if (cleaned.isBlank()) return null
        val escaped = Pattern.quote(cleaned)
        return Pattern.compile("\\b$escaped\\b", Pattern.CASE_INSENSITIVE)
    }
}

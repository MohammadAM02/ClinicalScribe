package com.clinicalscribe.app.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/** Where the three editable lists live on disk, inside the app's private storage. */
private fun dataDir(context: Context): File = File(context.filesDir, "data")

fun vocabularyStore(context: Context): JsonListStore<VocabularyEntry> = JsonListStore(
    file = File(dataDir(context), "vocabulary.json"),
    toJson = { JSONObject().put("word", it.word).put("boost", it.boost.toDouble()) },
    fromJson = {
        VocabularyEntry(
            word = it.getString("word"),
            boost = it.optDouble("boost", 3.0).toFloat(),
        )
    },
)

fun snippetStore(context: Context): JsonListStore<SnippetEntry> = JsonListStore(
    file = File(dataDir(context), "snippets.json"),
    toJson = { JSONObject().put("trigger", it.trigger).put("expansion", it.expansion) },
    fromJson = {
        SnippetEntry(
            trigger = it.getString("trigger"),
            expansion = it.getString("expansion"),
        )
    },
)

fun dictionaryStore(context: Context): JsonListStore<DictionaryEntry> = JsonListStore(
    file = File(dataDir(context), "dictionary.json"),
    toJson = { JSONObject().put("from", it.from).put("to", it.to) },
    fromJson = {
        DictionaryEntry(
            from = it.getString("from"),
            to = it.getString("to"),
        )
    },
)

/**
 * Small SharedPreferences wrapper for the handful of app-wide toggles.
 * Field names mirror Chirp's settings.json where there's a direct analog.
 */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("clinical_scribe_settings", Context.MODE_PRIVATE)

    /** Mirrors Chirp's "beamSearch" setting -- must be on for vocabulary hotwords to take effect. */
    var enhancedRecognition: Boolean
        get() = prefs.getBoolean("enhanced_recognition", true)
        set(value) = prefs.edit().putBoolean("enhanced_recognition", value).apply()

    var onboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) = prefs.edit().putBoolean("onboarding_complete", value).apply()
}

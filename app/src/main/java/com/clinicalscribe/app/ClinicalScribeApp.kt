package com.clinicalscribe.app

import android.app.Application
import android.content.Context
import com.clinicalscribe.app.asr.SherpaAsrEngine
import com.clinicalscribe.app.data.AppPreferences
import com.clinicalscribe.app.data.DictionaryEntry
import com.clinicalscribe.app.data.JsonListStore
import com.clinicalscribe.app.data.SnippetEntry
import com.clinicalscribe.app.data.VocabularyEntry
import com.clinicalscribe.app.data.dictionaryStore
import com.clinicalscribe.app.data.snippetStore
import com.clinicalscribe.app.data.vocabularyStore

class ClinicalScribeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}

/**
 * Small manual-DI container shared by the main Activity, the keyboard
 * service and the download service (all part of the same app process).
 * No DI framework: this app is small enough that a plain singleton is
 * clearer than the ceremony a framework would add, and it keeps the build
 * simple since it's not something that's been verified in a full Android
 * Studio build in this environment.
 */
object AppGraph {
    lateinit var vocabulary: JsonListStore<VocabularyEntry>
        private set
    lateinit var snippets: JsonListStore<SnippetEntry>
        private set
    lateinit var dictionary: JsonListStore<DictionaryEntry>
        private set
    lateinit var preferences: AppPreferences
        private set
    lateinit var asrEngine: SherpaAsrEngine
        private set

    private var initialized = false

    /** Idempotent -- safe to call defensively from any entry point (Activity, IME service, ...). */
    fun init(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        vocabulary = vocabularyStore(appContext).also { it.load() }
        snippets = snippetStore(appContext).also { it.load() }
        dictionary = dictionaryStore(appContext).also { it.load() }
        preferences = AppPreferences(appContext)
        asrEngine = SherpaAsrEngine(appContext)
        initialized = true
    }
}

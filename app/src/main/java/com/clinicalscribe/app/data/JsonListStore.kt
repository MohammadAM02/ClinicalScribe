package com.clinicalscribe.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Minimal JSON-array-of-objects persistence for a list of items, kept
 * deliberately dumb (no external JSON library) so it has zero extra
 * dependencies. Used for vocabulary / snippets / dictionary, each of which
 * is just "a list of small objects" on disk -- exactly the shape Chirp
 * (the desktop app this is modeled on) uses for the same files.
 */
class JsonListStore<T>(
    private val file: File,
    private val toJson: (T) -> JSONObject,
    private val fromJson: (JSONObject) -> T,
) {
    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()

    fun load() {
        _items.value = if (file.exists()) {
            runCatching { parse(file.readText()) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }

    fun add(item: T) {
        _items.value = _items.value + item
        persist()
    }

    fun removeAt(index: Int) {
        val current = _items.value
        if (index !in current.indices) return
        _items.value = current.toMutableList().apply { removeAt(index) }
        persist()
    }

    fun replaceAll(newItems: List<T>) {
        _items.value = newItems
        persist()
    }

    /**
     * Merge entries from a Chirp export (or any file in the same format),
     * skipping items that are already present. Returns how many entries the
     * imported file contained (not how many were newly added).
     */
    fun importJson(jsonText: String): Int {
        val incoming = parse(jsonText)
        val existingKeys = _items.value.map { it.toString() }.toSet()
        val newOnes = incoming.filter { it.toString() !in existingKeys }
        _items.value = _items.value + newOnes
        persist()
        return incoming.size
    }

    fun exportJson(): String {
        val arr = JSONArray()
        _items.value.forEach { arr.put(toJson(it)) }
        return arr.toString(2)
    }

    private fun parse(jsonText: String): List<T> {
        if (jsonText.isBlank()) return emptyList()
        val arr = JSONArray(jsonText)
        return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }

    private fun persist() {
        file.parentFile?.mkdirs()
        file.writeText(exportJson())
    }
}

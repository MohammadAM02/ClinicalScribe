package com.clinicalscribe.app.ui

import android.content.Context
import android.provider.Settings
import com.clinicalscribe.app.ime.DictationInputMethodService

/** Whether the user has enabled this app's keyboard in system Settings (not necessarily selected as active). */
fun isImeEnabled(context: Context): Boolean {
    val imeId = "${context.packageName}/${DictationInputMethodService::class.java.name}"
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_INPUT_METHODS)
        ?: return false
    return enabled.split(":").any { it == imeId }
}

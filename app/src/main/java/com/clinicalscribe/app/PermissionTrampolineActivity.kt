package com.clinicalscribe.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * The keyboard (an InputMethodService) can't show a permission dialog
 * itself -- its window isn't allowed to. This transparent, no-history
 * activity is what it launches instead: it asks for RECORD_AUDIO and
 * finishes immediately, whatever the answer. The keyboard checks again the
 * next time the mic button is tapped.
 */
class PermissionTrampolineActivity : ComponentActivity() {

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
}

package com.clinicalscribe.app.ui

import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.clinicalscribe.app.R
import com.clinicalscribe.app.asr.ModelFiles
import com.clinicalscribe.app.download.ModelDownloadService
import com.clinicalscribe.app.download.ModelDownloadState
import com.clinicalscribe.app.download.ModelDownloadStatus

@Composable
fun HomeScreen(
    onOpenVocabulary: () -> Unit,
    onOpenSnippets: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var modelReady by remember { mutableStateOf(ModelFiles.isModelReady(context)) }
    var imeEnabled by remember { mutableStateOf(isImeEnabled(context)) }
    val downloadStatus by ModelDownloadState.status.collectAsState()

    // Re-check status whenever the user comes back to this screen (e.g. after
    // enabling the keyboard in system Settings, or after a download finishes).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                modelReady = ModelFiles.isModelReady(context)
                imeEnabled = isImeEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }
    LaunchedEffect(downloadStatus) {
        if (downloadStatus is ModelDownloadStatus.Done) {
            modelReady = ModelFiles.isModelReady(context)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResText(R.string.home_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SetupCard(
                    icon = Icons.Filled.Mic,
                    title = "Speech model",
                    ready = modelReady,
                    readyLabel = stringResText(R.string.home_model_ready),
                    notReadyLabel = stringResText(R.string.home_model_not_ready),
                ) {
                    when (val status = downloadStatus) {
                        is ModelDownloadStatus.Downloading -> {
                            Column(Modifier.fillMaxWidth()) {
                                Text("Downloading ${status.fileName} (${status.fileIndex}/${status.fileCount})")
                                LinearProgressIndicator(
                                    progress = status.overallProgress,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                )
                            }
                        }
                        is ModelDownloadStatus.Error -> {
                            Column(Modifier.fillMaxWidth()) {
                                Text("Download failed: ${status.message}", color = MaterialTheme.colorScheme.error)
                                Button(
                                    onClick = { ModelDownloadService.start(context) },
                                    modifier = Modifier.heightIn(min = 48.dp).padding(top = 8.dp),
                                ) { Text(stringResText(R.string.action_retry)) }
                            }
                        }
                        else -> {
                            if (!modelReady) {
                                Button(
                                    onClick = { ModelDownloadService.start(context) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text(stringResText(R.string.home_download_model)) }
                            }
                        }
                    }
                }
            }

            item {
                SetupCard(
                    icon = Icons.Filled.Keyboard,
                    title = "Keyboard",
                    ready = imeEnabled,
                    readyLabel = stringResText(R.string.home_keyboard_enabled),
                    notReadyLabel = stringResText(R.string.home_keyboard_not_enabled),
                ) {
                    Column {
                        if (!imeEnabled) {
                            Button(
                                onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                                modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                            ) { Text(stringResText(R.string.home_enable_keyboard)) }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    val imm = context.getSystemService(InputMethodManager::class.java)
                                    imm.showInputMethodPicker()
                                },
                                modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                            ) { Text(stringResText(R.string.home_switch_keyboard)) }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NavTile(Icons.Filled.List, stringResText(R.string.home_vocabulary), Modifier.weight(1f), onOpenVocabulary)
                    NavTile(Icons.Filled.List, stringResText(R.string.home_snippets), Modifier.weight(1f), onOpenSnippets)
                    NavTile(Icons.Filled.Settings, stringResText(R.string.home_settings), Modifier.weight(1f), onOpenSettings)
                }
            }
        }
    }
}

@Composable
private fun SetupCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    ready: Boolean,
    readyLabel: String,
    notReadyLabel: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (ready) readyLabel else notReadyLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(Modifier.padding(top = 12.dp)) { content() }
        }
    }
}

@Composable
private fun NavTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 64.dp),
    ) {
        Column {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun stringResText(id: Int): String = androidx.compose.ui.res.stringResource(id)

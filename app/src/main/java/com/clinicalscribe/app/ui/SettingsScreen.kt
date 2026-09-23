package com.clinicalscribe.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.clinicalscribe.app.AppGraph
import com.clinicalscribe.app.R

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var enhanced by remember { mutableStateOf(AppGraph.preferences.enhancedRecognition) }

    val importVocabLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.use { s -> runCatching { AppGraph.vocabulary.importJson(s.bufferedReader().readText()) } } }
    }
    val importSnippetsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.use { s -> runCatching { AppGraph.snippets.importJson(s.bufferedReader().readText()) } } }
    }
    val importDictionaryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.use { s -> runCatching { AppGraph.dictionary.importJson(s.bufferedReader().readText()) } } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_enhanced_recognition), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.settings_enhanced_recognition_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = enhanced,
                        onCheckedChange = {
                            enhanced = it
                            AppGraph.preferences.enhancedRecognition = it
                        },
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_import_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.settings_import_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.padding(top = 12.dp)) {
                        OutlinedButton(onClick = { importVocabLauncher.launch("*/*") }) { Text("vocabulary.json") }
                    }
                    Row(Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = { importSnippetsLauncher.launch("*/*") }) { Text("snippets.json") }
                    }
                    Row(Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = { importDictionaryLauncher.launch("*/*") }) { Text("dictionary.json") }
                    }
                }
            }

            Text(
                "On-device speech recognition only — audio never leaves this phone. " +
                    "Built independently using the open-source sherpa-onnx project; not affiliated with Chirp.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

package com.clinicalscribe.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clinicalscribe.app.AppGraph
import com.clinicalscribe.app.R
import com.clinicalscribe.app.data.SnippetEntry

@Composable
fun SnippetsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val entries by AppGraph.snippets.items.collectAsState()
    var trigger by remember { mutableStateOf("") }
    var expansion by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val text = stream.bufferedReader().readText()
            runCatching { AppGraph.snippets.importJson(text) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.snippets_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = { importLauncher.launch("*/*") }) {
                        Icon(Icons.Filled.UploadFile, contentDescription = stringResource(R.string.action_import))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text(
                stringResource(R.string.snippets_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(Modifier.padding(top = 16.dp)) {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    placeholder = { Text(stringResource(R.string.snippets_trigger_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = expansion,
                    onValueChange = { expansion = it },
                    placeholder = { Text(stringResource(R.string.snippets_expansion_hint)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    minLines = 2,
                )
                Button(
                    onClick = {
                        val t = trigger.trim()
                        val e = expansion.trim()
                        if (t.isNotEmpty() && e.isNotEmpty()) {
                            AppGraph.snippets.add(SnippetEntry(trigger = t, expansion = e))
                            trigger = ""
                            expansion = ""
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text(stringResource(R.string.action_add)) }
            }

            if (entries.isEmpty()) {
                Text(
                    stringResource(R.string.snippets_empty),
                    modifier = Modifier.padding(top = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                    items(entries.size) { index ->
                        val entry = entries[index]
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("“${entry.trigger}”", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        entry.expansion,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                    )
                                }
                                IconButton(onClick = { AppGraph.snippets.removeAt(index) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

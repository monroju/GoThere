package com.example.gothere.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gothere.viewmodel.NotesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NotesScreen() {
    val vm: NotesViewModel = viewModel()

    // Observe Firestore note as State
    val remote by vm.note.collectAsState(initial = "")

    // Local editable buffer, seeded whenever Firestore value changes.
    // rememberSaveable keeps unsaved edits across rotation/process death.
    var text by rememberSaveable(remote) { mutableStateOf(remote) }

    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // --- Debounced autosave ---
    // If the user stops typing for 800ms AND the buffer differs from Firestore, save it.
    LaunchedEffect(text, remote) {
        if (text != remote) {
            delay(800)
            saving = true
            vm.save(text)
            saving = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Notes", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Your note") },
            modifier = Modifier.padding(top = 12.dp)
        )

        // Manual Save still available (nice for explicit control)
        Button(
            enabled = !saving && text != remote,
            onClick = {
                saving = true
                scope.launch {
                    vm.save(text)
                    saving = false
                }
            },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(if (saving) "Saving…" else "Save")
        }

        if (text != remote && !saving) {
            Text(
                "Unsaved changes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

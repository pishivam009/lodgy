package com.lodgy.app.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.TenantNote
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.icons.CommonIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun NoteType.dotColor(): Color = when (this) {
    NoteType.COMPLAINT -> Color(0xFFE0A020)
    NoteType.DAMAGE -> Color(0xFFC0392B)
    NoteType.GENERAL -> Color(0xFF3F7FBF)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTimelineScreen(
    onBack: () -> Unit,
    onAddNote: () -> Unit,
    onOpenNote: (TenantNote) -> Unit,
    viewModel: NotesTimelineViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes_timeline_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNote) {
                Icon(CommonIcons.Plus, contentDescription = stringResource(R.string.note_add))
            }
        },
    ) { padding ->
        if (uiState.notes.isEmpty()) {
            if (!uiState.loading) {
                Box(modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp)) {
                    Text(
                        stringResource(R.string.notes_timeline_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding),
            ) {
                items(uiState.notes, key = TenantNote::id) { note ->
                    NoteRow(note = note, onClick = { onOpenNote(note) })
                }
            }
        }
    }
}

@Composable
private fun NoteRow(note: TenantNote, onClick: () -> Unit) {
    val dateFormat = remember(note.id) { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(note.type.dotColor()),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(note.type.label(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateFormat.format(Date(note.occurredOn)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(note.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

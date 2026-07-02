package com.example.ventlogger.ui.screens

import android.net.Uri
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.example.ventlogger.data.models.Encounter
import com.example.ventlogger.data.models.MediaAttachment
import com.example.ventlogger.data.models.MediaAttachmentType
import com.example.ventlogger.ui.VentLoggerViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReviewScreen(
    viewModel: VentLoggerViewModel,
    onEditEncounter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val encounters by viewModel.encounters.collectAsState()
    var selectedEncounter by remember { mutableStateOf<Encounter?>(null) }
    var deleteCandidates by remember { mutableStateOf<List<Encounter>>(emptyList()) }
    var selectedDeleteIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(EncounterSortMode.TIMESTAMP) }
    var sortAscending by rememberSaveable { mutableStateOf(false) }
    val sortedEncounters = remember(encounters, sortMode, sortAscending) {
        val sorted = encounters.sortedWith(sortMode.comparator)
        if (sortAscending) sorted else sorted.asReversed()
    }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saved Encounters",
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = { 
                viewModel.startNewEncounter()
                onEditEncounter()
            }) {
                Text("New")
            }
            TextButton(
                onClick = {
                    editMode = !editMode
                    if (!editMode) {
                        selectedDeleteIds = emptySet()
                    }
                }
            ) {
                Text(if (editMode) "Done" else "Edit")
            }
        }
        
        Spacer(Modifier.height(16.dp))

        SortControls(
            selectedMode = sortMode,
            ascending = sortAscending,
            onModeSelected = { mode ->
                if (mode == sortMode) {
                    sortAscending = !sortAscending
                } else {
                    sortMode = mode
                    sortAscending = mode != EncounterSortMode.TIMESTAMP
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        if (editMode) {
            SelectionControls(
                selectedCount = selectedDeleteIds.size,
                totalCount = sortedEncounters.size,
                onSelectAll = { selectedDeleteIds = sortedEncounters.map { it.id }.toSet() },
                onClear = { selectedDeleteIds = emptySet() },
                onDeleteSelected = {
                    deleteCandidates = sortedEncounters.filter { it.id in selectedDeleteIds }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedEncounters) { encounter ->
                EncounterItem(
                    encounter = encounter,
                    editMode = editMode,
                    selectedForDelete = encounter.id in selectedDeleteIds,
                    onSelectionChange = { selected ->
                        selectedDeleteIds = if (selected) {
                            selectedDeleteIds + encounter.id
                        } else {
                            selectedDeleteIds - encounter.id
                        }
                    },
                    onClick = {
                        if (editMode) {
                            selectedDeleteIds = if (encounter.id in selectedDeleteIds) {
                                selectedDeleteIds - encounter.id
                            } else {
                                selectedDeleteIds + encounter.id
                            }
                        } else {
                            selectedEncounter = encounter
                        }
                    },
                    onDelete = { deleteCandidates = listOf(encounter) },
                    onEdit = {
                        viewModel.selectEncounter(encounter)
                        onEditEncounter()
                    }
                )
            }
        }
    }

    if (selectedEncounter != null) {
        val summary = viewModel.generateSummary(selectedEncounter!!)
        AlertDialog(
            onDismissRequest = { selectedEncounter = null },
            title = { Text("Encounter Summary") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    MediaPreviewList(selectedEncounter!!.mediaAttachments)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(summary))
                    Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedEncounter = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (deleteCandidates.isNotEmpty()) {
        ConfirmDeleteDialog(
            encounters = deleteCandidates,
            onDismiss = { deleteCandidates = emptyList() },
            onConfirm = {
                viewModel.deleteEncounters(deleteCandidates)
                selectedDeleteIds = selectedDeleteIds - deleteCandidates.map { it.id }.toSet()
                deleteCandidates = emptyList()
            }
        )
    }
}

@Composable
private fun MediaPreviewList(attachments: List<MediaAttachment>) {
    if (attachments.isEmpty()) {
        return
    }

    Text(
        text = "Media",
        style = MaterialTheme.typography.titleSmall
    )
    attachments.forEachIndexed { index, attachment ->
        when (attachment.type) {
            MediaAttachmentType.PHOTO -> PhotoPreview(attachment, index + 1)
            MediaAttachmentType.VIDEO -> VideoPreview(attachment, index + 1)
        }
    }
}

@Composable
private fun PhotoPreview(attachment: MediaAttachment, index: Int) {
    Text(
        text = "Photo $index",
        style = MaterialTheme.typography.labelMedium
    )
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(Uri.parse(attachment.uri))
            }
        },
        update = { imageView ->
            imageView.setImageURI(Uri.parse(attachment.uri))
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}

@Composable
private fun VideoPreview(attachment: MediaAttachment, index: Int) {
    Text(
        text = "Video $index",
        style = MaterialTheme.typography.labelMedium
    )
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(Uri.parse(attachment.uri))
                setMediaController(MediaController(context).also { it.setAnchorView(this) })
            }
        },
        update = { videoView ->
            videoView.setVideoURI(Uri.parse(attachment.uri))
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    )
}

private enum class EncounterSortMode(
    val label: String,
    val comparator: Comparator<Encounter>
) {
    PATIENT(
        "Patient",
        compareBy<Encounter> { it.patientIdentifier.ifBlank { "\uFFFF" }.lowercase(Locale.getDefault()) }
            .thenBy { it.encounterTime }
    ),
    LOCATION(
        "Location",
        compareBy<Encounter> { it.location.ifBlank { "\uFFFF" }.lowercase(Locale.getDefault()) }
            .thenBy { it.encounterTime }
    ),
    TYPE(
        "Type",
        compareBy<Encounter> { it.interactionType.ifBlank { "\uFFFF" }.lowercase(Locale.getDefault()) }
            .thenBy { it.encounterTime }
    ),
    TIMESTAMP(
        "Time",
        compareBy<Encounter> { it.encounterTime }
    )
}

@Composable
private fun SortControls(
    selectedMode: EncounterSortMode,
    ascending: Boolean,
    onModeSelected: (EncounterSortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Sort by",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EncounterSortMode.entries.forEach { mode ->
                val selected = selectedMode == mode
                val directionMarker = if (selected) {
                    if (ascending) " ↑" else " ↓"
                } else {
                    ""
                }
                FilterChip(
                    selected = selected,
                    onClick = { onModeSelected(mode) },
                    label = { Text("${mode.label}$directionMarker") }
                )
            }
        }
    }
}

@Composable
private fun SelectionControls(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Selection",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onSelectAll,
                enabled = totalCount > 0
            ) {
                Text("Select all")
            }
            OutlinedButton(
                onClick = onClear,
                enabled = selectedCount > 0
            ) {
                Text("Clear")
            }
            Button(
                onClick = onDeleteSelected,
                enabled = selectedCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete $selectedCount")
            }
        }
    }
}

@Composable
fun EncounterItem(
    encounter: Encounter,
    editMode: Boolean,
    selectedForDelete: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(encounter.encounterTime))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editMode) {
                Checkbox(
                    checked = selectedForDelete,
                    onCheckedChange = onSelectionChange
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (encounter.patientIdentifier.isNotBlank()) encounter.patientIdentifier else "No ID",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${encounter.interactionType} - $dateString",
                    style = MaterialTheme.typography.bodySmall
                )
                if (encounter.location.isNotBlank()) {
                    Text(
                        text = "Loc: ${encounter.location}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (editMode) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    encounters: List<Encounter>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val patientNames = encounters
        .map { it.patientIdentifier.ifBlank { "No ID" } }
        .distinct()
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
    val patientSummary = patientNames.joinToString(", ")
    val entryLabel = if (encounters.size == 1) "entry" else "entries"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm permanent delete") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This data will be gone forever.")
                Text("You are deleting ${encounters.size} saved $entryLabel.")
                Text("Patient(s): $patientSummary")
                Text("This cannot be undone.")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete forever")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

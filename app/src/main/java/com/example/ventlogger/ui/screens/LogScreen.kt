package com.example.ventlogger.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ventlogger.data.models.Encounter
import com.example.ventlogger.data.models.MediaAttachment
import com.example.ventlogger.data.models.MediaAttachmentType
import com.example.ventlogger.data.models.RespiratoryGroup
import com.example.ventlogger.ui.VentLoggerViewModel
import com.example.ventlogger.ui.components.RespiratoryGroupPicker
import kotlinx.coroutines.delay

@Composable
fun AddEditScreen(
    viewModel: VentLoggerViewModel,
    onStartLogging: () -> Unit,
    modifier: Modifier = Modifier
) {
    val encounter by viewModel.currentEncounter.collectAsState()
    val encounters by viewModel.encounters.collectAsState()
    val groups by viewModel.respiratoryGroups.collectAsState()
    val patients = remember(encounters, encounter.patientIdentifier) {
        historicalPatients(encounters, encounter.patientIdentifier)
    }

    SetupContent(
        encounter = encounter,
        patients = patients,
        groups = groups,
        onUpdateEncounter = { viewModel.updateEncounter(it) },
        onPatientSelected = { patient ->
            val latestForPatient = latestEncounterForPatient(encounters, patient)
            if (latestForPatient != null) {
                viewModel.selectEncounterForSetup(latestForPatient)
            } else {
                viewModel.selectPatient(patient)
            }
        },
        onAddNewPatient = { viewModel.startNewEncounter() },
        onStartLogging = onStartLogging,
        modifier = modifier
    )
}

@Composable
fun LogScreen(
    viewModel: VentLoggerViewModel,
    modifier: Modifier = Modifier
) {
    val encounter by viewModel.currentEncounter.collectAsState()
    val encounters by viewModel.encounters.collectAsState()
    val groups by viewModel.respiratoryGroups.collectAsState()
    val patients = remember(encounters, encounter.patientIdentifier) {
        historicalPatients(encounters, encounter.patientIdentifier)
    }

    LoggingContent(
        encounter = encounter,
        patients = patients,
        groups = groups,
        onUpdateEncounter = { viewModel.updateEncounter(it) },
        onPatientSelected = { patient ->
            val latestForPatient = latestEncounterForPatient(encounters, patient)
            if (latestForPatient != null) {
                viewModel.startNewLogFromTemplate(latestForPatient)
            } else {
                viewModel.startNewLogForPatient(patient)
            }
        },
        onSave = { viewModel.saveEncounter() },
        modifier = modifier
    )
}

@Composable
fun SetupContent(
    encounter: Encounter,
    patients: List<String>,
    groups: List<RespiratoryGroup>,
    onUpdateEncounter: (Encounter) -> Unit,
    onPatientSelected: (String) -> Unit,
    onAddNewPatient: () -> Unit,
    onStartLogging: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartLogging,
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = { Text("Start Logging") }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Encounter Setup",
                style = MaterialTheme.typography.headlineMedium
            )

            PatientDropdown(
                patients = patients,
                selectedPatient = encounter.patientIdentifier,
                onPatientSelected = onPatientSelected,
                onAddNewPatient = onAddNewPatient,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = onAddNewPatient,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add New Patient")
            }

            OutlinedTextField(
                value = encounter.patientIdentifier,
                onValueChange = { onUpdateEncounter(encounter.copy(patientIdentifier = it)) },
                label = { Text("Patient Identifier") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = encounter.location,
                    onValueChange = { onUpdateEncounter(encounter.copy(location = it)) },
                    label = { Text("Location") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = encounter.interactionType,
                    onValueChange = { onUpdateEncounter(encounter.copy(interactionType = it)) },
                    label = { Text("Type") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            RespiratoryGroupPicker(
                groups = groups,
                selectedIds = encounter.selectedGroupIds.toSet(),
                onSelectionChanged = { selectedIds ->
                    onUpdateEncounter(encounter.copy(selectedGroupIds = selectedIds.toList()))
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun LoggingContent(
    encounter: Encounter,
    patients: List<String>,
    groups: List<RespiratoryGroup>,
    onUpdateEncounter: (Encounter) -> Unit,
    onPatientSelected: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showSavedPopup by remember { mutableStateOf(false) }
    val hasPhotoAttachment = encounter.mediaAttachments.any { it.type == MediaAttachmentType.PHOTO }
    val hasVideoAttachment = encounter.mediaAttachments.any { it.type == MediaAttachmentType.VIDEO }

    LaunchedEffect(showSavedPopup) {
        if (showSavedPopup) {
            delay(3000)
            showSavedPopup = false
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onUpdateEncounter(
                encounter.copy(
                    mediaAttachments = encounter.mediaAttachments + MediaAttachment(
                        uri = it.toString(),
                        type = MediaAttachmentType.PHOTO
                    )
                )
            )
        }
    }
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onUpdateEncounter(
                encounter.copy(
                    mediaAttachments = encounter.mediaAttachments + MediaAttachment(
                        uri = it.toString(),
                        type = MediaAttachmentType.VIDEO
                    )
                )
            )
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    LabeledLogAction(
                        label = "Photo",
                        active = hasPhotoAttachment,
                        onClick = { photoPicker.launch(arrayOf("image/*")) }
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Attach Photo")
                    }
                    LabeledLogAction(
                        label = "Video",
                        active = hasVideoAttachment,
                        onClick = { videoPicker.launch(arrayOf("video/*")) }
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Attach Video")
                    }
                    LabeledLogAction(
                        label = "Save",
                        active = false,
                        onClick = {
                            onSave()
                            showSavedPopup = true
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Changes")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Patient Log",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        PatientLogDropdowns(
                            patients = patients,
                            selectedPatient = encounter.patientIdentifier,
                            onPatientSelected = onPatientSelected
                        )
                        Text(
                            text = "${encounter.interactionType} @ ${encounter.location}".trim(),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (patients.isEmpty()) {
                            Text(
                                text = "No saved patients yet. Use Add/Edit to add one.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (encounter.mediaAttachments.isNotEmpty()) {
                            Text(
                                text = "${encounter.mediaAttachments.size} media attached",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (encounter.selectedGroupIds.isEmpty()) {
                    Text(
                        text = "No categories selected. Use 'Change Setup' to add categories for charting.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }

                encounter.selectedGroupIds.forEach { groupId ->
                    val groupName = findGroupName(groups, groupId) ?: groupId
                    OutlinedTextField(
                        value = encounter.groupedNotes[groupId] ?: "",
                        onValueChange = { note ->
                            val newNotes = encounter.groupedNotes.toMutableMap()
                            newNotes[groupId] = note
                            onUpdateEncounter(encounter.copy(groupedNotes = newNotes))
                        },
                        label = { Text(groupName) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1
                    )
                }
            }

            SavedPopup(
                visible = showSavedPopup,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun LabeledLogAction(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val contentColor = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        IconButton(
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(contentColor = contentColor)
        ) {
            icon()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Composable
private fun SavedPopup(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.padding(top = 12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp
        ) {
            Text(
                text = "Saved",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun PatientDropdown(
    patients: List<String>,
    selectedPatient: String,
    onPatientSelected: (String) -> Unit,
    onAddNewPatient: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (selectedPatient.isBlank()) "Select a Saved Patient" else selectedPatient,
                modifier = Modifier.weight(1f)
            )
            Text("Select")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            DropdownMenuItem(
                text = { Text("Select a Saved Patient") },
                onClick = {
                    expanded = false
                    onAddNewPatient()
                }
            )
            patients.forEach { patient ->
                DropdownMenuItem(
                    text = { Text(patient) },
                    onClick = {
                        expanded = false
                        onPatientSelected(patient)
                    }
                )
            }
        }
    }
}

@Composable
private fun PatientLogDropdowns(
    patients: List<String>,
    selectedPatient: String,
    onPatientSelected: (String) -> Unit
) {
    if (patients.isEmpty()) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Patient",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PatientOnlyDropdown(
            patients = patients,
            selectedPatient = selectedPatient,
            onPatientSelected = onPatientSelected,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PatientOnlyDropdown(
    patients: List<String>,
    selectedPatient: String,
    onPatientSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedPatient.ifBlank { "Select patient" },
                modifier = Modifier.weight(1f)
            )
            Text("Select")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            patients.forEach { patient ->
                DropdownMenuItem(
                    text = { Text(patient) },
                    onClick = {
                        expanded = false
                        onPatientSelected(patient)
                    }
                )
            }
        }
    }
}

private fun historicalPatients(encounters: List<Encounter>, currentPatient: String): List<String> {
    return (encounters.map { it.patientIdentifier } + currentPatient)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
}

private fun latestEncounterForPatient(encounters: List<Encounter>, patient: String): Encounter? {
    return encounters
        .filter { it.patientIdentifier == patient }
        .maxByOrNull { it.updatedAt }
}

private fun findGroupName(groups: List<RespiratoryGroup>, id: String): String? {
    for (group in groups) {
        if (group.id == id) return group.name
        val childName = findGroupName(group.children, id)
        if (childName != null) return childName
    }
    return null
}

package com.example.ventlogger.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ventlogger.AppThemeMode
import com.example.ventlogger.data.models.Encounter
import com.example.ventlogger.data.models.RespiratoryGroup
import com.example.ventlogger.ui.VentLoggerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: VentLoggerViewModel,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val encounters by viewModel.encounters.collectAsState()
    val groups by viewModel.respiratoryGroups.collectAsState()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(buildCsv(encounters, groups).toByteArray())
            }
            Toast.makeText(context, "CSV exported", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bulk Export",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Exports one CSV row per saved interaction. Patient, location, type, and timestamp are included first. Each charting group becomes its own column.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { exportLauncher.launch("ventlogger_export.csv") },
                    enabled = encounters.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export All Patients to CSV")
                }
                Text(
                    text = "${encounters.size} saved interactions ready for export.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun buildCsv(encounters: List<Encounter>, groups: List<RespiratoryGroup>): String {
    val flatGroups = flattenGroups(groups)
    val headers = listOf(
        "patient",
        "location",
        "type",
        "timestamp",
        "created_at",
        "updated_at"
    ) + flatGroups.map { group ->
        group.path.ifEmpty { listOf(group.name) }.joinToString(" > ")
    }
    val rows = encounters.sortedBy { it.encounterTime }.map { encounter ->
        listOf(
            encounter.patientIdentifier,
            encounter.location,
            encounter.interactionType,
            formatTimestamp(encounter.encounterTime),
            formatTimestamp(encounter.createdAt),
            formatTimestamp(encounter.updatedAt)
        ) + flatGroups.map { group ->
            encounter.groupedNotes[group.id].orEmpty()
        }
    }

    return buildString {
        appendLine(headers.joinToString(",") { csvCell(it) })
        rows.forEach { row ->
            appendLine(row.joinToString(",") { csvCell(it) })
        }
    }
}

private fun flattenGroups(groups: List<RespiratoryGroup>): List<RespiratoryGroup> {
    return groups.flatMap { group ->
        listOf(group) + flattenGroups(group.children)
    }
}

private fun formatTimestamp(value: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(value))
}

private fun csvCell(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}

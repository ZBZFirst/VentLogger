package com.example.ventlogger.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ventlogger.data.models.RespiratoryGroup

@Composable
fun RespiratoryGroupPicker(
    groups: List<RespiratoryGroup>,
    selectedIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Select Charting Categories",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        groups.forEach { group ->
            GroupItem(
                group = group,
                selectedIds = selectedIds,
                onSelectionChanged = onSelectionChanged,
                level = 0
            )
        }
    }
}

@Composable
fun GroupItem(
    group: RespiratoryGroup,
    selectedIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    level: Int
) {
    var expanded by remember { mutableStateOf(false) }
    val isSelected = selectedIds.contains(group.id)
    val hasSelectedChild = group.children.any { child ->
        child.hasSelectedDescendantOrSelf(selectedIds)
    }
    val selectionIcon = when {
        isSelected -> Icons.Default.CheckBox
        hasSelectedChild -> Icons.Default.IndeterminateCheckBox
        else -> Icons.Default.CheckBoxOutlineBlank
    }
    val selectionDescription = when {
        isSelected -> "Selected group"
        hasSelectedChild -> "Partially selected group"
        else -> "Not selected"
    }
    val selectionTint = if (isSelected || hasSelectedChild) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val groupIds = group.descendantIdsAndSelf()
                    val nextIds = if (isSelected) {
                        selectedIds - groupIds
                    } else {
                        selectedIds + groupIds
                    }
                    onSelectionChanged(nextIds)
                }
                .padding(start = (level * 16).dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (group.children.isNotEmpty()) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }

            Icon(
                imageVector = selectionIcon,
                contentDescription = selectionDescription,
                tint = selectionTint,
                modifier = Modifier.size(20.dp).padding(end = 8.dp)
            )

            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }

        if (expanded) {
            group.children.forEach { child ->
                GroupItem(
                    group = child,
                    selectedIds = selectedIds,
                    onSelectionChanged = onSelectionChanged,
                    level = level + 1
                )
            }
        }
    }
}

private fun RespiratoryGroup.hasSelectedDescendantOrSelf(selectedIds: Set<String>): Boolean {
    return selectedIds.contains(id) || children.any { it.hasSelectedDescendantOrSelf(selectedIds) }
}

private fun RespiratoryGroup.descendantIdsAndSelf(): Set<String> {
    return buildSet {
        add(id)
        children.forEach { addAll(it.descendantIdsAndSelf()) }
    }
}

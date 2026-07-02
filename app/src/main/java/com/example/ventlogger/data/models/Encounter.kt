package com.example.ventlogger.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "encounters")
data class Encounter(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val patientIdentifier: String = "",
    val location: String = "",
    val encounterTime: Long = System.currentTimeMillis(),
    val interactionType: String = "",
    val selectedGroupIds: List<String> = emptyList(),
    val groupedNotes: Map<String, String> = emptyList<Pair<String, String>>().toMap(),
    val mediaAttachments: List<MediaAttachment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

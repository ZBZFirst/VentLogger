package com.example.ventlogger.data

import android.content.Context
import com.example.ventlogger.data.local.EncounterDao
import com.example.ventlogger.data.models.Encounter
import com.example.ventlogger.data.models.RespiratoryGroup
import com.example.ventlogger.data.models.RespiratoryHierarchy
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class AppRepository(
    private val context: Context,
    private val encounterDao: EncounterDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getRespiratoryHierarchy(): List<RespiratoryGroup> {
        return try {
            val jsonString = context.assets.open("respiratory_group_hierarchy.json")
                .bufferedReader()
                .use { it.readText() }
            val hierarchy = json.decodeFromString<RespiratoryHierarchy>(jsonString)
            hierarchy.hierarchy + RespiratoryGroup(
                id = "app_comment",
                name = "Comment",
                path = listOf("Comment")
            )
        } catch (e: Exception) {
            listOf(
                RespiratoryGroup(
                    id = "app_comment",
                    name = "Comment",
                    path = listOf("Comment")
                )
            )
        }
    }

    fun getAllEncounters(): Flow<List<Encounter>> = encounterDao.getAllEncounters()

    suspend fun getEncounterById(id: String): Encounter? = encounterDao.getEncounterById(id)

    suspend fun saveEncounter(encounter: Encounter) {
        if (encounterDao.getEncounterById(encounter.id) != null) {
            encounterDao.updateEncounter(encounter.copy(updatedAt = System.currentTimeMillis()))
        } else {
            encounterDao.insertEncounter(encounter)
        }
    }

    suspend fun deleteEncounter(encounter: Encounter) = encounterDao.deleteEncounter(encounter)
}

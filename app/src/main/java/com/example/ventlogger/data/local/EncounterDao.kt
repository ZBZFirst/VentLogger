package com.example.ventlogger.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ventlogger.data.models.Encounter
import kotlinx.coroutines.flow.Flow

@Dao
interface EncounterDao {
    @Query("SELECT * FROM encounters ORDER BY updatedAt DESC")
    fun getAllEncounters(): Flow<List<Encounter>>

    @Query("SELECT * FROM encounters WHERE id = :id")
    suspend fun getEncounterById(id: String): Encounter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEncounter(encounter: Encounter)

    @Update
    suspend fun updateEncounter(encounter: Encounter)

    @Delete
    suspend fun deleteEncounter(encounter: Encounter)
}

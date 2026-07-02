package com.example.ventlogger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ventlogger.data.AppRepository
import com.example.ventlogger.data.models.Encounter
import com.example.ventlogger.data.models.RespiratoryGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VentLoggerViewModel(private val repository: AppRepository) : ViewModel() {

    val encounters: StateFlow<List<Encounter>> = repository.getAllEncounters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _respiratoryGroups = MutableStateFlow<List<RespiratoryGroup>>(emptyList())
    val respiratoryGroups: StateFlow<List<RespiratoryGroup>> = _respiratoryGroups.asStateFlow()

    private val _currentEncounter = MutableStateFlow(Encounter())
    val currentEncounter: StateFlow<Encounter> = _currentEncounter.asStateFlow()

    private val _isSetupMode = MutableStateFlow(true)
    val isSetupMode: StateFlow<Boolean> = _isSetupMode.asStateFlow()

    init {
        loadRespiratoryHierarchy()
    }

    private fun loadRespiratoryHierarchy() {
        viewModelScope.launch {
            _respiratoryGroups.value = repository.getRespiratoryHierarchy()
        }
    }

    fun updateEncounter(encounter: Encounter) {
        _currentEncounter.value = encounter
    }

    fun setSetupMode(isSetup: Boolean) {
        _isSetupMode.value = isSetup
    }

    fun startLogging() {
        _isSetupMode.value = false
    }

    fun startNewLogFromCurrentTemplate() {
        _currentEncounter.value = newLogEntryFromTemplate(_currentEncounter.value)
        _isSetupMode.value = false
    }

    fun startNewLogFromTemplate(template: Encounter) {
        _currentEncounter.value = newLogEntryFromTemplate(template)
        _isSetupMode.value = false
    }

    fun startNewLogForPatient(patientIdentifier: String) {
        _currentEncounter.value = Encounter(patientIdentifier = patientIdentifier)
        _isSetupMode.value = false
    }

    fun startNewEncounter() {
        _currentEncounter.value = Encounter()
        _isSetupMode.value = true
    }

    fun selectPatient(patientIdentifier: String) {
        _currentEncounter.value = _currentEncounter.value.copy(patientIdentifier = patientIdentifier)
    }

    fun saveEncounter() {
        viewModelScope.launch {
            val savedEncounter = _currentEncounter.value
            repository.saveEncounter(savedEncounter)
            _currentEncounter.value = newLogEntryFromTemplate(savedEncounter)
        }
    }

    fun finishSession() {
        viewModelScope.launch {
            repository.saveEncounter(_currentEncounter.value)
            startNewEncounter()
        }
    }

    fun deleteEncounter(encounter: Encounter) {
        viewModelScope.launch {
            repository.deleteEncounter(encounter)
        }
    }

    fun deleteEncounters(encounters: List<Encounter>) {
        viewModelScope.launch {
            encounters.forEach { repository.deleteEncounter(it) }
        }
    }

    fun selectEncounter(encounter: Encounter) {
        _currentEncounter.value = encounter
        _isSetupMode.value = false
    }

    fun selectEncounterForSetup(encounter: Encounter) {
        _currentEncounter.value = encounter
        _isSetupMode.value = true
    }

    private fun newLogEntryFromTemplate(template: Encounter): Encounter {
        val now = System.currentTimeMillis()
        return Encounter(
            patientIdentifier = template.patientIdentifier,
            location = template.location,
            encounterTime = now,
            interactionType = template.interactionType,
            selectedGroupIds = template.selectedGroupIds,
            groupedNotes = emptyMap(),
            mediaAttachments = emptyList(),
            createdAt = now,
            updatedAt = now
        )
    }

    fun generateSummary(encounter: Encounter): String {
        val sb = StringBuilder()
        sb.append("Encounter Summary\n")
        sb.append("Patient: ${encounter.patientIdentifier}\n")
        sb.append("Location: ${encounter.location}\n")
        sb.append("Type: ${encounter.interactionType}\n\n")

        if (encounter.selectedGroupIds.isNotEmpty()) {
            sb.append("Observations:\n")
            repository.getRespiratoryHierarchy().forEach { group ->
                appendGroupSummary(sb, group, encounter.selectedGroupIds, encounter.groupedNotes)
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun appendGroupSummary(
        sb: StringBuilder,
        group: RespiratoryGroup,
        selectedIds: List<String>,
        notes: Map<String, String>
    ) {
        if (selectedIds.contains(group.id)) {
            val note = notes[group.id]
            if (!note.isNullOrBlank()) {
                sb.append("- ${group.name}: $note\n")
            }
        }
        group.children.forEach { child ->
            appendGroupSummary(sb, child, selectedIds, notes)
        }
    }

    class Factory(private val repository: AppRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VentLoggerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return VentLoggerViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

package com.example.ventlogger.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RespiratoryGroup(
    val id: String,
    val name: String,
    val path: List<String>,
    val children: List<RespiratoryGroup> = emptyList()
)

@Serializable
data class RespiratoryHierarchy(
    val artifact: String,
    val hierarchy: List<RespiratoryGroup>
)

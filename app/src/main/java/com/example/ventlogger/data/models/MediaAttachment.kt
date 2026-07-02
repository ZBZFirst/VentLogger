package com.example.ventlogger.data.models

import kotlinx.serialization.Serializable

@Serializable
data class MediaAttachment(
    val uri: String,
    val type: MediaAttachmentType,
    val addedAt: Long = System.currentTimeMillis()
)

@Serializable
enum class MediaAttachmentType {
    PHOTO,
    VIDEO
}

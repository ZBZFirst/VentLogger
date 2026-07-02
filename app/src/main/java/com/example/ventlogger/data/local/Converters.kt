package com.example.ventlogger.data.local

import androidx.room.TypeConverter
import com.example.ventlogger.data.models.MediaAttachment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromMediaAttachmentList(value: List<MediaAttachment>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toMediaAttachmentList(value: String): List<MediaAttachment> {
        return Json.decodeFromString(value)
    }
}

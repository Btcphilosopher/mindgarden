package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val folderId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isJournal: Boolean = false,
    val mood: String? = null, // "Happy", "Neutral", "Productive", "Anxious", "Inspired", "Tired", "Reflective"
    val imageUri: String? = null,
    val voiceUri: String? = null,
    val voiceText: String? = null
)

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#6200EE" // Hex color string for visual identity
)

@Entity(tableName = "note_links", primaryKeys = ["sourceId", "targetId"])
data class NoteLink(
    val sourceId: Long,
    val targetId: Long
)

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",,,")
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
         if (list.isNullOrEmpty()) return ""
         return list.joinToString(",,,")
    }
}

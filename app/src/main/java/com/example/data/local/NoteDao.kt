package com.example.data.local

import androidx.room.*
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteLink
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // Notes
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE isJournal = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getStandardNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isJournal = 1 ORDER BY timestamp DESC")
    fun getJournalNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    // Folders
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    // Links (Obsidian Knowledge connections)
    @Query("SELECT * FROM note_links")
    fun getAllLinks(): Flow<List<NoteLink>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: NoteLink)

    @Query("DELETE FROM note_links WHERE (sourceId = :id1 AND targetId = :id2) OR (sourceId = :id2 AND targetId = :id1)")
    suspend fun deleteLink(id1: Long, id2: Long)

    @Query("DELETE FROM note_links WHERE sourceId = :noteId OR targetId = :noteId")
    suspend fun deleteLinksForNote(noteId: Long)
}

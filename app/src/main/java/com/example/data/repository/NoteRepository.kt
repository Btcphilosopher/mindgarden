package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteLink
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val standardNotes: Flow<List<Note>> = noteDao.getStandardNotes()
    val journalNotes: Flow<List<Note>> = noteDao.getJournalNotes()
    val allFolders: Flow<List<Folder>> = noteDao.getAllFolders()
    val allLinks: Flow<List<NoteLink>> = noteDao.getAllLinks()

    suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteNote(id)
        noteDao.deleteLinksForNote(id)
    }

    suspend fun insertFolder(folder: Folder): Long {
        return noteDao.insertFolder(folder)
    }

    suspend fun deleteFolder(id: Long) {
        noteDao.deleteFolder(id)
    }

    suspend fun insertLink(sourceId: Long, targetId: Long) {
        if (sourceId != targetId) {
            noteDao.insertLink(NoteLink(sourceId = sourceId, targetId = targetId))
        }
    }

    suspend fun deleteLink(id1: Long, id2: Long) {
        noteDao.deleteLink(id1, id2)
    }
}

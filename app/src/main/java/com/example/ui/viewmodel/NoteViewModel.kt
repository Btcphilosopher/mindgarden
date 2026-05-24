package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteLink
import com.example.data.repository.NoteRepository
import com.example.ui.ai.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository

    val allNotes: StateFlow<List<Note>>
    val standardNotes: StateFlow<List<Note>>
    val journalNotes: StateFlow<List<Note>>
    val folders: StateFlow<List<Folder>>
    val links: StateFlow<List<NoteLink>>

    // Navigation and UI States
    private val _currentScreen = MutableStateFlow("library") // "capture", "library", "graph", "search", "journal", "editor"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote: StateFlow<Note?> = _selectedNote.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // AI summary and status states
    private val _weeklySummary = MutableStateFlow<String?>(null)
    val weeklySummary: StateFlow<String?> = _weeklySummary.asStateFlow()

    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    private val _aiStatusMessage = MutableStateFlow<String?>(null)
    val aiStatusMessage: StateFlow<String?> = _aiStatusMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NoteRepository(database.noteDao)

        allNotes = repository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        standardNotes = repository.standardNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        journalNotes = repository.journalNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        folders = repository.allFolders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        links = repository.allLinks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed default folders if empty
        viewModelScope.launch {
            folders.collect { list ->
                if (list.isEmpty()) {
                    repository.insertFolder(Folder(name = "Ideas", colorHex = "#FF9800"))
                    repository.insertFolder(Folder(name = "Work", colorHex = "#2196F3"))
                    repository.insertFolder(Folder(name = "Research", colorHex = "#4CAF50"))
                    repository.insertFolder(Folder(name = "Journal", colorHex = "#E91E63"))
                    repository.insertFolder(Folder(name = "Projects", colorHex = "#9C27B0"))
                }
            }
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
        if (screen != "editor") {
            _selectedNote.value = null
        }
    }

    fun selectNote(note: Note) {
        _selectedNote.value = note
        navigateTo("editor")
    }

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun clearAiStatus() {
        _aiStatusMessage.value = null
    }

    // Database Actions
    fun insertNote(note: Note, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertNote(note)
            onComplete?.invoke(id)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNote(note)
            // If editing active note, sync state
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = note
            }
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(noteId)
            if (_selectedNote.value?.id == noteId) {
                _selectedNote.value = null
                _currentScreen.value = "library"
            }
        }
    }

    fun addFolder(name: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertFolder(Folder(name = name, colorHex = colorHex))
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFolder(folderId)
            if (_selectedFolderId.value == folderId) {
                _selectedFolderId.value = null
            }
        }
    }

    fun addLink(sourceId: Long, targetId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLink(sourceId, targetId)
        }
    }

    fun deleteLink(sourceId: Long, targetId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLink(sourceId, targetId)
        }
    }

    // AI Actions
    fun suggestTags(note: Note) {
        _aiStatusMessage.value = "Suggesting tags..."
        viewModelScope.launch {
            val suggested = GeminiClient.suggestTags(note.title, note.content)
            if (suggested.isNotEmpty()) {
                val updatedTags = (note.tags + suggested).distinct()
                val updatedNote = note.copy(tags = updatedTags)
                updateNote(updatedNote)
                _aiStatusMessage.value = "Added tags: ${suggested.joinToString()}"
            } else {
                _aiStatusMessage.value = "No tags suggested OR Gemini Key missing."
            }
        }
    }

    fun extractChecklist(note: Note) {
        _aiStatusMessage.value = "Extracting tasks..."
        viewModelScope.launch {
            val tasks = GeminiClient.extractChecklist(note.title, note.content)
            if (tasks.isNotEmpty()) {
                val formattedTasks = "\n\n### Extracted Action Items\n" + tasks.joinToString("\n") { "- [ ] $it" }
                val updatedNote = note.copy(content = note.content + formattedTasks)
                updateNote(updatedNote)
                _aiStatusMessage.value = "Extracted ${tasks.size} action tasks!"
            } else {
                _aiStatusMessage.value = "No actionable items found or key missing."
            }
        }
    }

    fun generateWeeklySummary() {
        _isGeneratingSummary.value = true
        _weeklySummary.value = "Pondering weekly entries..."
        viewModelScope.launch {
            val entries = journalNotes.value.map {
                "Title: ${it.title}\nMood: ${it.mood ?: "Neutral"}\nContent: ${it.content}"
            }
            val summary = GeminiClient.generateWeeklySummary(entries)
            _weeklySummary.value = summary
            _isGeneratingSummary.value = false
        }
    }
}

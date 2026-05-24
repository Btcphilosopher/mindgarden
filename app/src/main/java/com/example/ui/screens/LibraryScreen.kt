package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.ui.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.standardNotes.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    // Dynamically compute unique tags from saved notes
    val allTags = remember(notes) {
        notes.flatMap { it.tags }.distinct().sorted()
    }

    // Direct Filtering logic based on search, folder, and tags criteria
    val filteredNotes = remember(notes, searchQuery, selectedFolderId, selectedTag) {
        notes.filter { note ->
            val matchSearch = searchQuery.isEmpty() || 
                note.title.contains(searchQuery, ignoreCase = true) || 
                note.content.contains(searchQuery, ignoreCase = true)
            
            val matchFolder = selectedFolderId == null || note.folderId == selectedFolderId
            val matchTag = selectedTag == null || note.tags.contains(selectedTag)

            matchSearch && matchFolder && matchTag
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with search & arrangement toggle (Mindscape signature branding)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                                .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                        )
                    }
                    Text(
                        text = "MindGarden",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { viewModel.toggleViewMode() }) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle Grid/List Layout",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Semantic & text search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search your cognitive map...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.outline
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Folder Horizonal Bar list
            Text(
                text = "📂 Folders",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFolderId == null,
                        onClick = { viewModel.selectFolder(null) },
                        label = { Text("All Notes") },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
                items(folders) { folder ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(folder.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.secondary
                    }
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { viewModel.selectFolder(folder.id) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(folder.name)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tags Horizontal Chip list
            if (allTags.isNotEmpty()) {
                Text(
                    text = "🏷️ Tags filter",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedTag == null,
                            onClick = { viewModel.selectTag(null) },
                            label = { Text("Clear Tag") }
                        )
                    }
                    items(allTags) { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { viewModel.selectTag(tag) },
                            label = { Text("#$tag") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Note List or Cards Grid
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty list",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (notes.isEmpty()) "No thoughts stored. Swipe over to Quick Capture!" 
                                   else "No thoughts match active filters.",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredNotes) { note ->
                            NoteCardItem(
                                note = note,
                                folder = folders.find { it.id == note.folderId },
                                onClick = { viewModel.selectNote(note) },
                                onLongClick = { noteToDelete = note }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredNotes) { note ->
                            NoteListItem(
                                note = note,
                                folder = folders.find { it.id == note.folderId },
                                onClick = { viewModel.selectNote(note) },
                                onLongClick = { noteToDelete = note }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button (FAB) to add blank note directly in editor
        LargeFloatingActionButton(
            onClick = {
                val newNote = Note(
                    title = "Untitled note",
                    content = "",
                    folderId = selectedFolderId,
                    tags = if (selectedTag != null) listOf(selectedTag!!) else emptyList()
                )
                viewModel.insertNote(newNote) { freshId ->
                    // Auto select the newly inserted note for editing
                    viewModel.selectNote(newNote.copy(id = freshId))
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add blank note", modifier = Modifier.size(28.dp))
        }

        // Delete Confirmation Dialog
        if (noteToDelete != null) {
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                title = { Text("Delete Mind Node?") },
                text = { Text("Are you sure you want to delete \"${noteToDelete?.title}\"? This operation cannot be undone and will strip any Obsidian mental linkage threads.") },
                confirmButton = {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            noteToDelete?.let { viewModel.deleteNote(it.id) }
                            noteToDelete = null
                        }
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// --- List Note Card Component ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItem(
    note: Note,
    folder: Folder?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateStr = remember(note.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(note.timestamp))
    }

    val folderColor = remember(folder) {
        try {
            Color(android.graphics.Color.parseColor(folder?.colorHex ?: "#6200EE"))
        } catch (e: Exception) {
            Color.LightGray
        }
    }

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder color strip banner
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(55.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (folder != null) folderColor else Color.Gray.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (note.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = "Pinned note",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = note.content.ifEmpty { "Empty document map" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (note.imageUri != null) {
                            Icon(Icons.Outlined.Photo, contentDescription = "Holds image", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(13.dp))
                        }
                        if (note.voiceUri != null) {
                            Icon(Icons.Outlined.Mic, contentDescription = "Holds voice note", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(13.dp))
                        }
                        note.tags.take(3).forEach { tag ->
                            Text(
                                text = "#$tag",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Grid Note Card Component ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCardItem(
    note: Note,
    folder: Folder?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateStr = remember(note.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(note.timestamp))
    }

    val folderColor = remember(folder) {
        try {
            Color(android.graphics.Color.parseColor(folder?.colorHex ?: "#6200EE"))
        } catch (e: Exception) {
            Color.LightGray
        }
    }

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Folder name badge
                    if (folder != null) {
                        Surface(
                            color = folderColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = folder.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = folderColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = "Pinned note",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = note.content.ifEmpty { "Empty document map" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)

                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (note.imageUri != null) {
                        Icon(Icons.Outlined.Photo, contentDescription = "Has photo", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(11.dp))
                    }
                    if (note.voiceUri != null) {
                        Icon(Icons.Outlined.Mic, contentDescription = "Has recording", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(11.dp))
                    }
                }
            }
        }
    }
}

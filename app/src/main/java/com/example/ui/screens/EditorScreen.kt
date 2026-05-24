package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val noteOpt by viewModel.selectedNote.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val links by viewModel.links.collectAsState()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsState()

    // Ensure we have a valid selected note, otherwise show guidance
    val note = noteOpt ?: return

    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    var selectedFolderId by remember(note.id) { mutableStateOf(note.folderId) }
    val tags = remember(note.id) { mutableStateListOf<String>().apply { addAll(note.tags) } }

    var tagInput by remember { mutableStateOf("") }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var linkNoteMenuExpanded by remember { mutableStateOf(false) }

    // Auto-save logic on title, content, folder, or tag change
    LaunchedEffect(title, content, selectedFolderId, tags.toList()) {
        if (title != note.title || content != note.content || selectedFolderId != note.folderId || tags.toList() != note.tags) {
            delay(800) // Debounce saves
            viewModel.updateNote(
                note.copy(
                    title = title,
                    content = content,
                    folderId = selectedFolderId,
                    tags = tags.toList(),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // Capture task lines in real-time to render interactive checkboxes
    val taskItems = remember(content) {
        content.split("\n")
            .mapIndexed { idx, line -> Pair(idx, line) }
            .filter { (_, line) -> line.trim().startsWith("- [ ]") || line.trim().startsWith("- [x]") }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header actions: Back, Pinned toggler, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo("library") }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pinned toggler
                    IconButton(
                        onClick = {
                            val nextPin = !note.isPinned
                            viewModel.updateNote(note.copy(isPinned = nextPin))
                        }
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Toggle Pin",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                        )
                    }

                    // Delete Note
                    IconButton(
                        onClick = {
                            viewModel.deleteNote(note.id)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Folder & Category selector label
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val activeFolder = folders.find { it.id == selectedFolderId }
                val folderColor = try {
                    Color(android.graphics.Color.parseColor(activeFolder?.colorHex ?: "#6200EE"))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                Box {
                    AssistChip(
                        onClick = { folderMenuExpanded = true },
                        label = { Text(activeFolder?.name ?: "Assign Folder") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (activeFolder != null) Icons.Default.FolderSpecial else Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = if (activeFolder != null) folderColor else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )

                    DropdownMenu(
                        expanded = folderMenuExpanded,
                        onDismissRequest = { folderMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Folder") },
                            onClick = {
                                selectedFolderId = null
                                folderMenuExpanded = false
                            }
                        )
                        Divider()
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(folder.colorHex)))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(folder.name)
                                    }
                                },
                                onClick = {
                                    selectedFolderId = folder.id
                                    folderMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Auto-save tag label
                Text(
                    text = "Autosaved ✓",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note title Editor
            TextField(
                value = title,
                onValueChange = { title = it },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                placeholder = { Text("Title...", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            // Dynamic Inline AI Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gemini Tag Generator
                OutlinedButton(
                    onClick = { viewModel.suggestTags(note) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Tags", fontSize = 11.sp)
                }

                // Gemini Action items Extractor
                OutlinedButton(
                    onClick = { viewModel.extractChecklist(note) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Tasks", fontSize = 11.sp)
                }
            }

            // AI Status notification panel
            if (aiStatusMessage != null) {
                OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = CardDefaults.outlinedCardBorder(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.6.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(aiStatusMessage!!, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        IconButton(onClick = { viewModel.clearAiStatus() }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Markdown Assistant bar shortcuts above editing text box
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val formats = listOf(
                        Pair("H1", "# "),
                        Pair("H2", "## "),
                        Pair("H3", "### "),
                        Pair("Bullet", "- "),
                        Pair("Task", "- [ ] "),
                        Pair("Code", "`")
                    )

                    formats.forEach { (label, token) ->
                        TextButton(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            onClick = {
                                if (token == "`") {
                                    content += " `code` "
                                } else {
                                    // Prepend markdown token to content
                                    content = if (content.isEmpty() || content.endsWith("\n")) {
                                        content + token
                                    } else {
                                        content + "\n" + token
                                    }
                                }
                            }
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note Content primary text box
            TextField(
                value = content,
                onValueChange = { content = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp, fontFamily = FontFamily.Default),
                placeholder = { Text("Write structured documentation, ideas, or markdown notebooks...", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Render interactive Checklist parsed section
            if (taskItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "☑ Interactive Checklist HUD",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Live-clicks sync directly into markdown document above.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                taskItems.forEach { (lineIndex, lineText) ->
                    val isChecked = lineText.trim().startsWith("- [x]")
                    val cleanText = lineText.replace("- [ ]", "").replace("- [x]", "").trim()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val split = content.split("\n").toMutableList()
                                val oldLine = split[lineIndex]
                                val newLine = if (isChecked) {
                                    oldLine.replace("- [x]", "- [ ]")
                                } else {
                                    oldLine.replace("- [ ]", "- [x]")
                                }
                                split[lineIndex] = newLine
                                content = split.joinToString("\n")
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                val split = content.split("\n").toMutableList()
                                val oldLine = split[lineIndex]
                                val newLine = if (isChecked) {
                                    oldLine.replace("- [x]", "- [ ]")
                                } else {
                                    oldLine.replace("- [ ]", "- [x]")
                                }
                                split[lineIndex] = newLine
                                content = split.joinToString("\n")
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cleanText.ifEmpty { "Empty Checklist Task" },
                            fontSize = 14.sp,
                            color = if (isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Backlink Obsidian Threads Section
            Text(
                text = "🌌 Backlink Threads (Bidirectional Map)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // List of notes this actively connects to
            val activeLinkedIds = remember(links, note.id) {
                links.filter { it.sourceId == note.id }.map { it.targetId } +
                links.filter { it.targetId == note.id }.map { it.sourceId }
            }

            val linkedNotes = remember(allNotes, activeLinkedIds) {
                allNotes.filter { activeLinkedIds.contains(it.id) }
            }

            if (linkedNotes.isEmpty()) {
                Text(
                    text = "No linked nodes. Click 'Weave Backlink' below to anchor this note to another knowledge point.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    items(linkedNotes) { lNote ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.selectNote(lNote) },
                            label = { Text("🔗 ${lNote.title}") },
                            trailingIcon = {
                                IconButton(
                                    modifier = Modifier.size(10.dp),
                                    onClick = { viewModel.deleteLink(note.id, lNote.id) }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Sever thread", modifier = Modifier.size(10.dp))
                                }
                            }
                        )
                    }
                }
            }

            // Weave backlink thread dropdown selector
            Box {
                Button(
                    onClick = { linkNoteMenuExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Weave backlink thread...", fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = linkNoteMenuExpanded,
                    onDismissRequest = { linkNoteMenuExpanded = false }
                ) {
                    val linkCandidates = allNotes.filter { it.id != note.id && !activeLinkedIds.contains(it.id) }
                    if (linkCandidates.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No other notes available") },
                            onClick = { linkNoteMenuExpanded = false }
                        )
                    } else {
                        linkCandidates.forEach { candidateNote ->
                            DropdownMenuItem(
                                text = { Text(candidateNote.title) },
                                onClick = {
                                    viewModel.addLink(note.id, candidateNote.id)
                                    linkNoteMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Inline Tags chip list
            Text(
                text = "🏷️ Tags on this note",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { tags.remove(tag) },
                        label = { Text("#$tag") },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Remove tag", modifier = Modifier.size(12.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inline Add tag field
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                placeholder = { Text("Append custom tag (e.g. design, logic)...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (tagInput.trim().isNotEmpty()) {
                        val clean = tagInput.trim().lowercase().removePrefix("#")
                        if (!tags.contains(clean)) tags.add(clean)
                        tagInput = ""
                    }
                }),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        if (tagInput.trim().isNotEmpty()) {
                            val clean = tagInput.trim().lowercase().removePrefix("#")
                            if (!tags.contains(clean)) tags.add(clean)
                            tagInput = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                }
            )

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

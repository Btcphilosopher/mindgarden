package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val folders by viewModel.folders.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var tagInput by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() }

    var isChecklistMode by remember { mutableStateOf(false) }
    var isVoiceRecording by remember { mutableStateOf(false) }
    var voiceRecordDuration by remember { mutableStateOf(0) }
    var simulatedVoiceTranscript by remember { mutableStateOf("") }

    // Floating action feedback
    var showSuccessToast by remember { mutableStateOf(false) }

    // Recording animation timer
    LaunchedEffect(isVoiceRecording) {
        if (isVoiceRecording) {
            voiceRecordDuration = 0
            while (isVoiceRecording) {
                delay(1000)
                voiceRecordDuration++
                // simulated automatic voice transcribing in background
                if (voiceRecordDuration == 4) simulatedVoiceTranscript = "Researching Obsidian obsidian-style backlinks in Jetpack Compose layout..."
                if (voiceRecordDuration == 8) simulatedVoiceTranscript = "Idea: Combine nested folder schema with bidirectional mental clustering node diagrams."
            }
        }
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
            Text(
                text = "⚡ Quick Capture",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "QUICK CAPTURE",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                    // Title field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Unstructured Thought Title...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Content editor
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { 
                            Text(
                                if (isChecklistMode) "Enter tasks (separated by newline)..."
                                else "Start typing idea, journal, or notes..."
                            ) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Utilities Row: Voice, Checklist Format, Tags, Folder Quick Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left buttons: checklist toggle & dictation trigger
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledIconToggleButton(
                                checked = isChecklistMode,
                                onCheckedChange = { 
                                    isChecklistMode = it
                                    if (it && !content.startsWith("- [ ]")) {
                                        // Auto format entered lines to checklist block style
                                        val lines = content.split("\n")
                                        content = lines.joinToString("\n") { l ->
                                            if (l.trim().isNotEmpty() && !l.trim().startsWith("- [ ]")) "- [ ] $l" else l
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Checklist,
                                    contentDescription = "Toggle Checklist Mode"
                                )
                            }

                            FilledIconButton(
                                onClick = { isVoiceRecording = true },
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = "Voice Dictate",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Folder selector dropdown
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            val activeFolder = folders.find { it.id == selectedFolderId }

                            Button(
                                onClick = { expanded = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Outlined.Folder, contentDescription = "Folder", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeFolder?.name ?: "No Folder",
                                    fontSize = 13.sp
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("No Folder") },
                                    onClick = {
                                        selectedFolderId = null
                                        expanded = false
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
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tags Input Row
                    Text(
                        text = "🏷️ Tags",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        placeholder = { Text("Add tag and press Enter...") },
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
                                Icon(Icons.Default.Add, contentDescription = "Add Tag")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Render current tags lists
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
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove tag",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action submit button
            Button(
                onClick = {
                    if (title.trim().isEmpty() && content.trim().isEmpty()) return@Button
                    val finalTitle = title.trim().ifEmpty { "Quick Thought - ${System.currentTimeMillis() % 10000}" }
                    val finalNote = Note(
                        title = finalTitle,
                        content = content,
                        folderId = selectedFolderId,
                        tags = tags.toList(),
                        isPinned = false,
                        isJournal = false,
                        timestamp = System.currentTimeMillis()
                    )
                    viewModel.insertNote(finalNote)

                    // Clear inputs
                    title = ""
                    content = ""
                    selectedFolderId = null
                    tags.clear()
                    isChecklistMode = false
                    focusManager.clearFocus()

                    coroutineScope.launch {
                        showSuccessToast = true
                        delay(2000)
                        showSuccessToast = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = title.isNotEmpty() || content.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Instant Save Thought", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Quick Success Feedback Toast Overlay
        AnimatedVisibility(
            visible = showSuccessToast,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Saved to structured memory!",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Immersive simulated voice dictation recording modal overlay
        if (isVoiceRecording) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "🧠 Listening to thoughts...",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pulse Soundwave Animation visualizer
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    ) {
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {}
                        Surface(
                            modifier = Modifier.size(70.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        ) {}
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    // Complete Voice Dictation
                                    if (simulatedVoiceTranscript.isNotEmpty()) {
                                        if (title.isEmpty()) title = "Voice Capture: ${voiceRecordDuration}s"
                                        content = if (content.isEmpty()) simulatedVoiceTranscript else content + "\n\n" + simulatedVoiceTranscript
                                    } else {
                                        content = if (content.isEmpty()) "Voice capture timed at ${voiceRecordDuration}s" else content + "\n\nVoice capture timed at ${voiceRecordDuration}s"
                                    }
                                    isVoiceRecording = false
                                    simulatedVoiceTranscript = ""
                                }
                                .size(50.dp),
                            shape = CircleShape,
                            color = Color.Red
                        ) {
                            Icon(
                                imageVector = Icons.Default.Square,
                                contentDescription = "Stop recording",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(14.dp)
                                    .size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Duration clock
                    val minutes = voiceRecordDuration / 60
                    val seconds = voiceRecordDuration % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Real-time voice subtitle text box
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = simulatedVoiceTranscript.ifEmpty { "Start speaking. The real-time speech processor will parse structured thoughts automatically..." },
                            color = if (simulatedVoiceTranscript.isEmpty()) Color.LightGray else Color.White,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    TextButton(
                        onClick = {
                            isVoiceRecording = false
                            simulatedVoiceTranscript = ""
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Cancel Dictation", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

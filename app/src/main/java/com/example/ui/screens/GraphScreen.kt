package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteLink
import com.example.ui.viewmodel.NoteViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.allNotes.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val links by viewModel.links.collectAsState()

    val textMeasurer = rememberTextMeasurer()

    // Screen scale and pan variables
    var zoomScale by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    // Map storing active node visual coordinates. Seeded on change
    val nodePositions = remember { mutableStateMapOf<Long, Offset>() }

    // Floating dialog or notification
    var selectedNodeIdForLinking by remember { mutableStateOf<Long?>(null) }
    var noteSelectionDropdownExpanded by remember { mutableStateOf(false) }

    // Seed coordinates in a responsive spiral layout if notes exist and aren't seeded yet
    LaunchedEffect(notes) {
        val r = Random(42)
        notes.forEachIndexed { index, note ->
            if (!nodePositions.containsKey(note.id)) {
                // Circular/spiral layout coordinates
                val angle = index * 0.95f
                val radius = 180f + (index * 45f)
                val x = 400f + radius * cos(angle)
                val y = 400f + radius * sin(angle)
                nodePositions[note.id] = Offset(x, y)
            }
        }
        // Remove positions of deleted notes
        val activeIds = notes.map { it.id }.toSet()
        val storedIds = nodePositions.keys.toList()
        storedIds.forEach { id ->
            if (!activeIds.contains(id)) {
                nodePositions.remove(id)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main zoomable graph canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Let users click details OR drag nodes
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Look up if user clicked a node to drag it
                            val actualOffset = (offset - panOffset) / zoomScale
                            val clickedNode = nodePositions.entries.find { (_, pos) ->
                                (pos - actualOffset).getDistance() < 42f // 42dp tap radius
                            }
                            selectedNodeIdForLinking = clickedNode?.key
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val selectedId = selectedNodeIdForLinking
                            if (selectedId != null && nodePositions.containsKey(selectedId)) {
                                val curPos = nodePositions[selectedId]!!
                                nodePositions[selectedId] = curPos + (dragAmount / zoomScale)
                            } else {
                                // Pan entire canvas instead
                                panOffset += dragAmount
                            }
                        },
                        onDragEnd = {
                            selectedNodeIdForLinking = null
                        }
                    )
                }
        ) {
            // 1. Draw connection lines (links)
            links.forEach { link ->
                val start = nodePositions[link.sourceId]
                val end = nodePositions[link.targetId]
                if (start != null && end != null) {
                    val pStart = (start * zoomScale) + panOffset
                    val pEnd = (end * zoomScale) + panOffset
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.45f),
                        start = pStart,
                        end = pEnd,
                        strokeWidth = 2.5f * zoomScale
                    )
                }
            }

            // 2. Draw nodes for thoughts
            notes.forEach { note ->
                val rawPos = nodePositions[note.id]
                if (rawPos != null) {
                    val pos = (rawPos * zoomScale) + panOffset
                    
                    val folder = folders.find { it.id == note.folderId }
                    val folderColor = try {
                        Color(android.graphics.Color.parseColor(folder?.colorHex ?: "#6200EE"))
                    } catch (e: Exception) {
                        Color.Gray
                    }

                    // Pulse outer circle if long/linked
                    drawCircle(
                        color = folderColor.copy(alpha = 0.25f),
                        radius = 42f * zoomScale,
                        center = pos
                    )

                    // Sharp core circle dot
                    drawCircle(
                        color = folderColor,
                        radius = 16f * zoomScale,
                        center = pos
                    )

                    // Draw note title text right under the node icon dot
                    val textLayoutResult = textMeasurer.measure(
                        text = note.title,
                        style = TextStyle(
                            fontSize = (11f * zoomScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = folderColor,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    val textOffset = Offset(
                        x = pos.x - (textLayoutResult.size.width / 2f),
                        y = pos.y + (22f * zoomScale)
                    )
                    drawText(textLayoutResult = textLayoutResult, topLeft = textOffset)
                }
            }
        }

        // Title indicator bar overlay
        OutlinedCard(
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
            ),
            border = CardDefaults.outlinedCardBorder(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .fillMaxWidth(0.82f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🧠 Mental Knowledge Graph",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Observe clusters of ideas. Drag nodes to align. Tap details button below to establish new backlinks.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Side Canvas Zoom controls
        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterEnd),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(2.5f) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            SmallFloatingActionButton(
                onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.4f) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            SmallFloatingActionButton(
                onClick = {
                    zoomScale = 1.0f
                    panOffset = Offset(0f, 0f)
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Pan")
            }
        }

        // Bottom linkage establish bar
        OutlinedCard(
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            border = CardDefaults.outlinedCardBorder(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔗 Bidirectional Link Creator",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Build Obsidian-style semantic relationship threads.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Button(
                    onClick = { noteSelectionDropdownExpanded = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Link Thread", fontSize = 12.sp)
                }
            }
        }

        // Note linking builder overlay selector dialog
        if (noteSelectionDropdownExpanded) {
            var selectedNoteA by remember { mutableStateOf<Note?>(null) }
            var selectedNoteB by remember { mutableStateOf<Note?>(null) }
            var expandedA by remember { mutableStateOf(false) }
            var expandedB by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { noteSelectionDropdownExpanded = false },
                title = { Text("Thread a connection link") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Connect two standard nodes to cement a cognitive path link.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                        // Dropdown A
                        Column {
                            Text("Source Node", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedA = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(selectedNoteA?.title ?: "Select note A ...")
                                }
                                DropdownMenu(expanded = expandedA, onDismissRequest = { expandedA = false }) {
                                    notes.forEach { note ->
                                        DropdownMenuItem(
                                            text = { Text(note.title) },
                                            onClick = {
                                                selectedNoteA = note
                                                expandedA = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Dropdown B
                        Column {
                            Text("Target Node", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedB = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(selectedNoteB?.title ?: "Select note B ...")
                                }
                                DropdownMenu(expanded = expandedB, onDismissRequest = { expandedB = false }) {
                                    notes.forEach { note ->
                                        DropdownMenuItem(
                                            text = { Text(note.title) },
                                            onClick = {
                                                selectedNoteB = note
                                                expandedB = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = selectedNoteA != null && selectedNoteB != null && selectedNoteA?.id != selectedNoteB?.id,
                        onClick = {
                            if (selectedNoteA != null && selectedNoteB != null) {
                                viewModel.addLink(selectedNoteA!!.id, selectedNoteB!!.id)
                            }
                            noteSelectionDropdownExpanded = false
                        }
                    ) {
                        Text("Establish Thread Link", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteSelectionDropdownExpanded = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

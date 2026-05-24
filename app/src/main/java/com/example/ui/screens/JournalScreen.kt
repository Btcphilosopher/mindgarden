package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.Note
import com.example.ui.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val journalNotes by viewModel.journalNotes.collectAsState()
    val weeklySummary by viewModel.weeklySummary.collectAsState()
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsState()

    var journalTitle by remember { mutableStateOf("") }
    var journalContent by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("Neutral") }
    var activePromptIndex by remember { mutableStateOf(0) }

    val dailyPrompts = listOf(
        "🧠 What thoughts or exciting insights currently occupy your working memory?",
        "🧗 Describe a subtle friction or roadblock you parsed today and how you passed it.",
        "💡 If you could compress your intellectual progress today into one core lesson, what would it be?",
        "🧘 What are you deeply grateful or reflective of in your cognitive space today?",
        "🚀 What core objective will receive your structured focus tomorrow?"
    )

    // Set default journal title with current date
    LaunchedEffect(Unit) {
        val today = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date())
        journalTitle = "Reflection Log: $today"
    }

    val moods = listOf(
        Triple("😊 Happy", "Happy", Color(0xFF81C784)),
        Triple("😐 Neutral", "Neutral", Color(0xFF9E9E9E)),
        Triple("⚡ Inspired", "Inspired", Color(0xFFFFB74D)),
        Triple("😰 Anxious", "Anxious", Color(0xFFE57373)),
        Triple("😴 Tired", "Tired", Color(0xFF64B5F6)),
        Triple("🧘 Calm", "Calm", Color(0xFFBA68C8))
    )

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
                text = "⏳ Daily Journal Mode",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "De-congest your mental working memory. Track emotions, solve daily prompts, and synthesize cognitive threads over time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Mood Tracking Hub
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎭 Active Mood Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Graphing your psychological frequency enables long-term balance modeling.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(moods) { (label, value, color) ->
                            val isSelected = selectedMood == value
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMood = value },
                                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.25f),
                                    selectedLabelColor = color
                                )
                            )
                        }
                    }
                }
            }

            // Structured prompt selector helper
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Daily Inspirational Prompt",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(
                            modifier = Modifier.size(24.dp),
                            onClick = {
                                activePromptIndex = (activePromptIndex + 1) % dailyPrompts.size
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Next Prompt", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = dailyPrompts[activePromptIndex],
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            if (!journalContent.contains(dailyPrompts[activePromptIndex])) {
                                journalContent = dailyPrompts[activePromptIndex] + "\n\n" + journalContent
                            }
                        }
                    ) {
                        Text("Inject Prompt Into Journal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Journal Workspace card editor
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✍️ Write Daily Reflection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = journalTitle,
                        onValueChange = { journalTitle = it },
                        placeholder = { Text("Log Title...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = journalContent,
                        onValueChange = { journalContent = it },
                        placeholder = { Text("Describe today's thoughts, achievements, or reflect on current feelings...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (journalContent.trim().isEmpty()) return@Button
                            val log = Note(
                                title = journalTitle.trim().ifEmpty { "Daily Reflection Log" },
                                content = journalContent,
                                mood = selectedMood,
                                isJournal = true,
                                tags = listOf("journal", selectedMood.lowercase()),
                                timestamp = System.currentTimeMillis()
                            )
                            viewModel.insertNote(log)

                            // Clear content, re-stamp date title
                            journalContent = ""
                            val today = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date())
                            journalTitle = "Reflection Log: $today"
                        },
                        enabled = journalContent.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock Memory Entry", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Weekly Summary AI generator panel
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚡ Cognitive Pattern Synthesis (AI Reflection)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Processes saved journal transcripts to reveal emotional trends, progress achievements, and cognitive links.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reflection analysis results viewer
                    if (weeklySummary != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = weeklySummary!!,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.generateWeeklySummary() },
                        enabled = !isGeneratingSummary && journalNotes.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGeneratingSummary) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGeneratingSummary) "Analyzing Journals..." else "Synthesize Weekly Patterns",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Historical reflections lists
            Text(
                text = "📜 Past Reflections Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (journalNotes.isEmpty()) {
                Text(
                    text = "No saved journal logs. Log your first mental entry above!",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            } else {
                journalNotes.forEach { jNote ->
                    val jDate = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(jNote.timestamp))
                    OutlinedCard(
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                     ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(jNote.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                
                                jNote.mood?.let { m ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(m, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(jDate, fontSize = 10.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                jNote.content,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

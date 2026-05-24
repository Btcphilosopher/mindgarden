package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.GraphScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.QuickCaptureScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NoteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: NoteViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Avoid showing bottom bar when deep inside active text editor
                        if (currentScreen != "editor") {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentScreen == "library",
                                    onClick = { viewModel.navigateTo("library") },
                                    label = { Text("Library") },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentScreen == "library") Icons.Filled.StickyNote2 else Icons.Outlined.StickyNote2,
                                            contentDescription = "Notes Library"
                                        )
                                    }
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "capture",
                                    onClick = { viewModel.navigateTo("capture") },
                                    label = { Text("Capture") },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentScreen == "capture") Icons.Filled.FlashOn else Icons.Outlined.FlashOn,
                                            contentDescription = "Quick Capture"
                                        )
                                    }
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "graph",
                                    onClick = { viewModel.navigateTo("graph") },
                                    label = { Text("Mind Graph") },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentScreen == "graph") Icons.Filled.Hub else Icons.Outlined.Hub,
                                            contentDescription = "Knowledge Graph"
                                        )
                                    }
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "journal",
                                    onClick = { viewModel.navigateTo("journal") },
                                    label = { Text("Journal") },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentScreen == "journal") Icons.Filled.HourglassFull else Icons.Outlined.HourglassEmpty,
                                            contentDescription = "Daily Journal"
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)

                    when (currentScreen) {
                        "library" -> LibraryScreen(viewModel = viewModel, modifier = contentModifier)
                        "capture" -> QuickCaptureScreen(viewModel = viewModel, modifier = contentModifier)
                        "graph" -> GraphScreen(viewModel = viewModel, modifier = contentModifier)
                        "journal" -> JournalScreen(viewModel = viewModel, modifier = contentModifier)
                        "editor" -> EditorScreen(viewModel = viewModel, modifier = contentModifier)
                        else -> LibraryScreen(viewModel = viewModel, modifier = contentModifier)
                    }
                }
            }
        }
    }
}

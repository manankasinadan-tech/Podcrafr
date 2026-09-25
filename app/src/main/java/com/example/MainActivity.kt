package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioExporter
import com.example.ui.PodcastViewModel
import com.example.ui.StudioUiEvent
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500

enum class AppScreen(val title: String, val testTag: String) {
    STUDIO("Studio", "nav_tab_studio"),
    PLAYER("Lecteur", "nav_tab_player"),
    LIBRARY("Bibliothèque", "nav_tab_library"),
    SETTINGS("Voix & Paramètres", "nav_tab_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: PodcastViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                var currentScreen by remember { mutableStateOf(AppScreen.STUDIO) }

                // Collect ViewModel events (Snackbar & Audio Export)
                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is StudioUiEvent.ShowSnackbar -> {
                                snackbarHostState.showSnackbar(event.message)
                            }
                            is StudioUiEvent.AudioExported -> {
                                val shareIntent = AudioExporter.shareAudioFile(
                                    this@MainActivity,
                                    event.file,
                                    event.title
                                )
                                startActivity(Intent.createChooser(shareIntent, "Partager le podcast audio"))
                            }
                        }
                    }
                }

                // Handle system back navigation
                if (currentScreen != AppScreen.STUDIO) {
                    BackHandler {
                        currentScreen = AppScreen.STUDIO
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        FlatBottomNavigation(
                            currentScreen = currentScreen,
                            onSelectScreen = { currentScreen = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.STUDIO -> StudioScreen(
                                viewModel = viewModel,
                                onNavigateToPlayer = { currentScreen = AppScreen.PLAYER }
                            )
                            AppScreen.PLAYER -> PlayerScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = AppScreen.STUDIO }
                            )
                            AppScreen.LIBRARY -> LibraryScreen(
                                viewModel = viewModel,
                                onOpenEpisode = { currentScreen = AppScreen.PLAYER }
                            )
                            AppScreen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlatBottomNavigation(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.border(width = 1.dp, color = Slate200),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.STUDIO,
            onClick = { onSelectScreen(AppScreen.STUDIO) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Studio",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Studio",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == AppScreen.STUDIO) FontWeight.Bold else FontWeight.Medium
                )
            },
            modifier = Modifier.testTag("nav_studio"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Slate500,
                unselectedTextColor = Slate500,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.PLAYER,
            onClick = { onSelectScreen(AppScreen.PLAYER) },
            icon = {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Lecteur",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Lecteur",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == AppScreen.PLAYER) FontWeight.Bold else FontWeight.Medium
                )
            },
            modifier = Modifier.testTag("nav_player"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Slate500,
                unselectedTextColor = Slate500,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.LIBRARY,
            onClick = { onSelectScreen(AppScreen.LIBRARY) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Podcasts,
                    contentDescription = "Bibliothèque",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Épisodes",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == AppScreen.LIBRARY) FontWeight.Bold else FontWeight.Medium
                )
            },
            modifier = Modifier.testTag("nav_library"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Slate500,
                unselectedTextColor = Slate500,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.SETTINGS,
            onClick = { onSelectScreen(AppScreen.SETTINGS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Paramètres",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Voix & Clé",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == AppScreen.SETTINGS) FontWeight.Bold else FontWeight.Medium
                )
            },
            modifier = Modifier.testTag("nav_settings"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Slate500,
                unselectedTextColor = Slate500,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

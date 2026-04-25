package com.pitchpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pitchpulse.ui.screens.HomeScreen
import com.pitchpulse.ui.screens.MatchesScreen
import com.pitchpulse.ui.theme.LiveScoresTheme
import com.pitchpulse.ui.components.BottomNav
import com.pitchpulse.ui.components.NavItem

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pitchpulse.ui.viewmodel.MatchViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pitchpulse.ui.screens.MatchDetailScreen
import com.pitchpulse.ui.viewmodel.MatchDetailViewModel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.work.*
import com.pitchpulse.sync.SyncWorker
import java.util.concurrent.TimeUnit

sealed class Screen(val route: String) {
    object Home : Screen(NavItem.Home.route)
    object Matches : Screen(NavItem.Matches.route)
    object Search : Screen(NavItem.Search.route)
    object MatchDetail : Screen("match_detail_screen/{fixtureId}") {
        fun createRoute(fixtureId: Int) = "match_detail_screen/$fixtureId"
    }
    object TeamDetail : Screen("team_detail_screen/{teamId}") {
        fun createRoute(teamId: Int) = "team_detail_screen/$teamId"
    }
    object Settings : Screen("settings")
    object Document : Screen("document/{fileName}") {
        fun createRoute(fileName: String) = "document/$fileName"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleBackgroundSync()
        setContent {
            LiveScoresTheme {
                val context = LocalContext.current
                val viewModel: MatchViewModel = viewModel()

                // Notification Permission Request for Android 13+
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) {
                            // Permission granted
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val navController = rememberNavController()
                MainContent(navController, viewModel)
            }
        }
    }

    private fun scheduleBackgroundSync() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("LiveScoresSync")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LiveScoresSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}

@Composable
fun MainContent(navController: androidx.navigation.NavHostController, viewModel: MatchViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute in listOf(NavItem.Home.route, NavItem.Matches.route, NavItem.Search.route)) {
                BottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    uiState = uiState,
                    onMatchClick = { id -> 
                        navController.navigate(Screen.MatchDetail.createRoute(id))
                    },
                    onTeamClick = { id ->
                        navController.navigate(Screen.TeamDetail.createRoute(id))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Matches.route) {
                MatchesScreen(
                    uiState = uiState,
                    onDateSelected = { viewModel.selectDate(it) },
                    onMatchClick = { id -> 
                        navController.navigate(Screen.MatchDetail.createRoute(id))
                    },
                    onTeamClick = { id ->
                        navController.navigate(Screen.TeamDetail.createRoute(id))
                    }
                )
            }
            composable(Screen.Search.route) {
                com.pitchpulse.ui.screens.SearchScreen()
            }
            composable(
                route = Screen.MatchDetail.route,
                arguments = listOf(navArgument("fixtureId") { type = NavType.IntType })
            ) {
                val detailViewModel: MatchDetailViewModel = viewModel()
                MatchDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onTeamClick = { id ->
                        navController.navigate(Screen.TeamDetail.createRoute(id))
                    }
                )
            }
            composable(
                route = Screen.TeamDetail.route,
                arguments = listOf(navArgument("teamId") { type = NavType.IntType })
            ) {
                val teamViewModel: com.pitchpulse.ui.viewmodel.TeamDetailViewModel = viewModel()
                com.pitchpulse.ui.screens.TeamDetailScreen(
                    viewModel = teamViewModel,
                    onBack = { navController.popBackStack() },
                    onMatchClick = { id ->
                        navController.navigate(Screen.MatchDetail.createRoute(id))
                    },
                    onTeamClick = { id ->
                        navController.navigate(Screen.TeamDetail.createRoute(id))
                    }
                )
            }
            composable(Screen.Settings.route) {
                com.pitchpulse.ui.screens.SettingsScreen(
                    onNavigateToDocument = { fileName ->
                        navController.navigate(Screen.Document.createRoute(fileName))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Document.route,
                arguments = listOf(navArgument("fileName") { type = NavType.StringType })
            ) { backStackEntry ->
                val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
                com.pitchpulse.ui.screens.DocumentScreen(
                    fileName = fileName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
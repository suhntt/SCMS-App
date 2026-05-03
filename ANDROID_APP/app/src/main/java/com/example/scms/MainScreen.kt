package com.example.scms
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.CloudDone
import androidx.navigation.*
import androidx.navigation.compose.*

@Composable
fun MainScreen(initialComplaintId: String? = null, initialScreen: String? = null) {

    val navController = rememberNavController()
    
    // 🚀 DEEP LINK NAVIGATION
    LaunchedEffect(initialComplaintId, initialScreen) {
        if (initialComplaintId != null && initialScreen == "complaints") {
            navController.navigate("complaints?id=$initialComplaintId")
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val sessionManager = remember { SessionManager(context) }
    
    // 💡 PERSISTENCE: Check if we already have a session
    val startDest = remember {
        if (auth.currentUser != null && sessionManager.getUser() != null) {
            UserSession.currentUser = sessionManager.getUser()
            "home"
        } else {
            "login"
        }
    }

    var showOfflinePrompt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!NetworkUtils.isInternetAvailable(context)) {
            showOfflinePrompt = true
        }
    }

    // ✅ Bottom bar visible ONLY after login
    val bottomBarScreens = listOf(
        "home",
        "complaints?id={id}",
        "alerts",
        "leaderboard",
        "profile"
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = bottomBarScreens.contains(currentRoute)

    // 🛰️ GLOBAL SYNC MONITOR
    val syncCount by NotificationUtils.syncSuccessFlow.collectAsState()
    var showSyncSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(syncCount) {
        if (syncCount > 0) {
            showSyncSuccessDialog = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController)
            }
        }
    ) { paddingValues ->

        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(paddingValues)
        ) {

            // 🔐 LOGIN / SIGNUP
            composable("login") { LoginScreen(navController) }
            composable("signup") { SignupScreen(navController) }

            // 🏠 HOME
            composable("home") { HomeInfoScreen(navController) }

            // 📋 COMPLAINTS (Supports optional filtering by ID)
            composable(
                route = "complaints?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { backStackEntry ->
                val filteredId = backStackEntry.arguments?.getString("id")
                ComplaintsScreen(navController, filteredId)
            }

            // 🔔 ALERTS
            composable("alerts") { AlertsScreen(navController) }

            // 📷 CAMERA
            composable("camera") { CameraScreen(navController) }

            // 👤 PROFILE
            composable("profile") { ProfileScreen(navController) }

            // ➕ REPORT
            composable("report") { ReportComplaintScreen(navController) }

            // 🏆 LEADERBOARD
            composable("leaderboard") { LeaderboardScreen(navController) }

            // 🕒 TIMELINE
            composable(
                route = "timeline/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val complaintId = backStackEntry.arguments?.getString("id") ?: ""
                ComplaintTimelineScreen(navController, complaintId)
            }

            // 🚨 SOS EMERGENCY
            composable("sos") { SosScreen(navController) }

        }
    }

    // 🏆 GLOBAL SYNC SUCCESS DIALOG
    if (showSyncSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSyncSuccessDialog = false
                NotificationUtils.resetSyncFlow()
            },
            icon = { Icon(androidx.compose.material.icons.Icons.Default.CloudDone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) },
            title = { Text("Mission Certified! 🏆") },
            text = { Text("Your offline reports ($syncCount) have been successfully synchronized with the City Command Center and logged in the registry.") },
            confirmButton = {
                Button(
                    onClick = { 
                        showSyncSuccessDialog = false
                        NotificationUtils.resetSyncFlow()
                    }
                ) { Text("Awesome!") }
            }
        )
    }

    // 🔴 OFFLINE MODE PROMPT
    if (showOfflinePrompt) {
        AlertDialog(
            onDismissRequest = { showOfflinePrompt = false },
            title = { Text("No Internet Connection") },
            text = { Text("You have no internet. Go to the offline mode to submit a complaint?") },
            confirmButton = {
                Button(onClick = {
                    showOfflinePrompt = false
                    navController.navigate("report")
                }) {
                    Text("Yes, Offline Mode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOfflinePrompt = false }) {
                    Text("Ignore")
                }
            }
        )
    }
}

package com.example.scms
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun NavGraph(navController: NavHostController) {

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        // 🔐 AUTH
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignupScreen(navController) }

        // 🏠 HOME & DASHBOARD
        composable("home") { HomeInfoScreen(navController) }

        // 📋 COMPLAINTS (Supports optional filtering by ID)
        composable(
            route = "complaints?id={id}",
            arguments = listOf(navArgument("id") { nullable = true; defaultValue = null })
        ) { backStack ->
            val id = backStack.arguments?.getString("id")
            ComplaintsScreen(navController, id)
        }

        // 📝 REPORT
        composable("report") { ReportComplaintScreen(navController) }

        // 🚨 ALERTS
        composable("alerts") { AlertsScreen(navController) }

        // 🏆 LEADERBOARD
        composable("leaderboard") { LeaderboardScreen(navController) }

        // 🚨 SOS
        composable("sos") { SosScreen(navController) }

        // 👤 PROFILE
        composable("profile") { ProfileScreen(navController) }


        // 📊 TIMELINE
        composable(
            route = "timeline/{complaintId}",
            arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
        ) { backStack ->
            val complaintId = backStack.arguments?.getString("complaintId") ?: return@composable
            ComplaintTimelineScreen(navController, complaintId)
        }
    }
}

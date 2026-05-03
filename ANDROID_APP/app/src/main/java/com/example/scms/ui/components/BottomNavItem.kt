package com.example.scms.ui.components
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home        : BottomNavItem("home",        "Home",        Icons.Filled.Home)
    object Complaints  : BottomNavItem("complaints",  "Complaints",  Icons.Filled.List)
    object Alerts      : BottomNavItem("alerts",      "Alerts",      Icons.Filled.Notifications)
    object Leaderboard : BottomNavItem("leaderboard", "Leaderboard", Icons.Filled.EmojiEvents)
    object Profile     : BottomNavItem("profile",     "Profile",     Icons.Filled.Person)
}

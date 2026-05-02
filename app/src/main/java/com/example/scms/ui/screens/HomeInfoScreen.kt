package com.example.scms.ui.screens
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.scms.utils.NetworkObserver
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// SCMS Branding Tokens
private val AccentBlue = Color(0xFF003366)
private val GuardianGreen = Color(0xFF10B981)
private val WarningOrange = Color(0xFFF59E0B)
private val SOSRed = Color(0xFFEF4444)

data class Helpline(val name: String, val number: String, val icon: ImageVector, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeInfoScreen(navController: NavController) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    val user = UserSession.currentUser

    // 📶 Network Observer for Offline Mode
    val networkObserver = remember { NetworkObserver(context) }
    val networkStatus by networkObserver.observe.collectAsState(initial = NetworkObserver.Status.Available)
    var showOfflineDialog by remember { mutableStateOf(false) }

    LaunchedEffect(networkStatus) {
        if (networkStatus == NetworkObserver.Status.Lost) {
            delay(500) // 🕒 Small delay to ensure it's not a temporary flicker
            showOfflineDialog = true
        } else if (networkStatus == NetworkObserver.Status.Available) {
            showOfflineDialog = false // 🔄 Dismiss if internet returns
            scope.launch {
                com.example.scms.utils.OfflineComplaintManager.syncOfflineComplaints(context)
            }
        }
    }
    
    // Environment States
    var temp by remember { mutableStateOf("--") }
    var aqi by remember { mutableStateOf("--") }
    var humidity by remember { mutableStateOf("--") }
    var isWeatherLoading by remember { mutableStateOf(true) }

    val helplines = listOf(
        Helpline("Police", "100", Icons.Default.Shield, Color(0xFF3B82F6)),
        Helpline("Ambulance", "102", Icons.Default.MedicalServices, Color(0xFFEF4444)),
        Helpline("Fire Dept", "101", Icons.Default.FireTruck, Color(0xFFF97316)),
        Helpline("Women Help", "1091", Icons.Default.Face, Color(0xFFA855F7)),
        Helpline("Disaster", "108", Icons.Default.Warning, Color(0xFFF59E0B))
    )

    LaunchedEffect(Unit) {
        delay(1500) // 🕵️‍♂️ Simulate "Scanning City Vitals"
        temp = "32°C"
        aqi = "42 (Good)"
        humidity = "65%"
        isWeatherLoading = false
        isVisible = true
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        
        // 🌌 Background Glows
        Box(modifier = Modifier.size(500.dp).align(Alignment.TopEnd).offset(x = 200.dp, y = (-150).dp).blur(150.dp).background(AccentBlue.copy(alpha = 0.15f), CircleShape))
        Box(modifier = Modifier.size(400.dp).align(Alignment.CenterStart).offset(x = (-200).dp).blur(120.dp).background(GuardianGreen.copy(alpha = 0.1f), CircleShape))

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            
            HeaderSection(user, onProfileClick = { 
                navController.navigate("profile") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })

            /* 🌍 CITY SENTINEL (Weather & Air Quality) */
            EnvironmentSentinel(temp, aqi, humidity, isWeatherLoading)

            Spacer(Modifier.height(24.dp))

            /* 📊 CITY VITAL STATS */
            CityStatsSection()

            Spacer(Modifier.height(32.dp))

            /* 📞 RAPID RESPONSE DIRECTORY */
            Text(
                "24/7 EMERGENCY HELPLINES", 
                modifier = Modifier.padding(horizontal = 24.dp), 
                fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), letterSpacing = 2.sp
            )
            Spacer(Modifier.height(16.dp))
            HelplineCarousel(helplines)

            Spacer(Modifier.height(32.dp))

            /* ⚡ CORE ACTIONS GRID */
            Text(
                "CITY SERVICES", 
                modifier = Modifier.padding(horizontal = 24.dp), 
                fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), letterSpacing = 2.sp
            )
            Spacer(Modifier.height(16.dp))
            ActionGrid(navController)

            Spacer(Modifier.height(32.dp))

            /* 📖 SMART CITIZEN GUIDE (How to Use) */
            SmartCitizenGuide()

            Spacer(Modifier.height(32.dp))

            /* 🏆 TOP CONTRIBUTORS PREVIEW */
            LeaderboardPreview(onViewAll = { navController.navigate("leaderboard") })

            Spacer(Modifier.height(100.dp))
        }
    }

    // 📶 OFFLINE MODE DIALOG
    if (showOfflineDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineDialog = false },
            icon = { Icon(Icons.Default.WifiOff, null, tint = Color.Red, modifier = Modifier.size(40.dp)) },
            title = { Text("Connection Lost 📡", fontWeight = FontWeight.Bold) },
            text = { 
                Text("You are currently offline. Would you like to switch to 'Offline Mode' to report issues locally? They will sync automatically later.") 
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showOfflineDialog = false
                        navController.navigate("report") 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Go to Offline Mode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOfflineDialog = false }) {
                    Text("Stay Here", color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun HeaderSection(user: User?, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("SCMS PORTAL", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Text(
                text = "Welcome, ${user?.name?.split(" ")?.firstOrNull() ?: "Citizen"}",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
        }
        
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).clickable { onProfileClick() }
        ) {
            AsyncImage(
                model = user?.profile_picture ?: "https://api.dicebear.com/9.x/avataaars/png?seed=${user?.name ?: "User"}",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun EnvironmentSentinel(temp: String, aqi: String, humidity: String, isLoading: Boolean) {
    Card(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("LIVE CITY VITALS", color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text(if (isLoading) "Scanning..." else "Air Quality is Good", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                EnvItem(Icons.Default.Thermostat, temp)
                EnvItem(Icons.Default.Air, aqi.split(" ")[0])
            }
        }
    }
}

@Composable
fun EnvItem(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun CityStatsSection() {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatPill(Modifier.weight(1f), "Active", "24", WarningOrange)
        StatPill(Modifier.weight(1f), "Solved", "1.2k", GuardianGreen)
        StatPill(Modifier.weight(1f), "Safety", "98%", AccentBlue)
    }
}

@Composable
fun StatPill(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun HelplineCarousel(helplines: List<Helpline>) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(helplines) { line ->
            Card(
                modifier = Modifier.width(160.dp).height(120.dp).clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${line.number}"))
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(44.dp).background(line.color.copy(alpha=0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(line.icon, null, tint = line.color, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(line.name, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                        Text(line.number, color = line.color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionGrid(navController: NavController) {
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(Modifier.weight(1f), "Report", "Snap & Submit", Icons.Default.AddAPhoto, Color(0xFF6366F1)) {
                navController.navigate("report")
            }
            ActionCard(Modifier.weight(1f), "History", "My Complaints", Icons.Default.FactCheck, Color(0xFF10B981)) {
                navController.navigate("complaints")
            }
        }
    }
}

@Composable
fun ActionCard(modifier: Modifier, title: String, sub: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier.height(140.dp).clickable { 
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick() 
        },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp, letterSpacing = (-0.5).sp)
                Text(sub, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SmartCitizenGuide() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            "THE CITIZEN'S MISSION", 
            fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), letterSpacing = 2.sp
        )
        Spacer(Modifier.height(16.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GuideStepLong(
                "01", 
                "REPORTING THE ISSUE", 
                "Open the camera, snap a clear photo, and our AI will automatically detect the category. Add a description and submit.",
                Icons.Default.CameraAlt,
                AccentBlue
            )
            GuideStepLong(
                "02", 
                "GOVERNMENT RESOLUTION", 
                "Your report is instantly sent to the city admin. They assign it to the correct department (like PWD or Electricity) for a physical fix.",
                Icons.Default.Engineering,
                GuardianGreen
            )
            GuideStepLong(
                "03", 
                "EARNING YOUR REWARDS", 
                "Once your issue is marked 'Resolved', you earn 2 reward points. Upvotes from other citizens also help you rank up to 'Legendary Guardian'!",
                Icons.Default.MilitaryTech,
                Color(0xFFFFD700)
            )
        }
    }
}

@Composable
fun GuideStepLong(step: String, title: String, desc: String, icon: ImageVector, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(step, color = color, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(4.dp))
                Text(desc, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun LeaderboardPreview(onViewAll: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().clickable { 
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onViewAll() 
        },
        color = Color(0xFF1E1B4B),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFD700), modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("CITY RANKINGS", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
                Text("View Your Global Rank", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
        }
    }
}

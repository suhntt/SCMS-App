package com.example.scms.ui.screens
import com.example.scms.*
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

private val Gold = Color(0xFFFFD700)
private val AccentBlue = Color(0xFF003366)
private val GuardianGreen = Color(0xFF10B981)
private val LegendPurple = Color(0xFFA855F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // ─── Live state ────────────────────────────────────
    var user by remember { mutableStateOf(UserSession.currentUser) }
    var points by remember { mutableIntStateOf(user?.points ?: 0) }
    var myRank by remember { mutableIntStateOf(0) }
    var myStats by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(true) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Real-world Rank Logic
    val rankColor = when {
        points >= 1000 -> LegendPurple
        points >= 500 -> GuardianGreen
        else -> Gold
    }
    val rankTitle = when {
        points >= 1000 -> "LEGENDARY GUARDIAN"
        points >= 500 -> "CITY PROTECTOR"
        points >= 100 -> "SENIOR CITIZEN"
        else -> "ACTIVE CITIZEN"
    }

    val animatedPoints by animateIntAsState(
        targetValue = points,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "points_anim"
    )

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isRefreshing = true

                // ✅ STEP 1: Try Firestore directly (Firebase UID — always reliable)
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    val firestore = FirebaseFirestore.getInstance()
                    val doc = firestore.collection("users")
                        .document(firebaseUser.uid)
                        .get()
                        .await()

                    if (doc.exists()) {
                        val data = doc.data!!
                        val freshUser = User(
                            id = (data["id"] as? Long)?.toInt() ?: user?.id ?: 0,
                            firebaseUid = firebaseUser.uid,
                            name = data["name"] as? String ?: "Citizen",
                            phone = data["phone"] as? String ?: user?.phone ?: "",
                            email = data["email"] as? String ?: "",
                            profile_picture = data["profile_picture"] as? String,
                            points = (data["points"] as? Long)?.toInt() ?: 0,
                            badgeLevel = data["badgeLevel"] as? String ?: "Citizen",
                            district = data["district"] as? String
                        )
                        user = freshUser
                        points = freshUser.points
                        UserSession.currentUser = freshUser
                        SessionManager(context).saveUser(freshUser)
                    }
                } else if (UserSession.currentUser?.id != null) {
                    // ✅ STEP 2: Fallback — fetch via backend API if not logged into Firebase
                    val currentUserId = UserSession.currentUser!!.id
                    val resp = RetrofitClient.api.getUserPoints(currentUserId)
                    if (resp.isSuccessful) {
                        val data = resp.body()
                        if (data != null) {
                            val freshUser = User(
                                id = data.id ?: currentUserId,
                                name = data.name ?: "Citizen",
                                phone = user?.phone ?: "",
                                email = data.email ?: "",
                                profile_picture = data.profile_picture,
                                points = data.points,
                                badgeLevel = data.badgeLevel ?: "Citizen",
                                district = user?.district
                            )
                            user = freshUser
                            points = freshUser.points
                            UserSession.currentUser = freshUser
                            SessionManager(context).saveUser(freshUser)
                        }
                    }
                }

                // ✅ STEP 3: Leaderboard (optional, non-blocking)
                try {
                    val board = RetrofitClient.api.getLeaderboard()
                    leaderboard = board.take(3)
                    val currentId = UserSession.currentUser?.id
                    val entry = board.find { it.id == currentId }
                    if (entry != null) {
                        myStats = entry
                        myRank = board.indexOf(entry) + 1
                    }
                } catch (_: Exception) {}

            } catch (_: Exception) {
            } finally {
                isRefreshing = false
            }
        }
    }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isUploadingPhoto = true
                try {
                    val rawBytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (rawBytes != null) {
                        val originalBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                        val out = ByteArrayOutputStream()
                        originalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                        val part = okhttp3.MultipartBody.Part.createFormData("photo", "profile.jpg", out.toByteArray().toRequestBody("image/jpeg".toMediaTypeOrNull()))
                        val currentUserId = UserSession.currentUser?.id
                        if (currentUserId != null) {
                            val response = RetrofitClient.api.updateProfile(currentUserId.toString(), part)
                            if (response.isSuccessful) {
                                val updatedUser = user?.copy(profile_picture = response.body()?.profile_picture)
                                user = updatedUser
                                UserSession.currentUser = updatedUser
                                // ✅ PERSIST SESSION
                                if (updatedUser != null) SessionManager(context).saveUser(updatedUser)
                            }
                        }
                    }
                } catch (_: Exception) {} finally { isUploadingPhoto = false }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Aesthetic Glows
        Box(modifier = Modifier.size(400.dp).align(Alignment.TopStart).offset(x = (-150).dp, y = (-100).dp).blur(120.dp).background(rankColor.copy(alpha = 0.15f), CircleShape))

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            
            /* ---------- TOP HEADER ---------- */
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                AsyncImage(
                    model = user?.profile_picture ?: "https://images.unsplash.com/photo-1557683316-973673baf926",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(30.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.25f
                )
                
                Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(110.dp).clip(CircleShape).border(3.dp, Brush.sweepGradient(listOf(rankColor, AccentBlue, rankColor)), CircleShape)
                            .shadow(25.dp, CircleShape).clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = user?.profile_picture ?: "https://api.dicebear.com/9.x/avataaars/png?seed=${user?.name?.replace(" ", "") ?: "User"}",
                            contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop
                        )
                        if (isUploadingPhoto) CircularProgressIndicator(color = rankColor, modifier = Modifier.size(40.dp))
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text(user?.name ?: "Citizen", color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Surface(
                    color = rankColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, rankColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        rankTitle, 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = rankColor, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        /* ---------- CREDITS SECTION ---------- */
            Column(modifier = Modifier.padding(horizontal = 24.dp).offset(y = (-40).dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("TOTAL REWARD POINTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), letterSpacing = 1.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$animatedPoints", fontSize = 38.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(" pts", fontSize = 16.sp, color = Gold, modifier = Modifier.padding(bottom = 6.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(modifier = Modifier.size(56.dp).background(Gold.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Stars, null, tint = Gold, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // XP Progress bar
                val progress = (points % 100) / 100f
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Next Level Progress", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp)
                        Text("${(progress * 100).toInt()}%", color = rankColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = rankColor, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            /* ---------- 📊 REAL-TIME STATS ---------- */
            Row(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox(Modifier.weight(1f), "REPORTS", "${myStats?.total_complaints ?: 0}", Icons.Default.Description, Color(0xFF3B82F6))
                StatBox(Modifier.weight(1f), "RESOLVED", "${myStats?.resolved_complaints ?: 0}", Icons.Default.CheckCircle, Color(0xFF10B981))
                StatBox(Modifier.weight(1f), "GLOBAL RANK", "#${if (myRank > 0) myRank else "--"}", Icons.Default.Public, Color(0xFFF59E0B))
            }

            /* ---------- ⚙️ ACTION CENTER ---------- */
            Card(
                modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                ActionItem(Icons.Default.EmojiEvents, "Global Leaderboard", "View rankings of all city guardians", Gold) {
                    navController.navigate("leaderboard")
                }
                ActionItem(Icons.Default.Badge, "Personal Information", "View your official ID & contact", rankColor) { showInfoDialog = true }
                ActionItem(Icons.Default.Assignment, "My Activity", "See all reports filed by you", Color(0xFF3B82F6)) { 
                    navController.navigate("complaints?id=${user?.id}") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                ActionItem(Icons.Default.NotificationsActive, "City Alerts", "View emergency notifications", Color.Red) { 
                    navController.navigate("alerts") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                ActionItem(Icons.Default.Logout, "Sign Out", "Safely exit your account", MaterialTheme.colorScheme.error) {
                    showLogoutDialog = true
                }
                }
            }

            Spacer(Modifier.height(50.dp))
        }
    }

    // 🚪 LOGOUT CONFIRMATION DIALOG
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of your SCMS account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        UserSession.currentUser = null
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }

    // 🏆 PERSONAL INFO DIALOG
    if (showInfoDialog) {
        Dialog(onDismissRequest = { showInfoDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp), 
                shape = RoundedCornerShape(28.dp), 
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Identity Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(20.dp))
                    InfoRow("Full Name", user?.name ?: "N/A")
                    InfoRow("SCMS ID", "#${user?.id ?: "---"}")
                    InfoRow("Mobile", user?.phone ?: "N/A")
                    InfoRow("Email", user?.email ?: "N/A")
                    InfoRow("Current Rank", rankTitle)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showInfoDialog = false }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = rankColor)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
        Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StatBox(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = modifier, 
        shape = RoundedCornerShape(24.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), letterSpacing = 1.sp)
        }
    }
}

@Composable
fun ActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(42.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
    }
}

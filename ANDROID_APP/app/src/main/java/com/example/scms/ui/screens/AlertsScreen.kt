package com.example.scms.ui.screens
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.navigation.NavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.speech.tts.TextToSpeech
import android.content.Intent
import java.util.*

// ─── Data class for an alert ─────────────────────────────────────
data class Alert(
    val alertId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "info",          // "warning" | "danger" | "info"
    val area: String = "",
    val reporterName: String = "",
    val complaintId: String = "",
    val photoUrl: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val created_at: String? = null
)

fun formatTo12Hour(isoDate: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(isoDate)
        val outputFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
        outputFormat.format(date ?: java.util.Date())
    } catch (e: Exception) {
        isoDate
    }
}

private val GoldCol  = Color(0xFFFFD700)
private val RedAlert = Color(0xFFEF4444)
private val OrangeW  = Color(0xFFF97316)
private val BlueInfo = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(navController: NavController) {

    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastRefreshed by remember { mutableStateOf("") }
    
    // User's current location for 10km filtering
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLon by remember { mutableStateOf<Double?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val fusedLocationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }

    // Pulse animation for the live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // TTS Setup
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        tts.value = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale.US
            }
        }
        onDispose {
            tts.value?.stop()
            tts.value?.shutdown()
        }
    }

    fun loadAlerts() {
        scope.launch {
            try {
                // ✅ Filter alerts by user's district — system alerts still shown to everyone
                val userDistrict = UserSession.currentUser?.district
                val rawAlerts = RetrofitClient.api.getAlerts(district = userDistrict)
                alerts = rawAlerts
                val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
                lastRefreshed = "Updated at $now${if (userDistrict != null) " · $userDistrict" else ""}"
                error = null
            } catch (e: Exception) {
                error = "Could not load alerts. Check your connection."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        // First try to grab location, then load alerts
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    userLat = loc.latitude
                    userLon = loc.longitude
                }
                loadAlerts()
            }.addOnFailureListener {
                loadAlerts() // proceed without location if it fails
            }
        } catch(e: SecurityException) {
            loadAlerts() // proceed without location if no permission
        }
        
        // Auto-refresh every 30 seconds
        while (true) {
            delay(30_000)
            loadAlerts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = GoldCol,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Alerts & Incidents", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Live indicator or other actions can go here
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        when {
            isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoldCol)
                        Spacer(Modifier.height(12.dp))
                        Text("Fetching live alerts...", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Warning, null, tint = OrangeW, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(error!!, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { isLoading = true; loadAlerts() },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueInfo)
                        ) { Text("Retry") }
                    }
                }
            }

            alerts.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No active alerts in your area",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Stay safe! We'll notify you when something comes up.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                        if (lastRefreshed.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text(lastRefreshed, color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp)
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary banner
                    item {
                        AlertSummaryBanner(alerts = alerts, lastRefreshed = lastRefreshed)
                        Spacer(Modifier.height(4.dp))
                    }

                    items(alerts) { alert ->
                        AlertCard(
                            alert = alert,
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "🚨 SCMS EMERGENCY ALERT: ${alert.title}\n\n${alert.message}\n\nLocation: ${alert.area}\n\nStay safe!")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            onSpeak = {
                                tts.value?.speak(
                                    "${alert.title}. ${alert.message}",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    null
                                )
                            },
                            onClick = {
                                if (alert.complaintId.isNotEmpty()) {
                                    navController.navigate("complaints?id=${alert.complaintId}")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertSummaryBanner(alerts: List<Alert>, lastRefreshed: String) {
    val danger  = alerts.count { it.type == "danger" }
    val warning = alerts.count { it.type == "warning" }
    val info    = alerts.count { it.type == "info" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1E3A5F), Color(0xFF162E4A))
                )
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                "${alerts.size} Active Alert${if (alerts.size != 1) "s" else ""}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (danger > 0) SummaryChip("🔴 $danger Danger", RedAlert)
                if (warning > 0) SummaryChip("🟠 $warning Warning", OrangeW)
                if (info > 0) SummaryChip("🔵 $info Info", BlueInfo)
            }
            if (lastRefreshed.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(lastRefreshed, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AlertCard(
    alert: Alert,
    onShare: () -> Unit = {},
    onSpeak: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val (accentColor, iconVec, bgGrad) = when (alert.type) {
        "danger"  -> Triple(
            RedAlert,
            Icons.Filled.Warning,
            listOf(Color(0xFF450A0A), Color(0xFF180808))
        )
        "warning" -> Triple(
            OrangeW,
            Icons.Filled.Warning,
            listOf(Color(0xFF431407), Color(0xFF1C0D0D))
        )
        else -> Triple(
            BlueInfo,
            Icons.Filled.Info,
            listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(bgGrad))
        ) {
            // High-Impact Banner on top if Danger
            if (alert.type == "danger") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(accentColor.copy(alpha = 0.2f))
                        .align(Alignment.TopStart)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconVec, null, tint = accentColor, modifier = Modifier.size(32.dp))
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title.uppercase(),
                            color = accentColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        )
                        if (alert.area.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(alert.area, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }
                    }
                    
                    IconButton(onClick = onSpeak) {
                        Icon(Icons.Filled.VolumeUp, null, tint = Color.White.copy(0.4f))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = alert.message,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )

                Spacer(Modifier.height(20.dp))

                // Safety Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (alert.complaintId.isNotEmpty()) {
                            Button(
                                onClick = onClick,
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Take Action", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = onShare,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    alert.created_at?.let { ts ->
                        Text(
                            formatTo12Hour(ts),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

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

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.util.*

private val Gold   = Color(0xFFFFD700)
private val Silver = Color(0xFFC0C0C0)
private val Bronze = Color(0xFFCD7F32)
private val Navy   = Color(0xFF0F1B2D)
private val DeepBlue = Color(0xFF1A2E4A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var entries   by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error     by remember { mutableStateOf<String?>(null) }
    val currentUserId = UserSession.currentUser?.id ?: -1

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                entries = RetrofitClient.api.getLeaderboard()
            } catch (e: Exception) {
                error = "Could not load leaderboard"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = Gold, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("City Hall of Fame", fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, 
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Gold) }
            error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(error!!, color = MaterialTheme.colorScheme.error) }
            entries.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("No entries yet.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    item { LeaderboardHero(entries.size) }
                    
                    if (entries.size >= 3) {
                        item {
                            PodiumCard(first = entries[0], second = entries[1], third = entries[2], currentUserId = currentUserId)
                        }
                    }

                    item {
                        Text(
                            "TOP CONTRIBUTORS",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    val restStart = if (entries.size >= 3) 3 else 0
                    itemsIndexed(entries.drop(restStart)) { idx, entry ->
                        LeaderboardRow(rank = restStart + idx + 1, entry = entry, isCurrentUser = entry.id == currentUserId)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardHero(count: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().height(160.dp).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), Color.Transparent))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GLOBAL RANKINGS", color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 2.sp)
            Text("The Civic Guardians", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("Tracking the impact of $count dedicated citizens", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun PodiumCard(first: LeaderboardEntry, second: LeaderboardEntry, third: LeaderboardEntry, currentUserId: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(0.7f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "a")

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumPillar(second, 2, Silver, 100.dp, 1f, second.id == currentUserId)
        PodiumPillar(first, 1, Gold, 140.dp, alpha, first.id == currentUserId)
        PodiumPillar(third, 3, Bronze, 80.dp, 1f, third.id == currentUserId)
    }
}

@Composable
private fun PodiumPillar(entry: LeaderboardEntry, rank: Int, color: Color, height: androidx.compose.ui.unit.Dp, alpha: Float, isMe: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(entry.name.split(" ")[0], color = if (isMe) Gold else MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("${entry.points} pts", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.size(50.dp).background(color.copy(alpha = alpha), CircleShape), contentAlignment = Alignment.Center) {
            Text(entry.name.first().toString(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.width(80.dp).height(height).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Brush.verticalGradient(listOf(color.copy(0.4f), color.copy(0.1f)))), contentAlignment = Alignment.Center) {
            Text("#$rank", color = color, fontWeight = FontWeight.Black, fontSize = 24.sp)
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: LeaderboardEntry, isCurrentUser: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrentUser) androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(0.3f)) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#$rank", color = MaterialTheme.colorScheme.onSurface.copy(0.4f), fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Text(entry.name.first().toString(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, color = if (isCurrentUser) Gold else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                val badge = when {
                    entry.points > 1000 -> "🛡️ City Guardian"
                    entry.points > 500 -> "⚔️ Civic Hero"
                    else -> "🌱 Citizen"
                }
                Text(badge, color = Gold.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat(Icons.Default.Edit, "${entry.total_complaints} Reports")
                    MiniStat(Icons.Default.ThumbUp, "${entry.total_upvotes}")
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.points}", color = if (isCurrentUser) Gold else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("POINTS", color = MaterialTheme.colorScheme.onSurface.copy(0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 11.sp)
    }
}

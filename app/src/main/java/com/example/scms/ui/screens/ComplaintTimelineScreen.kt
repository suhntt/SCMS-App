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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Data model — mirrors what the backend returns in status_history[]
// ─────────────────────────────────────────────────────────────────────────────
data class StatusEvent(
    val status: String,
    val timestamp: String?,
    val note: String?
)

data class ComplaintDetail(
    val id: Int,
    val category: String?,
    val description: String?,
    val address: String?,
    val status: String?,
    val department: String?,
    val created_at: String?,
    val photo_url: String?,
    val reporter_name: String?,
    val statusHistory: List<StatusEvent> = emptyList()
)

// ─────────────────────────────────────────────────────────────────────────────
// Ordered pipeline — every complaint moves through these stages in order
// ─────────────────────────────────────────────────────────────────────────────
private val TIMELINE_STEPS = listOf(
    Triple("Pending",     Icons.Filled.UploadFile,    "Submitted"),
    Triple("Verified",    Icons.Filled.FactCheck,     "Verified"),
    Triple("Assigned",    Icons.Filled.AssignmentInd, "Assigned"),
    Triple("In Progress", Icons.Filled.Construction,  "In Progress"),
    Triple("Resolved",    Icons.Filled.CheckCircle,   "Resolved")
)

private val stepColors = listOf(
    Color(0xFF6366F1), // indigo  — Submitted
    Color(0xFF3B82F6), // blue    — Verified
    Color(0xFFF59E0B), // amber   — Assigned
    Color(0xFFEF4444), // red     — In Progress
    Color(0xFF10B981)  // green   — Resolved
)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintTimelineScreen(navController: NavController, complaintId: String) {

    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<ComplaintDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var lastRefresh by remember { mutableStateOf("") }

    // ── Helper: load complaint + its history ──
    suspend fun load() {
        try {
            // Fetch base complaints list and filter by id
            val list = RetrofitClient.api.getComplaints()
            val c = list.firstOrNull { it.id.toString() == complaintId }
            if (c != null) {
                // Fetch full status history from dedicated endpoint
                val historyResult = runCatching {
                    RetrofitClient.api.getComplaintHistory(c.id)
                }
                val history = historyResult.getOrDefault(emptyList()).map { h ->
                    StatusEvent(
                        status    = h.status,
                        timestamp = h.timestamp,
                        note      = h.note
                    )
                }
                detail = ComplaintDetail(
                    id = c.id,
                    category = c.category,
                    description = c.description,
                    address = c.address,
                    status = c.status,
                    department = c.department,
                    created_at = c.created_at,
                    photo_url = c.photo_url,
                    reporter_name = c.reporter_name,
                    statusHistory = history
                )
                lastRefresh = java.text.SimpleDateFormat(
                    "hh:mm:ss a", java.util.Locale.getDefault()
                ).format(java.util.Date())
            } else {
                errorMsg = "Complaint #$complaintId not found"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = "Could not load timeline. Is the server running?"
        } finally {
            isLoading = false
        }
    }

    // ── Initial load ──
    LaunchedEffect(complaintId) { load() }

    // ── Real-time polling every 30 seconds (Battery Optimized) ──
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            load()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Complaint Timeline", fontWeight = FontWeight.Bold)
                        if (lastRefresh.isNotEmpty()) {
                            Text(
                                "Live · Last updated $lastRefresh",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { isLoading = true; load() } }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->

        when {
            isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Loading Timeline…", style = MaterialTheme.typography.bodySmall)
                }
            }

            errorMsg != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { scope.launch { errorMsg = null; isLoading = true; load() } }) {
                        Text("Retry")
                    }
                }
            }

            detail != null -> TimelineContent(
                detail = detail!!,
                padding = padding
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main timeline content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TimelineContent(
    detail: ComplaintDetail,
    padding: PaddingValues
) {
    val currentStepIndex = TIMELINE_STEPS.indexOfFirst {
        it.first.equals(detail.status, ignoreCase = true)
    }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        // ── Complaint summary card ──
        ComplaintSummaryCard(detail)

        Spacer(Modifier.height(24.dp))

        // ── Section title ──
        Text(
            "Progress Timeline",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Real-time status tracked by SCMS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(20.dp))

        // ── Timeline steps ──
        TIMELINE_STEPS.forEachIndexed { index, (statusKey, icon, label) ->
            val isDone    = index <= currentStepIndex
            val isActive  = index == currentStepIndex
            val isLast    = index == TIMELINE_STEPS.lastIndex

            // Find matching history event (if any)
            val event = detail.statusHistory.firstOrNull {
                it.status.equals(statusKey, ignoreCase = true)
            }
            // For "Pending" step use created_at as fallback timestamp
            val timestamp = event?.timestamp
                ?: if (index == 0) detail.created_at else null

            TimelineStep(
                index       = index,
                icon        = icon,
                label       = label,
                statusKey   = statusKey,
                isDone      = isDone,
                isActive    = isActive,
                isLast      = isLast,
                timestamp   = timestamp,
                note        = event?.note,
                activeColor = stepColors[index]
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Department badge ──
        if (!detail.department.isNullOrBlank()) {
            DepartmentBadge(department = detail.department)
            Spacer(Modifier.height(16.dp))
        }

        // ── Reassuring footer ──
        LiveSyncBadge()
        Spacer(Modifier.height(32.dp))

        // ── GRIEVANCE REDRESSAL (ESCALATION) ──
        if (detail.status?.lowercase() == "resolved") {
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            val scope = rememberCoroutineScope()
            var isEscalating by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    isEscalating = true
                    scope.launch {
                        try {
                            RetrofitClient.api.updateComplaintStatus(
                                id = detail.id,
                                body = mapOf("status" to "Escalated", "note" to "CITIZEN ESCALATED: Reported issue is not actually fixed.")
                            )
                        } catch (e: Exception) {}
                    }
                },
                enabled = !isEscalating,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isEscalating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("NOT FIXED? RE-OPEN & ESCALATE", fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Complaint summary card at top
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ComplaintSummaryCard(detail: ComplaintDetail) {
    val statusColor = when (detail.status?.lowercase()) {
        "resolved"    -> Color(0xFF10B981)
        "in progress" -> Color(0xFFEF4444)
        "assigned"    -> Color(0xFFF59E0B)
        "verified"    -> Color(0xFF3B82F6)
        else          -> Color(0xFF6366F1)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = detail.category ?: "Unknown Issue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: SCMS-AS-2026-${detail.id}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = detail.status ?: "Pending",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (!detail.address.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = detail.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (!detail.reporter_name.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Reported by ${detail.reporter_name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// A single step in the timeline
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TimelineStep(
    index: Int,
    icon: ImageVector,
    label: String,
    statusKey: String,
    isDone: Boolean,
    isActive: Boolean,
    isLast: Boolean,
    timestamp: String?,
    note: String?,
    activeColor: Color
) {
    val iconColor by animateColorAsState(
        targetValue = if (isDone) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
        animationSpec = tween(500), label = "iconColor"
    )
    val circleScale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = tween(400), label = "circleScale"
    )

    Row(modifier = Modifier.fillMaxWidth()) {

        // ── Left column: circle + connector line ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .scale(circleScale)
                    .clip(CircleShape)
                    .background(
                        if (isDone)
                            Brush.radialGradient(listOf(iconColor.copy(alpha = 0.25f), iconColor.copy(alpha = 0.05f)))
                        else
                            Brush.radialGradient(listOf(Color.Transparent, Color.Transparent))
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(
                            if (isDone && index < TIMELINE_STEPS.size - 1
                                && TIMELINE_STEPS.indexOfFirst { it.first == statusKey } < TIMELINE_STEPS.size - 1)
                                iconColor.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // ── Right column: label + timestamp + note ──
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp, bottom = if (!isLast) 0.dp else 0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.ExtraBold else if (isDone) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
                if (isActive) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(iconColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Current",
                            color = iconColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (timestamp != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatTo12Hour(timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDone) iconColor.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            } else if (!isDone) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Awaiting...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            }

            if (!note.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(if (!isLast) 8.dp else 0.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Department badge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DepartmentBadge(department: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2563EB).copy(alpha = 0.08f)
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Business,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Assigned Department",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2563EB).copy(alpha = 0.7f)
                )
                Text(
                    department,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Live sync indicator
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LiveSyncBadge() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Auto-refreshes every 10 seconds",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}

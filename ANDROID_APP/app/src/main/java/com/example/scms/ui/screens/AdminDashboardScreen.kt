package com.example.scms.ui.screens
import com.example.scms.*
import com.example.scms.ui.components.*
import com.example.scms.data.model.*
import com.example.scms.data.network.*
import com.example.scms.utils.*
import com.example.scms.service.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

// ─────────────────────────── Colour palette ───────────────────────────
private val DarkBg        = Color(0xFF0F172A)
private val CardBg        = Color(0xFF1E293B)
private val AccentBlue    = Color(0xFF3B82F6)
private val PendingAmber  = Color(0xFFF59E0B)
private val VerifiedPurple= Color(0xFF8B5CF6)
private val InProgressCyan= Color(0xFF06B6D4)
private val ResolvedGreen = Color(0xFF10B981)
private val EscalatedRed  = Color(0xFFEF4444)
private val TextPrimary   = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)

// Status column definitions
data class StatusColumn(
    val label: String,
    val apiStatus: String,
    val color: Color,
    val icon: ImageVector
)

val STATUS_COLUMNS = listOf(
    StatusColumn("Submitted",    "Pending",             PendingAmber,   Icons.Default.Inbox),
    StatusColumn("Verified",     "Verified",            VerifiedPurple, Icons.Default.VerifiedUser),
    StatusColumn("In Progress",  "In Progress",         InProgressCyan, Icons.Default.Build),
    StatusColumn("Resolved",     "Resolved",            ResolvedGreen,  Icons.Default.CheckCircle),
    StatusColumn("Escalated",    "Escalated",           EscalatedRed,   Icons.Default.Warning),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var complaints      by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading       by remember { mutableStateOf(true) }
    var detailComplaint by remember { mutableStateOf<Complaint?>(null) }
    var snackMsg        by remember { mutableStateOf<String?>(null) }
    val snackbarHost    = remember { SnackbarHostState() }
    val pagerState      = rememberPagerState(pageCount = { STATUS_COLUMNS.size })

    fun loadData() {
        scope.launch {
            try {
                isLoading = true
                complaints = RetrofitClient.api.getComplaints()
            } catch (_: Exception) {
            } finally { isLoading = false }
        }
    }

    fun updateStatus(complaint: Complaint, newStatus: String, note: String = "") {
        scope.launch {
            try {
                val body = mutableMapOf("status" to newStatus)
                if (note.isNotBlank()) body["note"] = note
                RetrofitClient.api.updateComplaintStatus(complaint.id, body)
                snackMsg = "✅ ${complaint.id.take(6)}… → $newStatus"
                loadData()
                detailComplaint = null
            } catch (_: Exception) {
                snackMsg = "❌ Failed to update status"
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            snackMsg = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = DarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COMMAND CENTER", fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp, fontSize = 16.sp, color = TextPrimary)
                        Text("Admin Dashboard", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        if (isLoading)
                            CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp, color = AccentBlue)
                        else
                            Icon(Icons.Default.Refresh, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CardBg)
            )
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Stats HUD ─────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(STATUS_COLUMNS) { col ->
                    val count = complaints.count {
                        it.status?.equals(col.apiStatus, ignoreCase = true) == true
                    }
                    AdminHudCard(col.label, count, col.color, col.icon)
                }
                // Total card
                item {
                    AdminHudCard("TOTAL", complaints.size, AccentBlue, Icons.Default.Assessment)
                }
            }

            // ── Column Tabs ────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = CardBg,
                contentColor = TextPrimary,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = STATUS_COLUMNS[pagerState.currentPage].color
                    )
                }
            ) {
                STATUS_COLUMNS.forEachIndexed { index, col ->
                    val count = complaints.count {
                        it.status?.equals(col.apiStatus, ignoreCase = true) == true
                    }
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        selectedContentColor = col.color,
                        unselectedContentColor = TextSecondary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(col.icon, null, modifier = Modifier.size(16.dp))
                            Text(col.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (count > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = col.color.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        count.toString(),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = col.color
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Pager ─────────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val col = STATUS_COLUMNS[page]
                val filtered = complaints.filter {
                    it.status?.equals(col.apiStatus, ignoreCase = true) == true
                }

                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(col.icon, null, tint = col.color.copy(0.3f),
                                modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No ${col.label} complaints", color = TextSecondary,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { complaint ->
                            AdminComplaintRow(
                                complaint = complaint,
                                accentColor = col.color,
                                onClick = { detailComplaint = complaint }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Full Detail Sheet ──────────────────────────────────────────────
    detailComplaint?.let { complaint ->
        Dialog(onDismissRequest = { detailComplaint = null }) {
            AdminDetailSheet(
                complaint = complaint,
                onDismiss = { detailComplaint = null },
                onStatusChange = { newStatus, note -> updateStatus(complaint, newStatus, note) }
            )
        }
    }
}

// ─────────────────────────── HUD Card ───────────────────────────────
@Composable
fun AdminHudCard(label: String, count: Int, color: Color, icon: ImageVector) {
    val animCount by animateIntAsState(count, tween(600), label = "count")
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(animCount.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        }
    }
}

// ─────────────────────────── Complaint Row Card ─────────────────────
@Composable
fun AdminComplaintRow(complaint: Complaint, accentColor: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, accentColor.copy(0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Row 1 — Category + Status badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Report, null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        complaint.category ?: "Unknown Category",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "ID: ${complaint.id.take(8)}…",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                StatusBadge(complaint.status ?: "Pending")
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(0.06f))
            Spacer(Modifier.height(10.dp))

            // Detail columns — grid layout
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailColumn(Modifier.weight(1f), Icons.Default.Person,      "REPORTER", complaint.reporter_name ?: "Unknown")
                DetailColumn(Modifier.weight(1f), Icons.Default.LocationOn,  "DISTRICT", complaint.district ?: "Unknown")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailColumn(Modifier.weight(1f), Icons.Default.CalendarToday,"SUBMITTED", complaint.created_at?.take(10) ?: "-")
                DetailColumn(Modifier.weight(1f), Icons.Default.Business,    "DEPT",     complaint.department ?: "Unassigned")
            }
            Spacer(Modifier.height(10.dp))

            // Description
            Text(
                complaint.description ?: "No description",
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Photo thumbnail if available
            if (!complaint.photo_url.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                AsyncImage(
                    model = complaint.photo_url,
                    contentDescription = "Evidence",
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onClick) {
                    Text("VIEW & UPDATE →", fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────── Detail Column helper ────────────────────
@Composable
fun DetailColumn(modifier: Modifier, icon: ImageVector, label: String, value: String) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(13.dp).padding(top = 2.dp))
        Spacer(Modifier.width(4.dp))
        Column {
            Text(label, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─────────────────────────── Status Badge ────────────────────────────
@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "Pending"             -> PendingAmber.copy(0.15f) to PendingAmber
        "Verified"            -> VerifiedPurple.copy(0.15f) to VerifiedPurple
        "In Progress"         -> InProgressCyan.copy(0.15f) to InProgressCyan
        "Resolved"            -> ResolvedGreen.copy(0.15f) to ResolvedGreen
        "Escalated"           -> EscalatedRed.copy(0.15f) to EscalatedRed
        "Verification Pending"-> PendingAmber.copy(0.15f) to PendingAmber
        else                  -> Color.Gray.copy(0.15f) to Color.Gray
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

// ─────────────────────────── Full Detail Sheet ────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDetailSheet(
    complaint: Complaint,
    onDismiss: () -> Unit,
    onStatusChange: (String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }

    val nextStatuses = when (complaint.status) {
        "Pending"              -> listOf("Verified", "Escalated")
        "Verified"             -> listOf("In Progress", "Escalated")
        "In Progress"          -> listOf("Resolved", "Escalated")
        "Verification Pending" -> listOf("Resolved", "In Progress")
        "Resolved"             -> listOf("In Progress") // reopen
        else                   -> listOf("Verified", "In Progress", "Resolved")
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Color.White.copy(0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(complaint.status ?: "Pending")
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(complaint.category ?: "Unknown", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            Text("Complaint #${complaint.id.take(8)}", fontSize = 12.sp, color = TextSecondary)

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(0.08f))
            Spacer(Modifier.height(16.dp))

            // Full Details Grid
            Text("📋 COMPLAINT DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))

            DetailRow("👤 Reporter",   complaint.reporter_name ?: "Unknown")
            DetailRow("📍 Address",    complaint.address ?: "No address")
            DetailRow("🗺 District",   complaint.district ?: "Unknown")
            DetailRow("📅 Submitted",  complaint.created_at ?: "-")
            DetailRow("🏢 Department", complaint.department ?: "Unassigned")
            DetailRow("📊 Status",     complaint.status ?: "Pending")
            DetailRow("📌 Location",   if (complaint.latitude != null && complaint.longitude != null) "${complaint.latitude}, ${complaint.longitude}" else "N/A")

            Spacer(Modifier.height(12.dp))
            Text("📝 Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text(complaint.description ?: "No description provided.", fontSize = 13.sp, color = TextPrimary)

            // Photo evidence
            if (!complaint.photo_url.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("📷 Photo Evidence", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                AsyncImage(
                    model = complaint.photo_url,
                    contentDescription = "Evidence",
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(0.08f))
            Spacer(Modifier.height(16.dp))

            // Admin note
            Text("✏️ UPDATE STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Add a note (optional)", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = Color.White.copy(0.15f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 3
            )

            Spacer(Modifier.height(12.dp))

            // Action buttons for each valid next status
            nextStatuses.forEach { status ->
                val (btnColor, btnIcon) = when (status) {
                    "Verified"    -> VerifiedPurple to Icons.Default.VerifiedUser
                    "In Progress" -> InProgressCyan to Icons.Default.Build
                    "Resolved"    -> ResolvedGreen  to Icons.Default.CheckCircle
                    "Escalated"   -> EscalatedRed   to Icons.Default.Warning
                    else          -> AccentBlue      to Icons.Default.Update
                }
                Button(
                    onClick = {
                        isUpdating = true
                        onStatusChange(status, note)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                    enabled = !isUpdating
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(btnIcon, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("MARK AS $status", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(0.4f))
        Text(value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.6f))
    }
    HorizontalDivider(color = Color.White.copy(0.04f), modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
fun AdminStatCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(modifier = modifier, color = CardBg, shape = RoundedCornerShape(20.dp), shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        }
    }
}

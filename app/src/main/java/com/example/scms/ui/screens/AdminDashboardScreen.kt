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

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val AdminAccent = Color(0xFF3B82F6)
private val PendingGold = Color(0xFFF59E0B)
private val SuccessGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }

    val departments = listOf("Roads Dept", "Water Supply", "Electricity", "Sanitation", "Safety")

    fun loadData() {
        scope.launch {
            try {
                isLoading = true
                complaints = RetrofitClient.api.getComplaints()
            } catch (_: Exception) {} finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val filteredComplaints = when (selectedFilter) {
        "Pending" -> complaints.filter { it.status == "Pending" }
        "Resolved" -> complaints.filter { it.status == "Resolved" }
        "Assigned" -> complaints.filter { it.status == "Assigned" }
        else -> complaints
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("COMMAND CENTER", fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            /* ---------- 📊 ANALYTICS HUD ---------- */
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard(Modifier.weight(1f), "TOTAL", complaints.size.toString(), Icons.Default.Assessment, AdminAccent)
                AdminStatCard(Modifier.weight(1f), "OPEN", complaints.count { it.status != "Resolved" }.toString(), Icons.Default.PendingActions, PendingGold)
                AdminStatCard(Modifier.weight(1f), "CLOSED", complaints.count { it.status == "Resolved" }.toString(), Icons.Default.CheckCircle, SuccessGreen)
            }

            /* ---------- 📍 MAP VIEW PREVIEW ---------- */
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(140.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box {
                    AdminMapView(complaints = complaints, modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
                    Text("LIVE CITY ACTIVITY MAP", modifier = Modifier.align(Alignment.BottomStart).padding(12.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            /* ---------- 🏷️ FILTERS ---------- */
            LazyRow(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Pending", "Assigned", "Resolved")) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary, 
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            /* ---------- 📄 COMPLAINT FEED ---------- */
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(filteredComplaints) { complaint ->
                    AdminComplaintCard(
                        complaint = complaint,
                        departments = departments,
                        onAssign = { dept ->
                            scope.launch {
                                RetrofitClient.api.assignDepartment(complaint.id, mapOf("department" to dept))
                                loadData()
                            }
                        },
                        onResolve = {
                            scope.launch {
                                RetrofitClient.api.markResolved(complaint.id)
                                loadData()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(
        modifier = modifier, 
        color = MaterialTheme.colorScheme.surface, 
        shape = RoundedCornerShape(20.dp), 
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminComplaintCard(complaint: Complaint, departments: List<String>, onAssign: (String) -> Unit, onResolve: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(AdminAccent.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Report, null, tint = AdminAccent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(complaint.category ?: "Unknown", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(complaint.address ?: "No Address", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), maxLines = 1)
                }
                Surface(
                    color = if (complaint.status == "Pending") PendingGold.copy(alpha = 0.1f) else SuccessGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(complaint.status ?: "Pending", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = if (complaint.status == "Pending") PendingGold else SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            
            if (!complaint.photo_url.isNullOrEmpty()) {
                AsyncImage(model = complaint.photo_url, contentDescription = null, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.height(12.dp))
            }

            Text(complaint.description ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            if (complaint.status == "Pending") {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = complaint.department ?: "Assign Department",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AdminAccent)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        departments.forEach { dept ->
                            DropdownMenuItem(text = { Text(dept) }, onClick = { onAssign(dept); expanded = false })
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                Button(
                    onClick = onResolve,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.DoneAll, null)
                    Spacer(Modifier.width(8.dp))
                    Text("MARK AS RESOLVED", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Shield, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("HANDLED BY: ${complaint.department}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

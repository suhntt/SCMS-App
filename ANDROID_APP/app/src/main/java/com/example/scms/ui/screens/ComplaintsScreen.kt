package com.example.scms.ui.screens
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ComplaintsScreen(navController: NavController, filteredId: String? = null) {

    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) } // ✅ RENAMED TO AVOID CONFLICT
    
    var searchQuery by remember { mutableStateOf("") }
    val tabs = listOf("Pending", "Active", "Verify", "Resolved")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // 🌪️ IMPROVED FLIPKART COLLAPSING LOGIC
    val headerHeight = 180.dp
    val headerHeightPx = with(LocalDensity.current) { headerHeight.toPx() }
    val headerOffsetHeightPx = remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = headerOffsetHeightPx.value + delta
                headerOffsetHeightPx.value = newOffset.coerceIn(-headerHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val fusedLocationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }

    suspend fun loadComplaints() {
        isLoading = true
        errorMsg = null
        try {
            val userDistrict = UserSession.currentUser?.district
            val fetched = RetrofitClient.api.getComplaints(district = userDistrict)
            complaints = if (filteredId != null && filteredId != "null" && filteredId.isNotEmpty()) {
                fetched.filter { it.user_id?.toString() == filteredId }
            } else {
                fetched
            }
            
            // 🗺️ Feature 2: Check for nearby High Severity hazards
            userLocation?.let { (lat, lon) ->
                GeofenceManager.checkProximity(context, lat, lon, complaints)
            }
        } catch (e: Exception) {
            errorMsg = "Network Error: ${e.message ?: "Unable to reach server"}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(filteredId, userLocation) { loadComplaints() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { 
                    loadComplaints() 
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) userLocation = Pair(loc.latitude, loc.longitude)
                        }
                    } catch (_: SecurityException) {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        floatingActionButton = {
            if (filteredId == null || filteredId == "null") {
                FloatingActionButton(
                    onClick = { navController.navigate("report") }, 
                    containerColor = MaterialTheme.colorScheme.primary, 
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Report")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            // 📑 MAIN FEED CONTENT (Non-Swipeable)
            AnimatedContent(
                targetState = selectedTabIndex,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "TabTransition"
            ) { pageIndex ->
                val currentTabStatus = tabs[pageIndex]
                val displayComplaints = complaints.filter { c ->
                    val matchesSearch = searchQuery.isBlank() || 
                        c.category?.contains(searchQuery, true) == true || 
                        c.description?.contains(searchQuery, true) == true
                    
                    val matchesTab = when (currentTabStatus) {
                        "Active" -> c.status == "In Progress" || c.status == "Assigned"
                        "Pending" -> c.status == "Pending" || c.status == "Verified"
                        "Verify" -> c.status == "Verification Pending"
                        else -> c.status == "Resolved"
                    }
                    
                    matchesSearch && matchesTab
                }

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) 
                    }
                } else if (errorMsg != null) { 
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.WifiOff, null, modifier = Modifier.size(64.dp), tint = Color.Red)
                            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                            Button(
                                onClick = { scope.launch { loadComplaints() } }, 
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                } else if (displayComplaints.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Text("No $currentTabStatus issues found.", color = Color.Gray)
                            Text("Pull down to refresh or try searching.", fontSize = 12.sp, color = Color.LightGray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = headerHeight, bottom = 100.dp)
                    ) {
                        items(displayComplaints, key = { it.id }) { complaint ->
                            ComplaintCard(
                                complaint = complaint,
                                navController = navController,
                                userLocation = userLocation,
                                onUpvote = {
                                    if (complaint.status != "Resolved") {
                                        scope.launch {
                                            try {
                                                val userId = UserSession.currentUser?.id ?: return@launch
                                                RetrofitClient.api.upvote(complaint.id, mapOf("user_id" to userId))
                                                loadComplaints()
                                            } catch (_: Exception) {}
                                        }
                                    }
                                },
                                onVerify = { isSuccess ->
                                    scope.launch {
                                        try {
                                            val userId = UserSession.currentUser?.id ?: return@launch
                                            RetrofitClient.api.verifyComplaint(
                                                complaint.id, 
                                                mapOf("user_id" to userId, "verified" to isSuccess)
                                            )
                                            loadComplaints()
                                        } catch (_: Exception) {}
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 🏙️ COLLAPSIBLE HEADER
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .offset { IntOffset(x = 0, y = headerOffsetHeightPx.value.roundToInt()) },
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                if (filteredId != null && filteredId != "null") "My Activity" else "Report Complaints", 
                                style = MaterialTheme.typography.headlineSmall, 
                                fontWeight = FontWeight.ExtraBold
                            )
                            // ✅ Show user's district
                            val district = UserSession.currentUser?.district
                            if (district != null) {
                                Text(
                                    "📍 $district",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search issues...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

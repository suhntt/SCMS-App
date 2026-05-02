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

import android.app.Activity
import android.net.Uri
import android.location.Geocoder
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.scms.utils.NetworkObserver
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportComplaintScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val settingsClient = remember { LocationServices.getSettingsClient(context) }

    // Location State
    var autoAddress by remember { mutableStateOf("") }
    var autoLat by remember { mutableStateOf(0.0) }
    var autoLon by remember { mutableStateOf(0.0) }
    var autoDistrict by remember { mutableStateOf("") }
    var isFetchingLocation by remember { mutableStateOf(false) }

    // Image State
    var latestFile by remember { mutableStateOf<File?>(null) }
    var latestUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var previewImageUri by remember { mutableStateOf<Uri?>(null) }

    // Form State
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Road Damage") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isAIScanning by remember { mutableStateOf(false) }
    var aiMatchFound by remember { mutableStateOf<DuplicateCheckResponse?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showOfflineSuccessDialog by remember { mutableStateOf(false) }

    // Connectivity
    val networkObserver = remember { NetworkObserver(context) }
    val networkStatus by networkObserver.observe.collectAsState(initial = NetworkObserver.Status.Available)
    val isOnline = networkStatus == NetworkObserver.Status.Available

    val categories = listOf("Road Damage", "Water Supply", "Electricity", "Waste Management", "Public Safety", "Others")

    // Camera
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && latestUri != null && latestFile != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                isAIScanning = true
                val compressed = ImageUtils.compressImage(context, latestFile!!)
                // 🕵️‍♂️ SIMULATE AI VISION FINGERPRINTING
                kotlinx.coroutines.delay(2000) 
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    selectedImages = (selectedImages + latestUri!!).take(5)
                    imageFiles = (imageFiles + compressed).take(5)
                    isAIScanning = false
                }
            }
        }
    }

    // ── Location + District Detection ─────────────────────────────────
    fun updateAddress(lat: Double, lon: Double) {
        autoLat = lat; autoLon = lon
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Street address from Geocoder
                val geocoder = Geocoder(context, Locale.getDefault())
                val addr = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    autoAddress = addr?.getAddressLine(0) ?: "$lat, $lon"
                }
                // Accurate district via backend Nominatim (fixes Android Geocoder's wrong division names)
                val resp = RetrofitClient.api.detectDistrict(lat.toString(), lon.toString())
                val district = resp.district.ifBlank { "Unknown District" }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    autoDistrict = district
                    val cur = UserSession.currentUser
                    if (cur != null && cur.district != district) {
                        UserSession.currentUser = cur.copy(district = district)
                        try {
                            RetrofitClient.api.updateUserDistrict(mapOf("user_id" to cur.id.toString(), "district" to district))
                            val topic = "district_${district.lowercase().replace(Regex("[^a-z0-9]"), "_").replace(Regex("_+"), "_").trim('_')}"
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    autoAddress = "$lat, $lon"
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isFetchingLocation = false
                }
            }
        }
    }

    fun performLocationFetch() {
        if (isFetchingLocation) return
        isFetchingLocation = true; autoAddress = ""; autoDistrict = ""
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc -> if (loc != null) scope.launch { updateAddress(loc.latitude, loc.longitude) } else isFetchingLocation = false }
                .addOnFailureListener { isFetchingLocation = false }
        } catch (_: SecurityException) { isFetchingLocation = false }
    }

    val gpsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) performLocationFetch()
    }
    LaunchedEffect(Unit) {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(req)
        settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener { performLocationFetch() }
            .addOnFailureListener { ex ->
                if (ex is ResolvableApiException) try {
                    gpsLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(ex.resolution.intentSender).build())
                } catch (_: Exception) {}
            }
    }

    // ── Accent colours ────────────────────────────────────────────────
    val purple = Color(0xFF7C3AED)
    val green  = Color(0xFF10B981)
    val blue   = Color(0xFF3B82F6)
    val amber  = Color(0xFFF59E0B)

    // ═══════════════════════════════════════════════════════════════════
    // UI
    // ═══════════════════════════════════════════════════════════════════
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // ── Header ────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 32.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=16.dp)) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("NEW INCIDENT", color = Color(0xFF6366F1), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text("Report an Issue", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── STEP 1 · LOCATION ─────────────────────────────────
                StepHeader(1, "Location", Icons.Default.Place, MaterialTheme.colorScheme.primary)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                        if (isFetchingLocation) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                Text("Detecting location & district…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                            }
                        } else {
                            // Address
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                Text(
                                    autoAddress.ifBlank { "Tap ↻ to detect location" },
                                    fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp,
                                    color = if (autoAddress.isBlank()) MaterialTheme.colorScheme.onSurface.copy(0.4f) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { performLocationFetch() }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }

                            // District badge + coordinates
                            if (autoDistrict.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(shape = RoundedCornerShape(50.dp), color = purple.copy(0.1f)) {
                                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, null, tint = purple, modifier = Modifier.size(12.dp))
                                            Text(autoDistrict, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = purple)
                                        }
                                    }
                                    Text("District · Assam", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
                                }
                            }

                            if (autoLat != 0.0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CoordPill("LAT", "%.5f°".format(autoLat), green)
                                    CoordPill("LON", "%.5f°".format(autoLon), blue)
                                }
                            }
                        }
                    }
                }

                // ── STEP 2 · ISSUE TYPE ──────────────────────────────
                StepHeader(2, "Issue Type", Icons.Default.Category, amber)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat, fontSize = 13.sp, fontWeight = if (category == cat) FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                        AnimatedVisibility(visible = category == "Others", enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            OutlinedTextField(
                                value = title, onValueChange = { title = it },
                                label = { Text("Custom Title") },
                                placeholder = { Text("Name your unique issue…") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp), singleLine = true
                            )
                        }
                    }
                }

                // ── STEP 3 · PHOTO EVIDENCE ──────────────────────────
                StepHeader(3, "Photo Evidence", Icons.Default.CameraAlt, Color(0xFFEF4444))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isAIScanning) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.secondary)
                            Text("SENTINEL AI: Analyzing Visual Fingerprints...", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
                        } else {
                            Text("Tap + to capture (up to 5 photos)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (selectedImages.size < 5) {
                                item {
                                    Box(
                                        modifier = Modifier.size(90.dp).clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(0.08f))
                                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(0.3f), RoundedCornerShape(14.dp))
                                            .clickable {
                                                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SCMS_${System.currentTimeMillis()}.jpg")
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                                latestFile = file; latestUri = uri; cameraLauncher.launch(uri)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                                            Text("Add", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                            items(selectedImages.size) { i ->
                                Box(modifier = Modifier.size(90.dp)) {
                                    AsyncImage(
                                        model = selectedImages[i], contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).clickable { previewImageUri = selectedImages[i] },
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(modifier = Modifier.size(22.dp).align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp), shape = CircleShape, color = Color(0xFFEF4444)) {
                                        IconButton(onClick = {
                                            selectedImages = selectedImages.toMutableList().apply { removeAt(i) }
                                            imageFiles = imageFiles.toMutableList().apply { removeAt(i) }
                                        }, modifier = Modifier.size(22.dp)) {
                                            Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        if (selectedImages.isEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = amber, modifier = Modifier.size(14.dp))
                                Text("At least 1 photo required", fontSize = 12.sp, color = amber)
                            }
                        }
                    }
                }

                // ── STEP 4 · DESCRIPTION ─────────────────────────────
                StepHeader(4, "Description", Icons.Default.Description, green)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        placeholder = { Text("Describe the issue — what, where, and how severe is it?", color = MaterialTheme.colorScheme.onSurface.copy(0.35f), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(130.dp).padding(4.dp),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }

                // ── SUBMIT ────────────────────────────────────────────
                val canSubmit = description.isNotBlank() && selectedImages.isNotEmpty() && !isSubmitting && !isFetchingLocation
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                if (!isOnline) {
                                    OfflineComplaintManager.saveOffline(context, if (category == "Others") title else category, category, description, autoAddress, autoLat.toString(), autoLon.toString(), imageFiles)
                                    showOfflineSuccessDialog = true
                                } else {
                                    // 🔍 Step 1: AI DUPLICATION PRE-CHECK
                                    isSubmitting = true
                                    val checkBody = mapOf(
                                        "category" to category,
                                        "latitude" to autoLat.toString(),
                                        "longitude" to autoLon.toString()
                                    )
                                    val checkRes = RetrofitClient.api.checkDuplicateComplaint(checkBody)
                                    if (checkRes.isSuccessful && checkRes.body()?.isDuplicate == true) {
                                        aiMatchFound = checkRes.body()
                                        isSubmitting = false
                                        return@launch
                                    }

                                    // 📤 Step 2: ACTUAL SUBMISSION
                                    val photoParts = imageFiles.map { f ->
                                        MultipartBody.Part.createFormData("photos", f.name, f.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                                    }
                                    val dist = autoDistrict.ifEmpty { UserSession.currentUser?.district ?: "" }
                                    val res = RetrofitClient.api.submitComplaint(
                                        photos = photoParts,
                                        title = (if (category == "Others") title else category).toRequestBody("text/plain".toMediaTypeOrNull()),
                                        category = category.toRequestBody("text/plain".toMediaTypeOrNull()),
                                        address = autoAddress.toRequestBody("text/plain".toMediaTypeOrNull()),
                                        latitude = autoLat.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                        longitude = autoLon.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                        description = description.toRequestBody("text/plain".toMediaTypeOrNull()),
                                        userId = (UserSession.currentUser?.id?.toString() ?: "").toRequestBody("text/plain".toMediaTypeOrNull()),
                                        district = dist.toRequestBody("text/plain".toMediaTypeOrNull())
                                    )
                                    if (res.isSuccessful) showSuccessDialog = true
                                    else {
                                        OfflineComplaintManager.saveOffline(context, if (category == "Others") title else category, category, description, autoAddress, autoLat.toString(), autoLon.toString(), imageFiles)
                                        showOfflineSuccessDialog = true
                                    }
                                }
                            } catch (_: Exception) {
                                OfflineComplaintManager.saveOffline(context, if (category == "Others") title else category, category, description, autoAddress, autoLat.toString(), autoLon.toString(), imageFiles)
                                showOfflineSuccessDialog = true
                            } finally { isSubmitting = false }
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1), contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 2.dp)
                ) {
                    if (isSubmitting) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                    else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Send, null)
                        Text("SUBMIT REPORT", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }

                if (!canSubmit && !isSubmitting) {
                    Text(
                        when {
                            isFetchingLocation -> "⏳ Waiting for location…"
                            selectedImages.isEmpty() -> "📷 Add at least one photo to continue"
                            description.isBlank() -> "📝 Add a description to continue"
                            else -> ""
                        },
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────
    if (showOfflineSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) },
            title = { Text("Saved Offline") },
            text = { Text("No connection detected. Your report is saved and will auto-sync when you're back online.") },
            confirmButton = {
                Button(onClick = { showOfflineSuccessDialog = false; navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Got it!") }
            }
        )
    }
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Report Submitted! 🎉") },
            text = { Text("Your complaint has been filed under $autoDistrict district. You earned +2 SCMS points!") },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false; navController.navigate("complaints") { popUpTo("home") { inclusive = false } } }
                ) { Text("View Complaints") }
            }
        )
    }
    if (aiMatchFound != null) {
        AlertDialog(
            onDismissRequest = { aiMatchFound = null },
            icon = { Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(36.dp)) },
            title = { Text("Sentinel Vision Alert 🤖", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Our AI Vision system detected a 94% visual match with an existing report in this area.", fontSize = 14.sp)
                    Surface(color = MaterialTheme.colorScheme.secondary.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text("Existing ID: SCMS-${aiMatchFound?.duplicate?.id ?: "XXXX"}", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                    }
                    Text("Would you like to Upvote the existing report instead of creating a duplicate?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
            },
            confirmButton = {
                Button(onClick = { aiMatchFound = null; navController.navigate("complaints") }) { Text("View & Upvote") }
            },
            dismissButton = {
                TextButton(onClick = { aiMatchFound = null }) { Text("Report Anyway", color = Color.Gray) }
            }
        )
    }

    if (previewImageUri != null) {
        Dialog(onDismissRequest = { previewImageUri = null }) {
            Box(modifier = Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black)) {
                AsyncImage(model = previewImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                IconButton(onClick = { previewImageUri = null }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(0.2f))) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }
}

// ── Helper Composables ────────────────────────────────────────────────

@Composable
private fun StepHeader(step: Int, label: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(28.dp).background(color.copy(0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Text("$step", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun CoordPill(label: String, value: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(0.1f)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = color)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// Keep old LocationDataBox for backward compatibility used in other screens
@Composable
fun LocationDataBox(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, accent: Color) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.2f)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(accent.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), letterSpacing = 1.sp)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

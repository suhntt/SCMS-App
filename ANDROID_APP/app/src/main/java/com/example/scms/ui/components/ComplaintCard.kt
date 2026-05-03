package com.example.scms.ui.components
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyRow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ComplaintCard(
    complaint: Complaint,
    onUpvote: () -> Unit,
    onVerify: ((Boolean) -> Unit)? = null,
    navController: androidx.navigation.NavController? = null,
    userLocation: Pair<Double, Double>? = null
) {
    val context = LocalContext.current

    val statusColor = when (complaint.status) {
        "Resolved" -> Color(0xFF10B981)
        "In Progress" -> Color(0xFF3B82F6)
        "Assigned" -> Color(0xFFF59E0B)
        else -> Color(0xFF6B7280)
    }
    
    val severityColor = when (complaint.severity) {
        "High" -> Color(0xFFEF4444)
        "Medium" -> Color(0xFFF97316)
        else -> Color(0xFF22C55E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            /* ---------- HEADER: TITLE & STATUS CHIP ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = complaint.title ?: complaint.category ?: "Civic Issue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID: SCMS-AS-2026-${complaint.id} • ${(complaint.category ?: "General").uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF003366),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = (complaint.status ?: "Pending").uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            /* ---------- ADDRESS ROW ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = complaint.address ?: "Location not specified",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            /* ---------- PHOTOS ---------- */
            val images = complaint.photo_urls?.filter { it.isNotBlank() } 
                ?: listOfNotNull(complaint.photo_url?.trim()?.takeIf { it.isNotEmpty() })
                
            if (images.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images.size) { index ->
                        AsyncImage(
                            model = images[index],
                            contentDescription = null,
                            modifier = Modifier.fillParentMaxWidth(if (images.size == 1) 1f else 0.85f).height(180.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            /* ---------- PROFESSIONAL INFO BAR (Reporter, Distance, Date) ---------- */
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Reporter
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(complaint.reporter_name ?: "Citizen", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Middle: Distance
                    if (userLocation != null && complaint.latitude != null && complaint.longitude != null) {
                        val dist = calculateDistanceInKm(userLocation.first, userLocation.second, complaint.latitude.toDouble(), complaint.longitude.toDouble())
                        val distStr = if (dist < 1.0) "${(dist * 1000).toInt()}m" else "${String.format("%.1f", dist)}km"
                        Text(distStr, fontSize = 11.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Black)
                    }

                    // Right Side: Date & Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatDateTime(complaint.created_at),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            /* ---------- DESCRIPTION ---------- */
            if (!complaint.description.isNullOrBlank()) {
                Text(
                    text = complaint.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            /* ---------- FOOTER: UPVOTE & ACTIONS ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Severity Tag
                Surface(
                    color = severityColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${complaint.severity ?: "Low"} Priority",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = severityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (complaint.status != "Resolved") {
                        IconButton(onClick = onUpvote) {
                            Icon(Icons.Default.ThumbUp, null, tint = if (complaint.upvotes > 0) Color(0xFF6366F1) else Color.Gray)
                        }
                    } else {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.padding(12.dp))
                    }
                    
                    Text("${complaint.upvotes}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "🚨 *I just reported an issue on the SCMS App!*\n\n*Issue:* ${complaint.category}\n*Location:* ${complaint.address}\n*Tracking ID:* SCMS-AS-2026-${complaint.id}\n\nJoin me and UPVOTE this report so the municipality fixes it faster! 🚀")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Rally Support via"))
                    }) {
                        Icon(Icons.Default.Share, null, tint = Color(0xFF10B981))
                    }

                    if (navController != null) {
                        IconButton(onClick = { navController.navigate("timeline/${complaint.id}") }) {
                            Icon(Icons.Default.History, null, tint = Color(0xFF003366))
                        }
                    }
                }
            }

            /* ---------- TRUST SYSTEM: VERIFICATION BUTTONS ---------- */
            if (complaint.status == "Verification Pending") {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "🛡️ TRUST SYSTEM: Is this issue fixed?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF003366),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onVerify?.invoke(true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Yes, Fixed", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    
                    OutlinedButton(
                        onClick = { onVerify?.invoke(false) },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("No, Broken", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/**
 * Formats ISO date to "dd-MM-yyyy | hh:mm AM/PM"
 */
fun formatDateTime(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return "N/A"
    return try {
        // Try parsing ISO format
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { 
            timeZone = TimeZone.getTimeZone("UTC") 
        }
        val date = inputFormat.parse(isoDate)
        val outputFormat = SimpleDateFormat("dd-MM-yyyy | hh:mm a", Locale.US)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        // Fallback: Just return the date part if parsing fails
        isoDate.split("T").firstOrNull() ?: "N/A"
    }
}

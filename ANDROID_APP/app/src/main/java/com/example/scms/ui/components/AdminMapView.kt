package com.example.scms.ui.components
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import android.preference.PreferenceManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun AdminMapView(
    complaints: List<Complaint>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->

            Configuration.getInstance().load(
                context,
                PreferenceManager.getDefaultSharedPreferences(context)
            )

            val mapView = MapView(context)
            mapView.setMultiTouchControls(true)
            mapView.controller.setZoom(12.0)

            // Center map to first complaint
            complaints.firstOrNull()?.let { c ->
                val lat = c.latitude?.toDoubleOrNull()
                val lon = c.longitude?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    mapView.controller.setCenter(GeoPoint(lat, lon))
                }
            }

            // Add markers
            complaints.forEach { c ->
                val lat = c.latitude?.toDoubleOrNull()
                val lon = c.longitude?.toDoubleOrNull()

                if (lat != null && lon != null) {
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(lat, lon)
                    marker.title = c.category ?: "Complaint"
                    marker.subDescription = c.address ?: ""
                    mapView.overlays.add(marker)
                }
            }

            mapView
        }
    )
}

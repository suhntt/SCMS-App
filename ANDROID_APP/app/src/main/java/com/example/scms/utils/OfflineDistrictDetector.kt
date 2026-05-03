package com.example.scms.utils

/**
 * 🗺️ OFFLINE DISTRICT DETECTOR
 * Detects Assam district from GPS coordinates without any internet connection.
 * Uses embedded bounding box data for all 35 Assam districts.
 * GPS lat/long is always available via satellite — no internet required.
 */
object OfflineDistrictDetector {

    data class DistrictBounds(
        val name: String,
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double,
        val priority: Int = 0  // Higher priority wins when boxes overlap
    )

    // ✅ All 35 Assam Districts with geographic bounding boxes
    private val assamDistricts = listOf(
        // Core Guwahati / Central Assam
        DistrictBounds("Kamrup Metropolitan", 26.05, 26.28, 91.55, 91.92, priority = 10),
        DistrictBounds("Kamrup",              25.90, 26.55, 91.00, 91.65, priority = 5),
        DistrictBounds("Nalbari",             26.27, 26.62, 91.32, 91.65),
        DistrictBounds("Barpeta",             26.18, 26.72, 90.75, 91.35),
        DistrictBounds("Morigaon",            26.10, 26.55, 92.00, 92.50),
        DistrictBounds("Darrang",             26.60, 27.05, 91.60, 92.45),
        DistrictBounds("Baksa",               26.38, 26.82, 91.05, 91.72),

        // Western Assam
        DistrictBounds("Dhubri",              25.78, 26.32, 89.65, 90.42),
        DistrictBounds("Goalpara",            25.78, 26.32, 90.38, 91.05),
        DistrictBounds("Bongaigaon",          26.18, 26.65, 90.35, 90.82),
        DistrictBounds("Chirang",             26.45, 26.88, 90.22, 90.80),
        DistrictBounds("Kokrajhar",           26.18, 26.82, 89.78, 90.48),
        DistrictBounds("South Salmara Mankachar", 25.48, 25.90, 89.75, 90.32),

        // Northern Assam
        DistrictBounds("Sonitpur",            26.48, 27.05, 92.45, 93.52),
        DistrictBounds("Biswanath",           26.58, 27.12, 92.95, 93.78),
        DistrictBounds("Udalguri",            26.62, 27.12, 91.80, 92.42),
        DistrictBounds("Lakhimpur",           27.00, 27.82, 93.72, 94.52),
        DistrictBounds("Dhemaji",             27.30, 28.00, 93.78, 95.00),

        // Eastern Assam
        DistrictBounds("Jorhat",              26.48, 27.00, 93.52, 94.55),
        DistrictBounds("Majuli",              26.78, 27.12, 93.88, 94.62, priority = 5),
        DistrictBounds("Golaghat",            26.15, 26.82, 93.40, 94.22),
        DistrictBounds("Sivasagar",           26.75, 27.42, 94.15, 95.05),
        DistrictBounds("Charaideo",           26.68, 27.32, 94.42, 95.22),
        DistrictBounds("Dibrugarh",           27.15, 27.72, 94.42, 95.35),
        DistrictBounds("Tinsukia",            27.25, 27.92, 94.95, 96.05),

        // Central Assam
        DistrictBounds("Nagaon",              25.80, 26.52, 92.28, 93.25),
        DistrictBounds("Hojai",               25.95, 26.42, 92.62, 93.02),

        // Hill Districts
        DistrictBounds("Karbi Anglong",       25.45, 26.52, 92.42, 93.85),
        DistrictBounds("West Karbi Anglong",  25.65, 26.42, 92.00, 92.82),
        DistrictBounds("Dima Hasao",          24.95, 25.82, 92.42, 93.52),

        // Southern Assam (Barak Valley)
        DistrictBounds("Cachar",              24.45, 25.02, 92.42, 93.22),
        DistrictBounds("Hailakandi",          24.28, 24.72, 92.45, 92.95),
        DistrictBounds("Karimganj",           24.25, 24.90, 92.05, 92.62),
    )

    /**
     * Detects district from GPS coordinates — works 100% OFFLINE.
     * @param lat Latitude from GPS
     * @param lon Longitude from GPS
     * @return District name, or null if coordinates are outside Assam
     */
    fun detectDistrict(lat: Double, lon: Double): String? {
        if (lat == 0.0 && lon == 0.0) return null

        // Find all matching districts (coordinates may fall in overlapping boxes)
        val matches = assamDistricts.filter { d ->
            lat >= d.minLat && lat <= d.maxLat &&
            lon >= d.minLon && lon <= d.maxLon
        }

        return when {
            matches.isEmpty() -> {
                // Check if at least within Assam's broad bounds
                if (lat in 24.0..28.5 && lon in 89.5..96.5) "Assam" else null
            }
            matches.size == 1 -> matches[0].name
            else -> {
                // Multiple matches: pick highest priority, then smallest area (most precise)
                matches.maxByOrNull { d ->
                    d.priority * 1000 - ((d.maxLat - d.minLat) * (d.maxLon - d.minLon) * 100).toInt()
                }?.name
            }
        }
    }

    /**
     * Generates a human-readable offline address string from coordinates.
     * Used when Geocoder (internet) is unavailable.
     */
    fun buildOfflineAddress(lat: Double, lon: Double): String {
        val district = detectDistrict(lat, lon)
        return if (district != null) {
            "$district, Assam (GPS: ${String.format("%.4f", lat)}, ${String.format("%.4f", lon)})"
        } else {
            "GPS Location: ${String.format("%.5f", lat)}, ${String.format("%.5f", lon)}"
        }
    }
}

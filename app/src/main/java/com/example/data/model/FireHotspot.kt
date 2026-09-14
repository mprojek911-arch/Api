package com.example.data.model

/**
 * Representasi domain model titik api / hotspot satelit.
 * Mengusung transparansi penuh: membedakan waktu observasi satelit dan waktu data diterima aplikasi.
 */
data class FireHotspot(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val acqDate: String,            // YYYY-MM-DD
    val acqTime: String,            // HHMM dalam UTC
    val satellite: String,          // NOAA-21, NOAA-20, Suomi-NPP, MODIS Terra, MODIS Aqua
    val instrument: String,         // VIIRS atau MODIS
    val confidence: String,         // high, nominal, low
    val confidenceRaw: String,      // nilai asli dari feed
    val frp: Double,                // Fire Radiative Power (Megawatts)
    val scan: Double,               // Resolusi piksel scan (km)
    val track: Double,              // Resolusi piksel track (km)
    val dayNight: String,           // D = Siang, N = Malam
    val version: String,            // e.g. 2.0NRT
    val source: String,             // NASA FIRMS (VIIRS / MODIS)
    val retrievedAt: Long,          // Timestamp aplikasi menerima data (ms)
    val observationEpochMs: Long,   // Timestamp waktu observasi satelit (ms UTC)
    val timeWita: String,           // Format waktu lokal WITA (UTC+8)
    val timeWib: String,            // Format waktu lokal WIB (UTC+7)
    val dataAgeMinutes: Long,       // Umur data sejak observasi dalam menit
    val dataAgeFormatted: String,   // Umur data manusiawi (e.g. "45 menit", "2 jam 15 menit")
    val dataAgeCategory: DataAgeCategory, // TERBARU, NRT, DATA_LEBIH_LAMA, DATA_LAMA
    val province: String,           // Provinsi (Kalimantan Tengah, dll.)
    val regency: String = "",       // Estimasi Kabupaten/Kota terdekat
    val distanceKm: Double? = null, // Jarak dari lokasi GPS pengguna
    val bearingDirection: String? = null // Arah kompas dari pengguna (e.g. "Barat Laut")
)

/**
 * Kategori umur data sesuai standar audit:
 * 🟢 0–60 menit: "TERBARU"
 * 🟡 61–180 menit: "NRT"
 * 🟠 181–360 menit: "DATA LEBIH LAMA"
 * 🔴 >360 menit: "DATA LAMA"
 */
enum class DataAgeCategory(val label: String, val maxMinutes: Long) {
    TERBARU("TERBARU", 60),
    NRT("NRT", 180),
    DATA_LEBIH_LAMA("DATA LEBIH LAMA", 360),
    DATA_LAMA("DATA LAMA", Long.MAX_VALUE)
}

/**
 * Status koneksi dan integritas data aplikasi
 */
enum class DataConnectionStatus(val label: String) {
    DATA_TERBARU("DATA TERBARU"),
    NRT("NRT"),
    DATA_TERTUNDA("DATA TERTUNDA"),
    DATA_TIDAK_TERSEDIA("DATA TIDAK TERSEDIA")
}

/**
 * Filter parameter untuk observasi satelit
 */
data class FilterState(
    val timeRange: String = "24h", // 1h, 3h, 6h, 12h, 24h, 48h, all
    val satellite: String = "all", // all, VIIRS, MODIS, NOAA-21, NOAA-20, Suomi-NPP
    val confidence: String = "all", // all, high, nominal, low
    val minFrp: Double = 0.0,
    val province: String = "all", // all, Kalimantan Tengah, etc.
    val nearbyRadiusKm: Int = 0, // 0 = off, 5, 10, 25, 50 km
    val searchQuery: String = "",
    val onlyLatestHotspots: Boolean = false // Tombol "HOTSPOT TERBARU" (< 60 menit atau < 180 menit)
)

/**
 * Data koordinat lokasi pengguna (GPS riil)
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

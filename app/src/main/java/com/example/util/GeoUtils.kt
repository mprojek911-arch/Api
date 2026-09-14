package com.example.util

import com.example.data.model.DataAgeCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

object GeoUtils {
    // Koordinat Pos Pemantauan Karhutla Hardi Mantangai, Kapuas, Kalimantan Tengah
    const val HARDI_MANTANGAI_LAT = -2.3456
    const val HARDI_MANTANGAI_LON = 114.4823
    const val KALIMANTAN_CENTER_LAT = -1.5
    const val KALIMANTAN_CENTER_LON = 114.0

    // Batas geografis Pulau Kalimantan wilayah Indonesia
    const val MIN_LAT = -4.5
    const val MAX_LAT = 4.3
    const val MIN_LON = 108.0
    const val MAX_LON = 119.2

    /**
     * Memeriksa apakah koordinat valid dan berada dalam rentang teritori Kalimantan Indonesia.
     */
    fun isWithinKalimantan(lat: Double, lon: Double): Boolean {
        if (lat.isNaN() || lon.isNaN()) return false
        if (abs(lat) < 0.0001 && abs(lon) < 0.0001) return false // Tolak Null Island
        if (lat < MIN_LAT || lat > MAX_LAT || lon < MIN_LON || lon > MAX_LON) return false

        // Filter perbatasan Sabah / Sarawak Malaysia di utara
        if (lat > 4.15) return false
        if (lat >= 4.0 && lon in 114.0..115.4) return false // Brunei
        if (lon in 109.5..111.0 && lat > 2.05) return false // Sarawak utara Sambas
        if (lon in 111.0..112.5 && lat > 1.7) return false
        if (lon in 112.5..114.0 && lat > 1.9) return false
        // Exclude Selat Makassar / barat Sulawesi
        if (lon > 119.0 && lat < 0.0) return false

        return true
    }

    /**
     * Perhitungan jarak geodesik menggunakan formula Haversine (hasil dalam km).
     */
    fun calculateHaversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1.isNaN() || lon1.isNaN() || lat2.isNaN() || lon2.isNaN()) return 0.0
        val r = 6371.0 // Radius bumi dalam km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    data class BearingResult(val degrees: Double, val direction: String)

    /**
     * Menghitung arah azimuth kompas dari titik asal ke titik tujuan.
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): BearingResult {
        if (lat1.isNaN() || lon1.isNaN() || lat2.isNaN() || lon2.isNaN()) {
            return BearingResult(0.0, "Utara")
        }
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        val degrees = (Math.toDegrees(theta) + 360.0) % 360.0
        val directions = listOf(
            "Utara", "Timur Laut", "Timur", "Tenggara",
            "Selatan", "Barat Daya", "Barat", "Barat Laut"
        )
        val index = ((degrees + 22.5) / 45.0).toInt() % 8
        return BearingResult(degrees, directions[index])
    }

    /**
     * Menentukan provinsi Kalimantan berdasarkan koordinat geografis.
     */
    fun determineProvince(lat: Double, lon: Double): String {
        if (lat.isNaN() || lon.isNaN()) return "Kalimantan Tengah"
        return when {
            // Kalimantan Utara
            lat >= 0.85 && lon >= 114.5 -> "Kalimantan Utara"
            // Kalimantan Barat
            lon <= 111.0 && lat in -3.2..2.2 -> "Kalimantan Barat"
            lon in 111.0..113.8 && lat in -0.8..2.2 -> "Kalimantan Barat"
            lon in 113.8..114.2 && lat in 0.3..1.5 -> "Kalimantan Barat"
            // Kalimantan Timur
            lon >= 116.6 && lat in -2.5..2.5 -> "Kalimantan Timur"
            lon in 115.3..119.0 && lat in -1.5..2.5 -> "Kalimantan Timur"
            // Kalimantan Selatan
            lat <= -3.15 && lon in 114.55..116.6 -> "Kalimantan Selatan"
            lat in -3.15..-1.7 && lon in 115.05..116.6 -> "Kalimantan Selatan"
            lat in -3.15..-2.95 && lon in 114.56..114.8 -> "Kalimantan Selatan"
            // Default Kalimantan Tengah (Mantangai, Kapuas, Palangka Raya, dll.)
            else -> "Kalimantan Tengah"
        }
    }

    /**
     * Menentukan estimasi kabupaten terdekat di Kalimantan Tengah / Selatan untuk konteks lapangan.
     */
    fun determineRegency(lat: Double, lon: Double, province: String): String {
        if (province == "Kalimantan Tengah") {
            return when {
                // Hardi Mantangai / Kapuas Hilir
                lat in -2.6..-2.0 && lon in 114.2..114.8 -> "Kab. Kapuas (Mantangai)"
                lat in -3.2..-2.6 && lon in 114.0..114.6 -> "Kab. Kapuas"
                lat in -2.4..-1.9 && lon in 113.7..114.1 -> "Kota Palangka Raya"
                lat in -3.0..-2.4 && lon in 113.9..114.4 -> "Kab. Pulang Pisau"
                lat in -2.2..-1.2 && lon in 113.0..113.8 -> "Kab. Katingan"
                lat in -2.8..-2.0 && lon in 112.5..113.2 -> "Kab. Kotawaringin Timur"
                lat in -2.9..-2.2 && lon in 111.3..112.0 -> "Kab. Kotawaringin Barat"
                else -> "Kalimantan Tengah"
            }
        }
        return province
    }

    data class TimestampInfo(
        val epochMs: Long,
        val witaFormatted: String,
        val wibFormatted: String
    )

    private val MONTH_NAMES_ID = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    /**
     * Mengurai acqDate (YYYY-MM-DD) dan acqTime (HHMM) UTC dari satelit NASA
     * ke UTC epoch ms, serta memformat waktu lokal WITA (UTC+8) dan WIB (UTC+7).
     */
    fun parseSatelliteUtcTime(acqDate: String, acqTime: String): TimestampInfo {
        return try {
            val padTime = acqTime.padStart(4, '0')
            val parts = acqDate.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val month = parts.getOrNull(1)?.toIntOrNull() ?: 9
            val day = parts.getOrNull(2)?.toIntOrNull() ?: 14
            val hour = padTime.substring(0, 2).toInt().coerceIn(0, 23)
            val min = padTime.substring(2, 4).toInt().coerceIn(0, 59)

            val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(year, month - 1, day, hour, min, 0)
            }
            val epochMs = calUtc.timeInMillis

            // Format WITA = UTC + 8 jam
            val calWita = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).apply {
                timeInMillis = epochMs
            }
            val witaDay = calWita.get(Calendar.DAY_OF_MONTH)
            val witaMonth = MONTH_NAMES_ID[calWita.get(Calendar.MONTH)]
            val witaYear = calWita.get(Calendar.YEAR)
            val witaHour = calWita.get(Calendar.HOUR_OF_DAY)
            val witaMin = calWita.get(Calendar.MINUTE)
            val witaFormatted = String.format(
                Locale.US,
                "%02d %s %d %02d:%02d WITA",
                witaDay, witaMonth, witaYear, witaHour, witaMin
            )

            // Format WIB = UTC + 7 jam
            val calWib = Calendar.getInstance(TimeZone.getTimeZone("GMT+7")).apply {
                timeInMillis = epochMs
            }
            val wibDay = calWib.get(Calendar.DAY_OF_MONTH)
            val wibMonth = MONTH_NAMES_ID[calWib.get(Calendar.MONTH)]
            val wibYear = calWib.get(Calendar.YEAR)
            val wibHour = calWib.get(Calendar.HOUR_OF_DAY)
            val wibMin = calWib.get(Calendar.MINUTE)
            val wibFormatted = String.format(
                Locale.US,
                "%02d %s %d %02d:%02d WIB",
                wibDay, wibMonth, wibYear, wibHour, wibMin
            )

            TimestampInfo(epochMs, witaFormatted, wibFormatted)
        } catch (e: Exception) {
            val now = System.currentTimeMillis()
            TimestampInfo(now, "$acqDate $acqTime (WITA)", "$acqDate $acqTime (WIB)")
        }
    }

    /**
     * Menghitung umur data aktual dari waktu observasi satelit terhadap waktu sekarang.
     * Mengembalikan selisih menit, format teks, dan kategori resmi:
     * 🟢 0–60 menit: "TERBARU"
     * 🟡 61–180 menit: "NRT"
     * 🟠 181–360 menit: "DATA LEBIH LAMA"
     * 🔴 >360 menit: "DATA LAMA"
     */
    fun calculateDataAge(observationEpochMs: Long, currentEpochMs: Long = System.currentTimeMillis()): Triple<Long, String, DataAgeCategory> {
        val diffMs = currentEpochMs - observationEpochMs
        val ageMinutes = max(0L, diffMs / 60000L)

        val formatted = when {
            ageMinutes < 1 -> "Baru saja (< 1 mnt)"
            ageMinutes < 60 -> "$ageMinutes menit"
            ageMinutes < 1440 -> {
                val h = ageMinutes / 60
                val m = ageMinutes % 60
                if (m > 0) "$h jam $m mnt" else "$h jam"
            }
            else -> {
                val d = ageMinutes / 1440
                val h = (ageMinutes % 1440) / 60
                if (h > 0) "$d hari $h jam" else "$d hari"
            }
        }

        val category = when {
            ageMinutes <= 60 -> DataAgeCategory.TERBARU
            ageMinutes <= 180 -> DataAgeCategory.NRT
            ageMinutes <= 360 -> DataAgeCategory.DATA_LEBIH_LAMA
            else -> DataAgeCategory.DATA_LAMA
        }

        return Triple(ageMinutes, formatted, category)
    }

    /**
     * Format timestamp sistem (misal saat sinkronisasi sukses) ke string WITA dan WIB.
     */
    fun formatSyncTime(epochMs: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).apply {
            timeInMillis = epochMs
        }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return String.format(Locale.US, "%02d:%02d WITA", h, m)
    }
}

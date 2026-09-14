package com.example.data.repository

import android.util.Log
import com.example.data.local.AppPreferences
import com.example.data.local.FireDatabase
import com.example.data.local.FireHotspotEntity
import com.example.data.model.DataAgeCategory
import com.example.data.model.DataConnectionStatus
import com.example.data.model.FilterState
import com.example.data.model.FireHotspot
import com.example.data.model.UserLocation
import com.example.data.remote.FirmsApiService
import com.example.util.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class HotspotRepository(
    private val database: FireDatabase,
    private val apiService: FirmsApiService = FirmsApiService.create(),
    private val preferences: AppPreferences? = null
) {
    private val dao = database.hotspotDao()
    private val TAG = "HotspotRepository"

    // Set ID hotspot yang sudah pernah dinotifikasikan untuk mencegah spam duplikat
    private val notifiedHotspotIds = ConcurrentHashMap.newKeySet<String>()

    /**
     * Memperoleh aliran data hotspot dari Room DB yang telah dipetakan ke domain model,
     * diperkaya dengan jarak geodesik dan arah kompas dari lokasi pengguna jika GPS aktif.
     */
    fun getHotspotsFlow(userLocation: UserLocation? = null): Flow<List<FireHotspot>> {
        return dao.getAllHotspotsFlow().map { entities ->
            val now = System.currentTimeMillis()
            entities.map { entity ->
                val distance = userLocation?.let { loc ->
                    GeoUtils.calculateHaversineDistanceKm(loc.latitude, loc.longitude, entity.latitude, entity.longitude)
                }
                val bearing = userLocation?.let { loc ->
                    GeoUtils.calculateBearing(loc.latitude, loc.longitude, entity.latitude, entity.longitude).direction
                }
                entity.toDomainModel(distance, bearing, now)
            }
        }
    }

    suspend fun getCachedCount(): Int = withContext(Dispatchers.IO) {
        dao.getCount()
    }

    suspend fun getLastRetrievedTimestamp(): Long? = withContext(Dispatchers.IO) {
        dao.getLastRetrievedAt()
    }

    data class RefreshResult(
        val isSuccess: Boolean,
        val newHotspotsCount: Int,
        val totalValidHotspots: Int,
        val newlyDetectedForNotification: List<FireHotspot>,
        val connectionStatus: DataConnectionStatus,
        val message: String
    )

    /**
     * Mengunduh data satelit aktual NASA FIRMS:
     * 1. Menggunakan NASA Area API jika mapKey dikonfigurasi.
     * 2. Atau menggunakan Feed CSV NRT Resmi Asia Tenggara terbuka dari NASA jika mapKey belum diisi.
     * 3. Memvalidasi koordinat dan timestamp observasi.
     * 4. Membuang koordinat palsu / di luar Kalimantan.
     * 5. Melakukan deduplikasi ketat.
     * 6. Menyimpan ke cache Room DB.
     */
    suspend fun refreshHotspots(
        mapKeyOverride: String? = null,
        userLocation: UserLocation? = null
    ): RefreshResult = withContext(Dispatchers.IO) {
        val mapKey = mapKeyOverride ?: preferences?.nasaFirmsMapKey ?: ""
        val now = System.currentTimeMillis()
        val allParsed = mutableListOf<FireHotspotEntity>()
        var apiSuccess = false
        var errorMessage = ""

        try {
            // STRATEGI 1: Jika user memiliki NASA FIRMS MAP_KEY terdaftar
            if (mapKey.isNotBlank() && mapKey.length >= 10) {
                val bbox = "108.0,-4.5,119.2,4.3" // Kalimantan Bounding Box
                val sources = listOf("VIIRS_NOAA21_NRT", "VIIRS_NOAA20_NRT", "VIIRS_SNPP_NRT", "MODIS_NRT")

                for (src in sources) {
                    try {
                        val response = apiService.getAreaCsv(mapKey.trim(), src, bbox, 1)
                        if (response.isSuccessful && response.body() != null) {
                            val csvString = response.body()!!.string()
                            if (!csvString.contains("Invalid API", ignoreCase = true) &&
                                !csvString.contains("Invalid map key", ignoreCase = true)) {
                                val list = parseFirmsCsv(csvString, src, now)
                                allParsed.addAll(list)
                                apiSuccess = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Gagal mengambil feed area $src: ${e.message}")
                    }
                }
            }

            // STRATEGI 2: Jika Area API belum menghasilkan data / mapKey kosong,
            // gunakan Feed NRT Resmi NASA FIRMS SouthEast Asia 24h
            if (allParsed.isEmpty()) {
                val openFeeds = listOf(
                    FirmsApiService.FEED_VIIRS_NOAA21 to "NOAA-21",
                    FirmsApiService.FEED_VIIRS_NOAA20 to "NOAA-20",
                    FirmsApiService.FEED_VIIRS_SNPP to "Suomi-NPP",
                    FirmsApiService.FEED_MODIS to "MODIS"
                )

                for ((url, satTag) in openFeeds) {
                    try {
                        val response = apiService.getDirectCsv(url)
                        if (response.isSuccessful && response.body() != null) {
                            val csvString = response.body()!!.string()
                            val list = parseFirmsCsv(csvString, satTag, now)
                            allParsed.addAll(list)
                            apiSuccess = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Gagal mengambil direct feed $satTag: ${e.message}")
                    }
                }
            }

            if (apiSuccess && allParsed.isNotEmpty()) {
                // Deduplikasi berdasarkan ID unik (satelit, lat, lon, acqDate, acqTime)
                val uniqueHotspots = allParsed.distinctBy { it.id }

                // Identifikasi hotspot baru yang belum pernah tercatat di database lokal
                val existingIds = dao.getAllHotspots().map { it.id }.toSet()
                val newlyAdded = uniqueHotspots.filter { !existingIds.contains(it.id) }

                // Bersihkan data lama di atas 7 hari dan masukkan data valid terbaru
                val sevenDaysAgo = now - (7L * 24L * 3600L * 1000L)
                dao.deleteOlderThan(sevenDaysAgo)
                dao.insertAll(uniqueHotspots)

                preferences?.apply {
                    lastSuccessfulSyncEpochMs = now
                    isLastSyncSuccess = true
                    lastSyncStatus = "Sinkronisasi berhasil: ${GeoUtils.formatSyncTime(now)}"
                }

                // Siapkan hotspot baru yang memenuhi kriteria peringatan lapangan
                val newlyDetectedModels = newlyAdded.map { entity ->
                    val dist = userLocation?.let { loc ->
                        GeoUtils.calculateHaversineDistanceKm(loc.latitude, loc.longitude, entity.latitude, entity.longitude)
                    }
                    val bearing = userLocation?.let { loc ->
                        GeoUtils.calculateBearing(loc.latitude, loc.longitude, entity.latitude, entity.longitude).direction
                    }
                    entity.toDomainModel(dist, bearing, now)
                }.filter { h ->
                    !notifiedHotspotIds.contains(h.id)
                }

                // Catat ID agar tidak pernah dinotifikasi ulang
                newlyDetectedModels.forEach { notifiedHotspotIds.add(it.id) }

                // Tentukan status data berdasarkan umur data teranyar yang diterima
                val latestEpoch = uniqueHotspots.maxOfOrNull { it.observationEpochMs } ?: now
                val ageMinutes = (now - latestEpoch) / 60000L
                val connStatus = when {
                    ageMinutes <= 60 -> DataConnectionStatus.DATA_TERBARU
                    ageMinutes <= 180 -> DataConnectionStatus.NRT
                    else -> DataConnectionStatus.DATA_TERTUNDA
                }

                RefreshResult(
                    isSuccess = true,
                    newHotspotsCount = newlyAdded.size,
                    totalValidHotspots = uniqueHotspots.size,
                    newlyDetectedForNotification = newlyDetectedModels,
                    connectionStatus = connStatus,
                    message = "Berhasil memperbarui ${uniqueHotspots.size} titik panas satelit aktual."
                )
            } else {
                // JIKA REQUEST GAGAL: Jangan buat data palsu!
                val cachedCount = dao.getCount()
                val lastSyncTime = preferences?.lastSuccessfulSyncEpochMs ?: 0L
                val lastSyncStr = if (lastSyncTime > 0) GeoUtils.formatSyncTime(lastSyncTime) else "-"

                preferences?.apply {
                    isLastSyncSuccess = false
                    lastSyncStatus = "Sinkronisasi gagal — menampilkan data tersimpan"
                }

                if (cachedCount > 0) {
                    RefreshResult(
                        isSuccess = false,
                        newHotspotsCount = 0,
                        totalValidHotspots = cachedCount,
                        newlyDetectedForNotification = emptyList(),
                        connectionStatus = DataConnectionStatus.DATA_TERTUNDA,
                        message = "Koneksi satelit terputus. Menampilkan $cachedCount data cache tersimpan (Sinkron terakhir: $lastSyncStr)."
                    )
                } else {
                    RefreshResult(
                        isSuccess = false,
                        newHotspotsCount = 0,
                        totalValidHotspots = 0,
                        newlyDetectedForNotification = emptyList(),
                        connectionStatus = DataConnectionStatus.DATA_TIDAK_TERSEDIA,
                        message = "DATA SATELIT TIDAK TERSEDIA — Periksa jaringan internet Anda."
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saat refresh data satelit: ${e.message}", e)
            val cachedCount = dao.getCount()
            RefreshResult(
                isSuccess = false,
                newHotspotsCount = 0,
                totalValidHotspots = cachedCount,
                newlyDetectedForNotification = emptyList(),
                connectionStatus = if (cachedCount > 0) DataConnectionStatus.DATA_TERTUNDA else DataConnectionStatus.DATA_TIDAK_TERSEDIA,
                message = "Sinkronisasi gagal: ${e.localizedMessage ?: "Gangguan jaringan"}"
            )
        }
    }

    /**
     * Mem-parsing format CSV resmi NASA FIRMS, memvalidasi koordinat,
     * menyaring batas geografis Kalimantan, mengkonversi waktu observasi satelit.
     */
    private fun parseFirmsCsv(csv: String, sourceTag: String, retrievedTimeMs: Long): List<FireHotspotEntity> {
        val lines = csv.lines()
        if (lines.size < 2) return emptyList()

        val header = lines.first().split(",").map { it.trim().lowercase() }
        val latIdx = header.indexOf("latitude")
        val lonIdx = header.indexOf("longitude")
        val dateIdx = header.indexOf("acq_date")
        val timeIdx = header.indexOf("acq_time")
        val satIdx = header.indexOf("satellite")
        val instIdx = header.indexOf("instrument")
        val confIdx = header.indexOf("confidence")
        val frpIdx = header.indexOf("frp")
        val scanIdx = header.indexOf("scan")
        val trackIdx = header.indexOf("track")
        val dnIdx = header.indexOf("daynight")
        val verIdx = header.indexOf("version")

        if (latIdx == -1 || lonIdx == -1) return emptyList()

        val results = mutableListOf<FireHotspotEntity>()

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val parts = line.split(",")
            if (parts.size <= maxOf(latIdx, lonIdx)) continue

            val lat = parts.getOrNull(latIdx)?.toDoubleOrNull() ?: continue
            val lon = parts.getOrNull(lonIdx)?.toDoubleOrNull() ?: continue

            // Validasi ketat: Hanya wilayah daratan Kalimantan Indonesia
            if (!GeoUtils.isWithinKalimantan(lat, lon)) continue

            val date = parts.getOrNull(dateIdx)?.trim() ?: ""
            val time = parts.getOrNull(timeIdx)?.trim()?.padStart(4, '0') ?: "0000"
            if (date.isEmpty()) continue

            val rawSat = parts.getOrNull(satIdx)?.trim() ?: sourceTag
            val satName = when {
                rawSat.contains("21", ignoreCase = true) || rawSat.equals("2", ignoreCase = true) -> "NOAA-21"
                rawSat.contains("20", ignoreCase = true) || rawSat.equals("1", ignoreCase = true) -> "NOAA-20"
                rawSat.contains("NPP", ignoreCase = true) || rawSat.equals("N", ignoreCase = true) -> "Suomi-NPP"
                rawSat.contains("Terra", ignoreCase = true) || rawSat.equals("T", ignoreCase = true) -> "MODIS Terra"
                rawSat.contains("Aqua", ignoreCase = true) || rawSat.equals("A", ignoreCase = true) -> "MODIS Aqua"
                else -> sourceTag
            }

            val inst = parts.getOrNull(instIdx)?.trim() ?: if (satName.contains("MODIS")) "MODIS" else "VIIRS"
            val confRaw = parts.getOrNull(confIdx)?.trim() ?: "nominal"
            val frp = parts.getOrNull(frpIdx)?.toDoubleOrNull() ?: 0.0
            val scan = parts.getOrNull(scanIdx)?.toDoubleOrNull() ?: 0.375
            val track = parts.getOrNull(trackIdx)?.toDoubleOrNull() ?: 0.375
            val dn = parts.getOrNull(dnIdx)?.trim()?.uppercase() ?: "D"
            val ver = parts.getOrNull(verIdx)?.trim() ?: "2.0NRT"

            // Normalisasi tingkat keyakinan (confidence)
            val conf = when {
                confRaw.equals("h", true) || confRaw.equals("high", true) -> "high"
                confRaw.equals("l", true) || confRaw.equals("low", true) -> "low"
                else -> {
                    val num = confRaw.toIntOrNull()
                    if (num != null) {
                        when {
                            num >= 80 -> "high"
                            num >= 30 -> "nominal"
                            else -> "low"
                        }
                    } else "nominal"
                }
            }

            val timeInfo = GeoUtils.parseSatelliteUtcTime(date, time)
            val prov = GeoUtils.determineProvince(lat, lon)
            val regency = GeoUtils.determineRegency(lat, lon, prov)

            // ID Unik berbasis telemetry satelit asli
            val id = "FIRMS-${satName.replace(" ", "")}-${String.format(java.util.Locale.US, "%.5f", lat)}_${String.format(java.util.Locale.US, "%.5f", lon)}-$date-$time"

            results.add(
                FireHotspotEntity(
                    id = id,
                    latitude = lat,
                    longitude = lon,
                    acqDate = date,
                    acqTime = time,
                    satellite = satName,
                    instrument = inst,
                    confidence = conf,
                    confidenceRaw = confRaw,
                    frp = frp,
                    scan = scan,
                    track = track,
                    dayNight = dn,
                    version = ver,
                    source = "NASA FIRMS ($satName)",
                    retrievedAt = retrievedTimeMs,
                    observationEpochMs = timeInfo.epochMs,
                    province = prov,
                    regency = regency
                )
            )
        }

        return results
    }

    /**
     * Menyaring daftar hotspot domain berdasarkan parameter filter.
     * Menggunakan WAKTU OBSERVASI SATELIT (observationEpochMs), bukan waktu data diterima.
     */
    fun applyFilters(hotspots: List<FireHotspot>, filter: FilterState): List<FireHotspot> {
        val now = System.currentTimeMillis()

        return hotspots.filter { h ->
            // 1. Filter Mode "HOTSPOT TERBARU" (hanya yang berumur <= 180 menit)
            if (filter.onlyLatestHotspots && h.dataAgeMinutes > 180) {
                return@filter false
            }

            // 2. Filter Waktu Observasi Satelit Aktual (bukan waktu retrieve)
            val matchTime = when (filter.timeRange) {
                "1h" -> h.dataAgeMinutes <= 60
                "3h" -> h.dataAgeMinutes <= 180
                "6h" -> h.dataAgeMinutes <= 360
                "12h" -> h.dataAgeMinutes <= 720
                "24h" -> h.dataAgeMinutes <= 1440
                "48h" -> h.dataAgeMinutes <= 2880
                else -> true // "all"
            }
            if (!matchTime) return@filter false

            // 3. Filter Tingkat Keyakinan (Confidence)
            val matchConf = when (filter.confidence) {
                "high" -> h.confidence == "high"
                "nominal" -> h.confidence == "nominal"
                "low" -> h.confidence == "low"
                else -> true
            }
            if (!matchConf) return@filter false

            // 4. Filter FRP Minimum
            if (h.frp < filter.minFrp) return@filter false

            // 5. Filter Satelit / Sensor
            val matchSat = when (filter.satellite) {
                "VIIRS" -> h.instrument.contains("VIIRS", ignoreCase = true)
                "MODIS" -> h.instrument.contains("MODIS", ignoreCase = true)
                "NOAA-21" -> h.satellite.contains("NOAA-21", ignoreCase = true)
                "NOAA-20" -> h.satellite.contains("NOAA-20", ignoreCase = true)
                "Suomi-NPP" -> h.satellite.contains("Suomi", ignoreCase = true)
                else -> true
            }
            if (!matchSat) return@filter false

            // 6. Filter Wilayah Provinsi
            val matchProv = if (filter.province == "all") true else h.province.equals(filter.province, ignoreCase = true)
            if (!matchProv) return@filter false

            // 7. Filter Radius dari Pengguna (km)
            if (filter.nearbyRadiusKm > 0) {
                if (h.distanceKm == null || h.distanceKm > filter.nearbyRadiusKm) {
                    return@filter false
                }
            }

            // 8. Filter Pencarian Teks
            if (filter.searchQuery.isNotBlank()) {
                val q = filter.searchQuery.trim().lowercase()
                val matchesText = h.province.lowercase().contains(q) ||
                        h.regency.lowercase().contains(q) ||
                        h.satellite.lowercase().contains(q) ||
                        h.id.lowercase().contains(q) ||
                        "${h.latitude},${h.longitude}".contains(q)
                if (!matchesText) return@filter false
            }

            true
        }
    }
}

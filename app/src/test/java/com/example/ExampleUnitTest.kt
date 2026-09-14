package com.example

import com.example.data.model.DataAgeCategory
import com.example.data.model.FilterState
import com.example.data.model.FireHotspot
import com.example.data.model.UserLocation
import com.example.util.GeoUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Pengujian komprehensif seluruh fungsi inti aplikasi monitoring titik api:
 * Mencakup 15 kriteria uji wajib (TEST 1 - TEST 15).
 */
class ExampleUnitTest {

    // TEST 1: Haversine distance akurat
    @Test
    fun test1_haversineDistanceCalculation() {
        val lat1 = -2.3456 // Mantangai
        val lon1 = 114.4823
        val lat2 = -2.2161 // Palangka Raya
        val lon2 = 113.9139

        val dist = GeoUtils.calculateHaversineDistanceKm(lat1, lon1, lat2, lon2)
        // Jarak Mantangai ke Palangka Raya berkisar ~64 km
        assertTrue("Jarak Mantangai ke Palangka Raya harus berkisar ~64 km", dist in 55.0..75.0)

        // Jarak ke titik yang sama harus 0
        val distSame = GeoUtils.calculateHaversineDistanceKm(lat1, lon1, lat1, lon1)
        assertEquals(0.0, distSame, 0.0001)
    }

    // TEST 2: Deteksi Arah Kompas (Bearing)
    @Test
    fun test2_bearingCompassDirection() {
        val northBearing = GeoUtils.calculateBearing(0.0, 114.0, 1.0, 114.0)
        assertEquals("Utara", northBearing.direction)

        val southBearing = GeoUtils.calculateBearing(0.0, 114.0, -1.0, 114.0)
        assertEquals("Selatan", southBearing.direction)

        val eastBearing = GeoUtils.calculateBearing(0.0, 114.0, 0.0, 115.0)
        assertEquals("Timur", eastBearing.direction)

        val westBearing = GeoUtils.calculateBearing(0.0, 114.0, 0.0, 113.0)
        assertEquals("Barat", westBearing.direction)
    }

    // TEST 3: Penentuan Provinsi Kalimantan
    @Test
    fun test3_determineProvince() {
        assertEquals("Kalimantan Tengah", GeoUtils.determineProvince(-2.3456, 114.4823)) // Mantangai
        assertEquals("Kalimantan Tengah", GeoUtils.determineProvince(-2.2161, 113.9139)) // Palangka Raya
        assertEquals("Kalimantan Selatan", GeoUtils.determineProvince(-3.3194, 114.5908)) // Banjarmasin
        assertEquals("Kalimantan Barat", GeoUtils.determineProvince(-0.0263, 109.3425)) // Pontianak
        assertEquals("Kalimantan Timur", GeoUtils.determineProvince(-0.5016, 117.1265)) // Samarinda
        assertEquals("Kalimantan Utara", GeoUtils.determineProvince(2.8375, 117.3653)) // Tanjung Selor
    }

    // TEST 4: Penyaringan Batas Geografis Kalimantan (Menolak Koordinat Luar Pulau/Laut)
    @Test
    fun test4_isWithinKalimantanTerritory() {
        // Kalimantan valid
        assertTrue(GeoUtils.isWithinKalimantan(-2.3456, 114.4823)) // Mantangai
        assertTrue(GeoUtils.isWithinKalimantan(-0.0263, 109.3425)) // Pontianak

        // Luar Kalimantan
        assertFalse(GeoUtils.isWithinKalimantan(-6.2088, 106.8456)) // Jakarta (Jawa)
        assertFalse(GeoUtils.isWithinKalimantan(0.0, 0.0)) // Null Island
        assertFalse(GeoUtils.isWithinKalimantan(-5.5, 114.0)) // Laut Jawa
        assertFalse(GeoUtils.isWithinKalimantan(5.0, 115.0)) // Sabah / Laut Cina Selatan
        assertFalse(GeoUtils.isWithinKalimantan(Double.NaN, 114.0)) // NaN
    }

    // TEST 5: Parsing Waktu Observasi Satelit UTC -> WITA (+8) & WIB (+7)
    @Test
    fun test5_utcToWitaAndWibTimezone() {
        // Contoh: Satelit melintas pada 14 September 2026 pukul 06:30 UTC
        // WITA (UTC+8) = 14:30 WITA
        // WIB (UTC+7) = 13:30 WIB
        val timeInfo = GeoUtils.parseSatelliteUtcTime("2026-09-14", "0630")

        assertTrue("WITA harus mengandung 14:30 WITA", timeInfo.witaFormatted.contains("14:30 WITA"))
        assertTrue("WIB harus mengandung 13:30 WIB", timeInfo.wibFormatted.contains("13:30 WIB"))

        // Contoh lewat tengah malam UTC: 2026-09-14 20:00 UTC
        // WITA = 15 September 2026 04:00 WITA
        val timeOverMidnight = GeoUtils.parseSatelliteUtcTime("2026-09-14", "2000")
        assertTrue("Harus berpindah ke 15 Sep pada WITA", timeOverMidnight.witaFormatted.contains("15 Sep") && timeOverMidnight.witaFormatted.contains("04:00 WITA"))
    }

    // TEST 6: Klasifikasi Umur Data (🟢 TERBARU, 🟡 NRT, 🟠 DATA LEBIH LAMA, 🔴 DATA LAMA)
    @Test
    fun test6_dataAgeClassification() {
        val now = System.currentTimeMillis()

        // 30 menit lalu -> TERBARU
        val (age30m, _, cat30m) = GeoUtils.calculateDataAge(now - 30 * 60 * 1000L, now)
        assertEquals(30L, age30m)
        assertEquals(DataAgeCategory.TERBARU, cat30m)

        // 120 menit (2 jam) lalu -> NRT
        val (age2h, _, cat2h) = GeoUtils.calculateDataAge(now - 120 * 60 * 1000L, now)
        assertEquals(120L, age2h)
        assertEquals(DataAgeCategory.NRT, cat2h)

        // 240 menit (4 jam) lalu -> DATA LEBIH LAMA
        val (age4h, _, cat4h) = GeoUtils.calculateDataAge(now - 240 * 60 * 1000L, now)
        assertEquals(240L, age4h)
        assertEquals(DataAgeCategory.DATA_LEBIH_LAMA, cat4h)

        // 600 menit (10 jam) lalu -> DATA LAMA
        val (age10h, _, cat10h) = GeoUtils.calculateDataAge(now - 600 * 60 * 1000L, now)
        assertEquals(600L, age10h)
        assertEquals(DataAgeCategory.DATA_LAMA, cat10h)
    }

    // TEST 7: Filter Waktu Berdasarkan Observasi Satelit
    @Test
    fun test7_timeFilteringBasedOnSatelliteObservation() {
        val now = System.currentTimeMillis()

        val hRecent = createMockHotspot("H1", now - 30 * 60 * 1000L)  // 30 menit
        val hMedium = createMockHotspot("H2", now - 150 * 60 * 1000L) // 2.5 jam
        val hOld = createMockHotspot("H3", now - 400 * 60 * 1000L)    // 6.6 jam

        val list = listOf(hRecent, hMedium, hOld)

        // Filter 1h
        val f1h = list.filter { it.dataAgeMinutes <= 60 }
        assertEquals(1, f1h.size)
        assertEquals("H1", f1h.first().id)

        // Filter 3h
        val f3h = list.filter { it.dataAgeMinutes <= 180 }
        assertEquals(2, f3h.size)

        // Filter 6h
        val f6h = list.filter { it.dataAgeMinutes <= 360 }
        assertEquals(2, f6h.size)

        // Filter 12h
        val f12h = list.filter { it.dataAgeMinutes <= 720 }
        assertEquals(3, f12h.size)
    }

    // TEST 8: Filter Radius dari Pengguna
    @Test
    fun test8_radiusFromUserFiltering() {
        val userLoc = UserLocation(latitude = -2.3456, longitude = 114.4823) // Mantangai

        // Hotspot 3 km dari Mantangai
        val hNear = createMockHotspot("NEAR", System.currentTimeMillis(), -2.3600, 114.4900).copy(
            distanceKm = GeoUtils.calculateHaversineDistanceKm(userLoc.latitude, userLoc.longitude, -2.3600, 114.4900)
        )
        // Hotspot 35 km dari Mantangai
        val hFar = createMockHotspot("FAR", System.currentTimeMillis(), -2.1000, 114.2000).copy(
            distanceKm = GeoUtils.calculateHaversineDistanceKm(userLoc.latitude, userLoc.longitude, -2.1000, 114.2000)
        )

        assertTrue(hNear.distanceKm!! < 5.0)
        assertTrue(hFar.distanceKm!! > 25.0)

        val list = listOf(hNear, hFar)

        // Filter radius 5 km
        val within5km = list.filter { it.distanceKm!! <= 5.0 }
        assertEquals(1, within5km.size)
        assertEquals("NEAR", within5km.first().id)

        // Filter radius 50 km
        val within50km = list.filter { it.distanceKm!! <= 50.0 }
        assertEquals(2, within50km.size)
    }

    // TEST 9: Deduplikasi ID Hotspot Unik
    @Test
    fun test9_hotspotDeduplication() {
        val h1 = createMockHotspot("FIRMS-NOAA21--2.34_114.48-20260914-0630", System.currentTimeMillis())
        val h2 = createMockHotspot("FIRMS-NOAA21--2.34_114.48-20260914-0630", System.currentTimeMillis())
        val h3 = createMockHotspot("FIRMS-NOAA20--2.10_114.20-20260914-0635", System.currentTimeMillis())

        val list = listOf(h1, h2, h3)
        val deduplicated = list.distinctBy { it.id }

        assertEquals(2, deduplicated.size)
    }

    // TEST 10: Fallback Lokasi Mantangai Jika GPS Mati/Ditolak
    @Test
    fun test10_gpsDisabledFallbackLocation() {
        val fallbackLoc = UserLocation(
            latitude = GeoUtils.HARDI_MANTANGAI_LAT,
            longitude = GeoUtils.HARDI_MANTANGAI_LON,
            accuracy = 10f
        )

        assertEquals(-2.3456, fallbackLoc.latitude, 0.0001)
        assertEquals(114.4823, fallbackLoc.longitude, 0.0001)
    }

    // TEST 11: Format Waktu Sinkronisasi Terakhir
    @Test
    fun test11_lastSyncTimeFormatting() {
        val epochMs = GeoUtils.parseSatelliteUtcTime("2026-09-14", "1000").epochMs // 10:00 UTC = 18:00 WITA
        val syncStr = GeoUtils.formatSyncTime(epochMs)
        assertEquals("18:00 WITA", syncStr)
    }

    // TEST 12: Penentuan Estimasi Kabupaten
    @Test
    fun test12_regencyDetermination() {
        val regencyMantangai = GeoUtils.determineRegency(-2.3456, 114.4823, "Kalimantan Tengah")
        assertEquals("Kab. Kapuas (Mantangai)", regencyMantangai)

        val regencyPky = GeoUtils.determineRegency(-2.2161, 113.9139, "Kalimantan Tengah")
        assertEquals("Kota Palangka Raya", regencyPky)
    }

    // TEST 13: Mode "HOTSPOT TERBARU" (Filter Umur <= 180 menit)
    @Test
    fun test13_onlyLatestHotspotsMode() {
        val now = System.currentTimeMillis()
        val h1 = createMockHotspot("H1", now - 45 * 60 * 1000L) // 45 mnt (TERBARU)
        val h2 = createMockHotspot("H2", now - 120 * 60 * 1000L) // 120 mnt (NRT)
        val h3 = createMockHotspot("H3", now - 300 * 60 * 1000L) // 300 mnt (DATA LEBIH LAMA)

        val all = listOf(h1, h2, h3)
        val onlyLatest = all.filter { it.dataAgeMinutes <= 180 }

        assertEquals(2, onlyLatest.size)
        assertTrue(onlyLatest.any { it.id == "H1" })
        assertTrue(onlyLatest.any { it.id == "H2" })
        assertFalse(onlyLatest.any { it.id == "H3" })
    }

    // TEST 14: Filter FRP Minimum
    @Test
    fun test14_minFrpThreshold() {
        val hLow = createMockHotspot("LOW", System.currentTimeMillis()).copy(frp = 8.5)
        val hMed = createMockHotspot("MED", System.currentTimeMillis()).copy(frp = 25.0)
        val hHigh = createMockHotspot("HIGH", System.currentTimeMillis()).copy(frp = 75.0)

        val list = listOf(hLow, hMed, hHigh)

        val min20 = list.filter { it.frp >= 20.0 }
        assertEquals(2, min20.size)

        val min50 = list.filter { it.frp >= 50.0 }
        assertEquals(1, min50.size)
        assertEquals("HIGH", min50.first().id)
    }

    // TEST 15: Penanganan Nilai Tak Berhingga (NaN/Infinity Immunity)
    @Test
    fun test15_nanImmunity() {
        val dist = GeoUtils.calculateHaversineDistanceKm(Double.NaN, 114.0, -2.0, 114.0)
        assertEquals(0.0, dist, 0.0001)

        val bearing = GeoUtils.calculateBearing(Double.NaN, 114.0, -2.0, 114.0)
        assertEquals("Utara", bearing.direction)

        val isWithin = GeoUtils.isWithinKalimantan(Double.NaN, Double.POSITIVE_INFINITY)
        assertFalse(isWithin)
    }

    private fun createMockHotspot(
        id: String,
        observationEpochMs: Long,
        lat: Double = -2.3456,
        lon: Double = 114.4823
    ): FireHotspot {
        val (ageMin, ageFormatted, ageCat) = GeoUtils.calculateDataAge(observationEpochMs)
        return FireHotspot(
            id = id,
            latitude = lat,
            longitude = lon,
            acqDate = "2026-09-14",
            acqTime = "0600",
            satellite = "NOAA-21",
            instrument = "VIIRS",
            confidence = "high",
            confidenceRaw = "h",
            frp = 32.5,
            scan = 0.375,
            track = 0.375,
            dayNight = "D",
            version = "2.0NRT",
            source = "NASA FIRMS",
            retrievedAt = System.currentTimeMillis(),
            observationEpochMs = observationEpochMs,
            timeWita = "14 Sep 2026 14:00 WITA",
            timeWib = "14 Sep 2026 13:00 WIB",
            dataAgeMinutes = ageMin,
            dataAgeFormatted = ageFormatted,
            dataAgeCategory = ageCat,
            province = GeoUtils.determineProvince(lat, lon),
            regency = GeoUtils.determineRegency(lat, lon, "Kalimantan Tengah")
        )
    }
}

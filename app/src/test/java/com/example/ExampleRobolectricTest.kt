package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FireDatabase
import com.example.data.local.FireHotspotEntity
import com.example.data.model.DataAgeCategory
import com.example.util.GeoUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun read_appName_from_context() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HARDI MANTANGAI FIRE MONITOR", appName)
    }

    @Test
    fun database_insertion_and_domain_mapping() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = FireDatabase.getInstance(context)
        val dao = db.hotspotDao()
        dao.clearAll()

        val entity = FireHotspotEntity(
            id = "FIRMS-TEST-1",
            latitude = -2.3456,
            longitude = 114.4823,
            acqDate = "2026-09-14",
            acqTime = "0600",
            satellite = "NOAA-21",
            instrument = "VIIRS",
            confidence = "high",
            confidenceRaw = "h",
            frp = 45.8,
            scan = 0.375,
            track = 0.375,
            dayNight = "D",
            version = "2.0NRT",
            source = "NASA FIRMS",
            retrievedAt = System.currentTimeMillis(),
            observationEpochMs = System.currentTimeMillis() - (30 * 60 * 1000L), // 30 menit lalu
            province = "Kalimantan Tengah",
            regency = "Kab. Kapuas (Mantangai)"
        )

        dao.insertAll(listOf(entity))
        assertEquals(1, dao.getCount())

        val retrieved = dao.getAllHotspots().first()
        val domain = retrieved.toDomainModel()

        assertEquals("FIRMS-TEST-1", domain.id)
        assertEquals(DataAgeCategory.TERBARU, domain.dataAgeCategory)
        assertTrue(domain.dataAgeMinutes in 29..31)
        assertEquals("Kalimantan Tengah", domain.province)
    }
}

package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.DataAgeCategory
import com.example.data.model.FireHotspot
import com.example.ui.screens.HotspotListItemCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun hotspot_card_screenshot() {
        val sample = FireHotspot(
            id = "HOTSPOT-SAMPLE",
            latitude = -2.3456,
            longitude = 114.4823,
            acqDate = "2026-09-14",
            acqTime = "0600",
            satellite = "NOAA-21",
            instrument = "VIIRS",
            confidence = "high",
            confidenceRaw = "h",
            frp = 48.5,
            scan = 0.375,
            track = 0.375,
            dayNight = "D",
            version = "2.0NRT",
            source = "NASA FIRMS",
            retrievedAt = System.currentTimeMillis(),
            observationEpochMs = System.currentTimeMillis() - (44 * 60 * 1000L),
            timeWita = "14 Sep 2026 18:21 WITA",
            timeWib = "14 Sep 2026 17:21 WIB",
            dataAgeMinutes = 44,
            dataAgeFormatted = "44 menit",
            dataAgeCategory = DataAgeCategory.TERBARU,
            province = "Kalimantan Tengah",
            regency = "Kab. Kapuas (Mantangai)",
            distanceKm = 8.4,
            bearingDirection = "Barat Laut"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                HotspotListItemCard(hotspot = sample, onClick = {})
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}

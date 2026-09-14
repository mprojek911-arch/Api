package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.DataAgeCategory
import com.example.data.model.DataConnectionStatus
import com.example.data.model.FireHotspot
import com.example.data.model.UserLocation
import com.example.ui.theme.*
import com.example.util.GeoUtils
import org.json.JSONObject
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(
    hotspots: List<FireHotspot>,
    userLocation: UserLocation?,
    connectionStatus: DataConnectionStatus,
    lastSyncTimeFormatted: String,
    onSelectHotspot: (FireHotspot) -> Unit,
    onRecenterUser: () -> Unit,
    onResetKalimantanView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var activeBaseLayer by remember { mutableStateOf("hybrid") } // DEFAULT: SATELLITE / HYBRID

    // Live Center Coordinates display (WGS84 6 desimal)
    var centerLat by remember { mutableDoubleStateOf(GeoUtils.HARDI_MANTANGAI_LAT) }
    var centerLon by remember { mutableDoubleStateOf(GeoUtils.HARDI_MANTANGAI_LON) }

    // Hitung ringkasan umur data
    val terbaruCount = hotspots.count { it.dataAgeCategory == DataAgeCategory.TERBARU }
    val nrtCount = hotspots.count { it.dataAgeCategory == DataAgeCategory.NRT }
    val lebihLamaCount = hotspots.count { it.dataAgeCategory == DataAgeCategory.DATA_LEBIH_LAMA }
    val lamaCount = hotspots.count { it.dataAgeCategory == DataAgeCategory.DATA_LAMA }

    val latestHotspot = hotspots.minByOrNull { it.dataAgeMinutes }

    // Helper: Konversi List<FireHotspot> ke JSON string yang aman dan efisien
    fun serializeHotspotsToJson(list: List<FireHotspot>): String {
        val sb = java.lang.StringBuilder(list.size * 180 + 32)
        sb.append("[")
        var first = true
        for (h in list) {
            // Validasi ketat WGS84
            if (h.latitude.isNaN() || h.longitude.isNaN()) continue
            if (h.latitude < -90.0 || h.latitude > 90.0 || h.longitude < -180.0 || h.longitude > 180.0) continue
            if (kotlin.math.abs(h.latitude) < 0.0001 && kotlin.math.abs(h.longitude) < 0.0001) continue

            if (!first) sb.append(",")
            first = false

            sb.append("{")
            sb.append("\"id\":").append(JSONObject.quote(h.id)).append(",")
            sb.append("\"lat\":").append(String.format(Locale.US, "%.6f", h.latitude)).append(",")
            sb.append("\"lon\":").append(String.format(Locale.US, "%.6f", h.longitude)).append(",")
            sb.append("\"frp\":").append(String.format(Locale.US, "%.1f", h.frp)).append(",")
            sb.append("\"conf\":").append(JSONObject.quote(h.confidence)).append(",")
            sb.append("\"sat\":").append(JSONObject.quote(h.satellite)).append(",")
            sb.append("\"inst\":").append(JSONObject.quote(h.instrument)).append(",")
            sb.append("\"timeWita\":").append(JSONObject.quote(h.timeWita)).append(",")
            sb.append("\"ageMin\":").append(h.dataAgeMinutes).append(",")
            sb.append("\"ageFormatted\":").append(JSONObject.quote(h.dataAgeFormatted)).append(",")
            sb.append("\"ageCategory\":").append(JSONObject.quote(h.dataAgeCategory.name)).append(",")
            sb.append("\"province\":").append(JSONObject.quote(h.province)).append(",")
            sb.append("\"regency\":").append(JSONObject.quote(h.regency))
            sb.append("}")
        }
        sb.append("]")
        return sb.toString()
    }

    // Kirim data hotspot ke Leaflet saat map siap atau saat list berubah
    LaunchedEffect(hotspots, isMapReady) {
        if (isMapReady && webViewInstance != null) {
            val json = serializeHotspotsToJson(hotspots)
            webViewInstance?.evaluateJavascript("loadHotspots($json);", null)
        }
    }

    // Perbarui posisi GPS pengguna ke Leaflet saat GPS berubah
    LaunchedEffect(userLocation, isMapReady) {
        if (isMapReady && webViewInstance != null && userLocation != null) {
            webViewInstance?.evaluateJavascript(
                "setUserLocation(${userLocation.latitude}, ${userLocation.longitude}, ${userLocation.accuracy});",
                null
            )
        }
    }

    // Helper fungsi aksi peta
    fun switchLayer(mode: String) {
        activeBaseLayer = mode
        webViewInstance?.evaluateJavascript("setBaseLayer('$mode');", null)
    }

    fun focusAll() {
        webViewInstance?.evaluateJavascript("fitAllHotspots();", null)
    }

    fun focusNearest() {
        val refLat = userLocation?.latitude ?: GeoUtils.HARDI_MANTANGAI_LAT
        val refLon = userLocation?.longitude ?: GeoUtils.HARDI_MANTANGAI_LON
        webViewInstance?.evaluateJavascript("focusNearestHotspot($refLat, $refLon);", null)
    }

    fun focusMantangai() {
        webViewInstance?.evaluateJavascript("focusMantangai();", null)
    }

    fun focusUserGps() {
        if (userLocation != null) {
            webViewInstance?.evaluateJavascript("focusUserLocation();", null)
        } else {
            onRecenterUser()
        }
    }

    fun copyCoordinates(lat: Double, lon: Double) {
        val coordText = String.format(Locale.US, "%.6f, %.6f", lat, lon)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Koordinat", coordText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Koordinat disalin: $coordText", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("map_screen_container")
    ) {
        // ========================================================
        // 1. WEBVIEW BASEMAP GEOGRAFIS (Leaflet Satelit / Hybrid / Street)
        // ========================================================
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("leaflet_webview_map"),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    setBackgroundColor(android.graphics.Color.parseColor("#0B0F17"))

                    // Javascript Interface Bridge
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onMapReady() {
                            post {
                                isMapReady = true
                                val json = serializeHotspotsToJson(hotspots)
                                evaluateJavascript("loadHotspots($json);", null)
                                userLocation?.let { loc ->
                                    evaluateJavascript("setUserLocation(${loc.latitude}, ${loc.longitude}, ${loc.accuracy});", null)
                                }
                            }
                        }

                        @JavascriptInterface
                        fun onHotspotSelected(id: String) {
                            post {
                                val found = hotspots.find { it.id == id }
                                if (found != null) {
                                    onSelectHotspot(found)
                                }
                            }
                        }

                        @JavascriptInterface
                        fun onMapMoved(lat: Double, lon: Double, zoom: Int) {
                            post {
                                centerLat = lat
                                centerLon = lon
                            }
                        }

                        @JavascriptInterface
                        fun onMapClicked(lat: Double, lon: Double) {
                            post {
                                centerLat = lat
                                centerLon = lon
                            }
                        }

                        @JavascriptInterface
                        fun copyCoordinates(lat: Double, lon: Double) {
                            post {
                                copyCoordinates(lat, lon)
                            }
                        }

                        @JavascriptInterface
                        fun showToast(msg: String) {
                            post {
                                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, "AndroidBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapReady = true
                            val json = serializeHotspotsToJson(hotspots)
                            evaluateJavascript("loadHotspots($json);", null)
                            userLocation?.let { loc ->
                                evaluateJavascript("setUserLocation(${loc.latitude}, ${loc.longitude}, ${loc.accuracy});", null)
                            }
                        }
                    }

                    loadUrl("file:///android_asset/leaflet/map.html")
                    webViewInstance = this
                }
            },
            update = { view ->
                webViewInstance = view
            }
        )

        // ========================================================
        // 2. KONTROL LAYER & AKSI CEPAT DI BAGIAN ATAS
        // ========================================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
        ) {
            // Baris Kontrol Mode Layer (Satelit / Hybrid / Jalan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode HYBRID (Default Utama)
                LayerFilterChip(
                    label = "🗺️ Hybrid",
                    selected = activeBaseLayer == "hybrid",
                    onClick = { switchLayer("hybrid") },
                    tag = "chip_layer_hybrid"
                )

                // Mode SATELLITE Murni
                LayerFilterChip(
                    label = "🛰️ Satelit",
                    selected = activeBaseLayer == "satellite",
                    onClick = { switchLayer("satellite") },
                    tag = "chip_layer_satellite"
                )

                // Mode STREET (Peta Jalan Geografis)
                LayerFilterChip(
                    label = "🛣️ Jalan",
                    selected = activeBaseLayer == "street",
                    onClick = { switchLayer("street") },
                    tag = "chip_layer_street"
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Tombol Fokus Semua Titik Api
                ActionPillButton(
                    icon = Icons.Default.ZoomOutMap,
                    label = "Fokus Semua (${hotspots.size})",
                    onClick = { focusAll() },
                    tag = "btn_focus_all_hotspots"
                )

                // Tombol Fokus Titik Terdekat
                ActionPillButton(
                    icon = Icons.Default.NearMe,
                    label = "Titik Terdekat",
                    onClick = { focusNearest() },
                    tag = "btn_focus_nearest_hotspot"
                )

                // Tombol Pusatkan ke Pos Hardi Mantangai
                ActionPillButton(
                    icon = Icons.Default.LocationCity,
                    label = "Pos Mantangai",
                    onClick = { focusMantangai() },
                    tag = "btn_focus_mantangai"
                )

                // Tombol GPS Lokasi Saya
                ActionPillButton(
                    icon = Icons.Default.MyLocation,
                    label = if (userLocation != null) "Lokasi Saya" else "Aktifkan GPS",
                    onClick = { focusUserGps() },
                    accentColor = if (userLocation != null) GpsBlue else TextSecondary,
                    tag = "btn_my_location_map"
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Banner Telemetri & Status Transparansi Data Satelit
            Surface(
                color = DarkSurface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔥 ${hotspots.size} TITIK API TERDETEKSI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = FireRed
                            )
                            if (latestHotspot != null) {
                                Text(
                                    text = " • Observasi: ${latestHotspot.timeWita}",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = "Data Satelit: NASA FIRMS (VIIRS/MODIS) • Sinkron: $lastSyncTimeFormatted",
                            fontSize = 9.5.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }

                    // Tombol Salin Koordinat Tengah
                    IconButton(
                        onClick = { copyCoordinates(centerLat, centerLon) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Koordinat Tengah",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // ========================================================
        // 3. BAR KOORDINAT TENGAH (LAT & LON PRESISI 6 DESIMAL)
        // ========================================================
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .clickable { copyCoordinates(centerLat, centerLon) }
                .testTag("coord_center_indicator"),
            color = DarkSurface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AgeTerbaru, CircleShape)
                )
                Text(
                    text = String.format(Locale.US, "LAT: %.6f   LON: %.6f", centerLat, centerLon),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // ========================================================
        // 4. LEGENDA UMUR DATA (Tepat di Atas Bottom Bar)
        // ========================================================
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("legend_data_age_bar"),
            color = DarkSurface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AgeLegendItem(color = AgeTerbaru, label = "<1 Jam", count = terbaruCount)
                AgeLegendItem(color = AgeNrt, label = "1–3 Jam", count = nrtCount)
                AgeLegendItem(color = AgeLebihLama, label = "3–6 Jam", count = lebihLamaCount)
                AgeLegendItem(color = AgeLama, label = ">6 Jam", count = lamaCount)
            }
        }
    }
}

@Composable
fun LayerFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) FireRed else DarkSurface.copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) FireRed else DarkBorder
        )
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    accentColor: Color = TextPrimary,
    tag: String
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface.copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

@Composable
fun AgeLegendItem(
    color: Color,
    label: String,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Text(
            text = "$label ($count)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

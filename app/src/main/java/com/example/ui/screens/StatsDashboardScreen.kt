package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DataAgeCategory
import com.example.data.model.DataConnectionStatus
import com.example.data.model.FireHotspot
import com.example.data.model.UserLocation
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun StatsDashboardScreen(
    hotspots: List<FireHotspot>,
    userLocation: UserLocation?,
    connectionStatus: DataConnectionStatus,
    lastSyncTimeFormatted: String,
    statusMessage: String?,
    isOnlyLatestActive: Boolean,
    onToggleOnlyLatest: () -> Unit,
    onOpenAboutData: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = hotspots.size
    val terbaruCount = hotspots.count { it.dataAgeCategory == DataAgeCategory.TERBARU }
    val nrtCount = hotspots.count { it.dataAgeCategory == DataAgeCategory.NRT }
    val lebihLamaCount = hotspots.count { it.dataAgeCategory == DataAgeCategory.DATA_LEBIH_LAMA }
    val lamaCount = hotspots.count { it.dataAgeCategory == DataAgeCategory.DATA_LAMA }

    val avgFrp = if (totalCount > 0) hotspots.map { it.frp }.average() else 0.0
    val maxFrp = hotspots.maxOfOrNull { it.frp } ?: 0.0

    // Hotspot terdekat dari GPS pengguna
    val nearest = hotspots.minByOrNull { it.distanceKm ?: Double.MAX_VALUE }

    // Waktu data terbaru dan tertua
    val newestHotspot = hotspots.minByOrNull { it.dataAgeMinutes }
    val oldestHotspot = hotspots.maxByOrNull { it.dataAgeMinutes }

    // Distribusi provinsi
    val provinceCounts = hotspots.groupBy { it.province }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    // Distribusi satelit
    val satCounts = hotspots.groupBy { it.satellite }
        .mapValues { it.value.size }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp)
            .testTag("stats_dashboard_screen")
    ) {
        // Header Dashboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dashboard Monitoring Titik Api",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
                Text(
                    text = "Sumber data: NASA FIRMS Near Real-Time (NRT)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status Koneksi & Sinkronisasi Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dotColor = when (connectionStatus) {
                            DataConnectionStatus.DATA_TERBARU -> AgeTerbaru
                            DataConnectionStatus.NRT -> AgeNrt
                            DataConnectionStatus.DATA_TERTUNDA -> AgeLebihLama
                            DataConnectionStatus.DATA_TIDAK_TERSEDIA -> AgeLama
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(dotColor, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = connectionStatus.label,
                            color = dotColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = "Sinkron: $lastSyncTimeFormatted",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (!statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusMessage,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                if (newestHotspot != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Data paling baru: ${newestHotspot.dataAgeFormatted} lalu (${newestHotspot.timeWita})",
                        color = AgeTerbaru,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (oldestHotspot != null && oldestHotspot != newestHotspot) {
                    Text(
                        text = "Data paling lama: ${oldestHotspot.dataAgeFormatted} lalu (${oldestHotspot.timeWita})",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tombol Mode "HOTSPOT TERBARU" (Requirement #21)
        Button(
            onClick = onToggleOnlyLatest,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_only_latest_hotspots"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isOnlyLatestActive) FireRedDark else DarkSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            border = if (isOnlyLatestActive) androidx.compose.foundation.BorderStroke(1.5.dp, FireRed) else null
        ) {
            Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                tint = if (isOnlyLatestActive) FireRed else FireOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isOnlyLatestActive) "🔴 MENAMPILKAN HOTSPOT TERBARU (AKTIF)" else "🔴 HOTSPOT TERBARU (≤3 JAM)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isOnlyLatestActive) Color.White else TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4 Kategori Umur Data Grid (Mandat Poin 5 & 11)
        Text("Kategori Umur Data Observasi Satelit", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "≤ 60 Menit",
                subtitle = "TERBARU",
                value = "$terbaruCount",
                icon = Icons.Default.Timer,
                accentColor = AgeTerbaru,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "61–180 Menit",
                subtitle = "NRT",
                value = "$nrtCount",
                icon = Icons.Default.AccessTime,
                accentColor = AgeNrt,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "3–6 Jam",
                subtitle = "LEBIH LAMA",
                value = "$lebihLamaCount",
                icon = Icons.Default.HourglassTop,
                accentColor = AgeLebihLama,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "> 6 Jam",
                subtitle = "DATA LAMA",
                value = "$lamaCount",
                icon = Icons.Default.History,
                accentColor = AgeLama,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Indikator Ringkasan Total & Telemetri
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Total Hotspot",
                subtitle = "Aktual",
                value = "$totalCount",
                icon = Icons.Default.LocalFireDepartment,
                accentColor = FireRed,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "FRP Puncak",
                subtitle = "Intensitas Termal",
                value = "${String.format(Locale.US, "%.1f", maxFrp)} MW",
                icon = Icons.Default.Bolt,
                accentColor = FireYellow,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hotspot Terdekat & Info GPS (Requirement #14)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📍 Deteksi Sekitar Posisi Anda",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (userLocation != null) "GPS Aktif" else "GPS Belum Aktif",
                        color = if (userLocation != null) GpsBlue else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (nearest != null && nearest.distanceKm != null) {
                    val d = nearest.distanceKm
                    Text(
                        text = "Hotspot Terdekat: ${String.format(Locale.US, "%.2f", d)} km (Arah ${nearest.bearingDirection ?: "Utara"})",
                        fontWeight = FontWeight.Bold,
                        color = GpsBlue,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${nearest.province} • Observasi: ${nearest.timeWita} (${nearest.dataAgeFormatted})",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Radius Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val in5km = hotspots.count { (it.distanceKm ?: Double.MAX_VALUE) <= 5.0 }
                        val in10km = hotspots.count { (it.distanceKm ?: Double.MAX_VALUE) <= 10.0 }
                        val in25km = hotspots.count { (it.distanceKm ?: Double.MAX_VALUE) <= 25.0 }
                        val in50km = hotspots.count { (it.distanceKm ?: Double.MAX_VALUE) <= 50.0 }

                        RadiusBadge("≤ 5 km", in5km)
                        RadiusBadge("≤ 10 km", in10km)
                        RadiusBadge("≤ 25 km", in25km)
                        RadiusBadge("≤ 50 km", in50km)
                    }
                } else {
                    Text(
                        text = "Aktifkan izin lokasi GPS untuk mengukur jarak ke titik api terdekat secara otomatis.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Breakdown Provinsi
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Sebaran per Provinsi Kalimantan",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (provinceCounts.isEmpty()) {
                    Text("Belum ada data pada filter saat ini", color = TextMuted, fontSize = 11.sp)
                } else {
                    provinceCounts.forEach { (prov, count) ->
                        val pct = if (totalCount > 0) count.toFloat() / totalCount else 0f
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(prov, color = TextSecondary, fontSize = 12.sp)
                                Text("$count titik (${(pct * 100).toInt()}%)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp),
                                color = FireRed,
                                trackColor = DarkSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Breakdown Satelit
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Sensor Satelit yang Aktif Merekam",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                satCounts.forEach { (sat, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(sat, color = TextSecondary, fontSize = 12.sp)
                        Text("$count deteksi", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Tombol Transparansi Data
        OutlinedButton(
            onClick = onOpenAboutData,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Penjelasan Sumber Data & Keterbatasan Satelit", fontSize = 11.sp)
        }
    }
}

@Composable
fun RadiusBadge(label: String, count: Int) {
    Surface(
        color = if (count > 0) FireRedDark.copy(alpha = 0.3f) else DarkSurfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = if (count > 0) androidx.compose.foundation.BorderStroke(1.dp, FireRed.copy(alpha = 0.5f)) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = TextSecondary, fontSize = 10.sp)
            Text(
                "$count",
                color = if (count > 0) FireOrange else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    subtitle: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = TextSecondary, fontSize = 10.sp)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

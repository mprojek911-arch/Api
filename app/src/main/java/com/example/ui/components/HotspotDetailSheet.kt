package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DataAgeCategory
import com.example.data.model.FireHotspot
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotDetailSheet(
    hotspot: FireHotspot,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val ageColor = when (hotspot.dataAgeCategory) {
        DataAgeCategory.TERBARU -> AgeTerbaru
        DataAgeCategory.NRT -> AgeNrt
        DataAgeCategory.DATA_LEBIH_LAMA -> AgeLebihLama
        DataAgeCategory.DATA_LAMA -> AgeLama
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary,
        modifier = Modifier.testTag("hotspot_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Judul & Badge Umur Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔥 HOTSPOT TERDETEKSI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = FireRed
                        )
                    }
                    Text(
                        text = "${hotspot.province} • ${hotspot.regency}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    color = ageColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ageColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = hotspot.dataAgeCategory.label,
                        color = ageColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card Telemetry Observasi Satelit
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    DetailRow("Status Telemetri", when {
                        hotspot.dataAgeMinutes <= 60 -> "BARU (< 1 jam)"
                        hotspot.dataAgeMinutes <= 180 -> "TERKINI (1–3 jam)"
                        else -> "TERTUNDA (> 3 jam)"
                    })
                    DetailRow("Satelit", hotspot.satellite)
                    DetailRow("Sensor", "${hotspot.instrument} (${if (hotspot.instrument == "VIIRS") "375 m" else "1 km"})")
                    DetailRow("Waktu Observasi (WITA)", hotspot.timeWita)
                    DetailRow("Waktu Observasi (WIB)", hotspot.timeWib)
                    DetailRow("Waktu Observasi (UTC)", "${hotspot.acqDate} ${hotspot.acqTime} UTC")
                    DetailRow("Umur Data", "${hotspot.dataAgeFormatted} (${hotspot.dataAgeMinutes} menit)")
                    DetailRow("Tingkat Keyakinan", "${hotspot.confidence.uppercase()} (${hotspot.confidenceRaw})")
                    DetailRow("Intensitas FRP", "${String.format(Locale.US, "%.1f", hotspot.frp)} MW")
                    DetailRow("Latitude (Lintang)", String.format(Locale.US, "%.6f", hotspot.latitude))
                    DetailRow("Longitude (Bujur)", String.format(Locale.US, "%.6f", hotspot.longitude))
                    DetailRow("Resolusi Scan & Track", "${hotspot.scan} km × ${hotspot.track} km")
                    DetailRow("Siklus Deteksi", if (hotspot.dayNight == "D") "Siang Hari" else "Malam Hari")
                    DetailRow("Sumber Data", hotspot.source)

                    hotspot.distanceKm?.let { dist ->
                        DetailRow(
                            "Jarak dari Posisi Anda",
                            "${String.format(Locale.US, "%.2f", dist)} km (Arah ${hotspot.bearingDirection ?: "Utara"})"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Catatan Transparansi Keterbatasan Satelit
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkBackground,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data titik panas satelit adalah indikasi anomali termal pada saat satelit melintas (bukan pantauan kamera langsung detik ini). Perlu verifikasi darat di lapangan.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tombol Salin Koordinat Format (LATITUDE, LONGITUDE)
            Button(
                onClick = {
                    val coordStr = String.format(Locale.US, "%.6f, %.6f", hotspot.latitude, hotspot.longitude)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Koordinat Hotspot", coordStr)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Koordinat disalin: $coordStr", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_copy_coordinates_sheet"),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Salin Koordinat (${String.format(Locale.US, "%.6f, %.6f", hotspot.latitude, hotspot.longitude)})",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tombol Navigasi Lapangan (Google Maps Intent)
            Button(
                onClick = {
                    val geoUri = Uri.parse("geo:${hotspot.latitude},${hotspot.longitude}?q=${hotspot.latitude},${hotspot.longitude}(Hotspot+${hotspot.satellite})")
                    val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=${hotspot.latitude},${hotspot.longitude}")
                        )
                        context.startActivity(browserIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_navigate_google_maps"),
                colors = ButtonDefaults.buttonColors(containerColor = FireRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Navigasi Lapangan (Google Maps)", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tombol Bagikan Info Hotspot
            OutlinedButton(
                onClick = {
                    val shareText = """
                        🔥 LAPORAN TITIK PANAS (HOTSPOT)
                        • Satelit: ${hotspot.satellite} (${hotspot.instrument})
                        • Wilayah: ${hotspot.province}, ${hotspot.regency}
                        • Koordinat: ${String.format(Locale.US, "%.6f", hotspot.latitude)}, ${String.format(Locale.US, "%.6f", hotspot.longitude)}
                        • Waktu Observasi: ${hotspot.timeWita} / ${hotspot.timeWib}
                        • Umur Data: ${hotspot.dataAgeFormatted} (${hotspot.dataAgeCategory.label})
                        • FRP: ${String.format(Locale.US, "%.1f", hotspot.frp)} MW
                        • Keyakinan: ${hotspot.confidence.uppercase()}
                        • Peta Google: https://www.google.com/maps/search/?api=1&query=${hotspot.latitude},${hotspot.longitude}
                        
                        Sumber: ${hotspot.source} — HARDI MANTANGAI FIRE MONITOR
                    """.trimIndent()

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan Informasi Hotspot"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bagikan Telemetri Hotspot")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text("Tutup")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}

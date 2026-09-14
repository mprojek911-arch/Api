package com.example.ui.components

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDataSheet(
    onDismiss: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary,
        modifier = Modifier.testTag("about_data_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = FireRed, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sumber Data & Transparansi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Warning Banner: Keterbatasan Satelit
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FireRed.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚠️ KETERBATASAN OBSERVASI SATELIT",
                        fontWeight = FontWeight.Bold,
                        color = FireRed,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tidak adanya titik panas satelit pada peta TIDAK SELALU BERARTI tidak ada kebakaran di lapangan. Satelit hanya mendeteksi radiasi termal saat melintas pada lintasan orbitnya. Kebakaran di bawah kanopi lebat, terhalang awan tebal, atau terjadi di antara jadwal lintasan satelit dapat tidak terdeteksi.",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Sumber Data NASA FIRMS
            Text("1. Sumber Data: NASA FIRMS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Data aktif kebakaran (Active Fire / Hotspot) diperoleh dari program NASA FIRMS (Fire Information for Resource Management System) secara Near Real-Time (NRT).",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Satelit & Sensor
            Text("2. Instrumen Sensor yang Digunakan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "• VIIRS 375m (NOAA-20, NOAA-21, Suomi-NPP): Memiliki resolusi spasial tinggi 375 meter (I-band). Sangat efektif mendeteksi anomali panas berukuran lebih kecil.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "• MODIS 1km (Terra & Aqua): Instrumen historis global dengan resolusi piksel 1 kilometer.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Waktu Observasi vs Waktu Diterima
            Text("3. Transparansi Waktu & Umur Data", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Aplikasi ini membedakan secara tegas antara:\n• Waktu Observasi Satelit: Waktu lintasan satelit merekam anomali termal di bumi (UTC dikonversi ke WITA / WIB).\n• Waktu Diterima: Waktu aplikasi mengunduh data dari server NASA.\n• Umur Data: Selisih waktu observasi satelit hingga saat ini.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Kategori Umur Data
            Text("4. Klasifikasi Umur Data", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🟢 0–60 menit:", fontWeight = FontWeight.Bold, color = AgeTerbaru, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TERBARU (Observasi sangat baru)", color = TextPrimary, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🟡 61–180 menit:", fontWeight = FontWeight.Bold, color = AgeNrt, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("NRT (Near Real-Time)", color = TextPrimary, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🟠 181–360 menit:", fontWeight = FontWeight.Bold, color = AgeLebihLama, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DATA LEBIH LAMA (3–6 jam)", color = TextPrimary, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔴 >360 menit:", fontWeight = FontWeight.Bold, color = AgeLama, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DATA LAMA (>6 jam)", color = TextPrimary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Arti FRP (Fire Radiative Power)
            Text("5. Fire Radiative Power (FRP)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "FRP diukur dalam Megawatts (MW), menggambarkan laju radiasi termal yang dilepaskan oleh api saat observasi. Semakin tinggi FRP, semakin besar intensitas panas yang terdeteksi.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FireRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Saya Mengerti", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

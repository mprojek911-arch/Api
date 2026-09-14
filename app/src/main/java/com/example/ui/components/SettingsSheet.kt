package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    currentMapKey: String,
    currentIntervalMinutes: Int,
    isNotifEnabled: Boolean,
    notifRadiusKm: Int,
    cachedCount: Int,
    onSaveSettings: (mapKey: String, intervalMinutes: Int, notifEnabled: Boolean, notifRadius: Int) -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit
) {
    var mapKeyInput by remember { mutableStateOf(currentMapKey) }
    var selectedInterval by remember { mutableIntStateOf(currentIntervalMinutes) }
    var notifActive by remember { mutableStateOf(isNotifEnabled) }
    var selectedRadius by remember { mutableIntStateOf(notifRadiusKm) }

    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary,
        modifier = Modifier.testTag("settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = FireRed, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pengaturan & Konfigurasi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. NASA FIRMS MAP_KEY
            Text("NASA FIRMS MAP_KEY (Opsional)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Bila kosong, aplikasi secara otomatis menggunakan feed satelit NRT terbuka resmi NASA untuk Kalimantan.",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = mapKeyInput,
                onValueChange = { mapKeyInput = it },
                placeholder = { Text("Contoh: 32 karakter map key", color = TextMuted) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_nasa_map_key"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = FireRed,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Interval Auto-Refresh
            Text("Interval Sinkronisasi Otomatis (Auto-Refresh)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Standar resmi: 15 menit untuk menghemat kuota dan mematuhi batas request satelit.",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0 to "Off", 5 to "5 mnt", 15 to "15 mnt", 30 to "30 mnt", 60 to "60 mnt").forEach { (min, label) ->
                    FilterChip(
                        selected = selectedInterval == min,
                        onClick = { selectedInterval = min },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FireRedDark,
                            selectedLabelColor = TextPrimary,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Notifikasi Titik Api Terdekat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Peringatan Hotspot Baru", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text("Bunyikan notifikasi bila ada hotspot baru terdeteksi di sekitar pos/pengguna", fontSize = 11.sp, color = TextSecondary)
                }
                Switch(
                    checked = notifActive,
                    onCheckedChange = { notifActive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FireRed,
                        checkedTrackColor = FireRedDark
                    )
                )
            }

            if (notifActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Radius Notifikasi Siaga", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5 to "5 km", 10 to "10 km", 25 to "25 km", 50 to "50 km").forEach { (rad, label) ->
                        FilterChip(
                            selected = selectedRadius == rad,
                            onClick = { selectedRadius = rad },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GpsBlue.copy(alpha = 0.4f),
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Cache Management
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Penyimpanan Cache Lokal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                        Text("$cachedCount titik hotspot tersimpan offline", fontSize = 11.sp, color = TextSecondary)
                    }
                    OutlinedButton(
                        onClick = onClearCache,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FireRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Kosongkan Cache", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    onSaveSettings(mapKeyInput, selectedInterval, notifActive, selectedRadius)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_save_settings"),
                colors = ButtonDefaults.buttonColors(containerColor = FireRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan Konfigurasi", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

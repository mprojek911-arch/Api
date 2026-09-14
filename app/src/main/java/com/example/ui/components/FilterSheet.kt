package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FilterState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    filterState: FilterState,
    onFilterChange: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary,
        modifier = Modifier.testTag("filter_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Filter Observasi Titik Panas",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )
            Text(
                text = "Filter diterapkan berdasarkan waktu observasi satelit aktual (UTC/WITA)",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Rentang Waktu Observasi Satelit
            Text(
                "Rentang Waktu Observasi Satelit",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val timeRanges = listOf(
                "1h" to "1 Jam Terakhir",
                "3h" to "3 Jam Terakhir",
                "6h" to "6 Jam Terakhir",
                "12h" to "12 Jam Terakhir",
                "24h" to "24 Jam Terakhir",
                "48h" to "48 Jam Terakhir",
                "all" to "Semua Data"
            )
            timeRanges.chunked(2).forEach { rowPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPair.forEach { (key, label) ->
                        FilterChip(
                            selected = filterState.timeRange == key,
                            onClick = { onFilterChange(filterState.copy(timeRange = key)) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FireRedDark,
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Radius dari Lokasi GPS Pengguna
            Text(
                "Radius Deteksi dari Lokasi Anda (GPS)",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0 to "Semua Jarak", 5 to "5 km", 10 to "10 km", 25 to "25 km", 50 to "50 km").forEach { (rad, label) ->
                    FilterChip(
                        selected = filterState.nearbyRadiusKm == rad,
                        onClick = { onFilterChange(filterState.copy(nearbyRadiusKm = rad)) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GpsBlue.copy(alpha = 0.4f),
                            selectedLabelColor = TextPrimary,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Tingkat Keyakinan (Confidence)
            Text("Tingkat Keyakinan (Confidence)", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "Semua", "high" to "Tinggi", "nominal" to "Sedang", "low" to "Rendah").forEach { (key, label) ->
                    FilterChip(
                        selected = filterState.confidence == key,
                        onClick = { onFilterChange(filterState.copy(confidence = key)) },
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

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Minimum FRP Slider
            Text(
                text = "Radiasi Panas Minimum: ${filterState.minFrp.toInt()} MW",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = filterState.minFrp.toFloat(),
                onValueChange = { onFilterChange(filterState.copy(minFrp = it.toDouble())) },
                valueRange = 0f..100f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = FireRed,
                    activeTrackColor = FireRed,
                    inactiveTrackColor = DarkSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Satelit / Instrumen
            Text("Satelit & Instrumen Sensor", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            val satellites = listOf(
                "all" to "Semua Satelit",
                "VIIRS" to "VIIRS (375m)",
                "MODIS" to "MODIS (1km)",
                "NOAA-21" to "NOAA-21",
                "NOAA-20" to "NOAA-20",
                "Suomi-NPP" to "Suomi-NPP"
            )
            satellites.chunked(3).forEach { rowList ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowList.forEach { (satKey, satLabel) ->
                        FilterChip(
                            selected = filterState.satellite == satKey,
                            onClick = { onFilterChange(filterState.copy(satellite = satKey)) },
                            label = { Text(satLabel, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FireRedDark,
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Provinsi Kalimantan
            Text("Wilayah Provinsi Kalimantan", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            val provinces = listOf(
                "all" to "Semua Wilayah",
                "Kalimantan Tengah" to "Kalteng (Mantangai)",
                "Kalimantan Barat" to "Kalbar",
                "Kalimantan Selatan" to "Kalsel",
                "Kalimantan Timur" to "Kaltim",
                "Kalimantan Utara" to "Kaltara"
            )
            provinces.chunked(2).forEach { rowPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPair.forEach { (provKey, provLabel) ->
                        FilterChip(
                            selected = filterState.province == provKey,
                            onClick = { onFilterChange(filterState.copy(province = provKey)) },
                            label = { Text(provLabel, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FireRedDark,
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_apply_filters"),
                colors = ButtonDefaults.buttonColors(containerColor = FireRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Terapkan Filter", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    onFilterChange(FilterState())
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text("Reset Semua Filter ke Standar")
            }
        }
    }
}

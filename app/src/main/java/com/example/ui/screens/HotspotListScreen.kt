package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.DataAgeCategory
import com.example.data.model.FireHotspot
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotListScreen(
    hotspots: List<FireHotspot>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectHotspot: (FireHotspot) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortBy by remember { mutableStateOf("frp") } // frp, time, distance, age

    val sortedList = remember(hotspots, sortBy) {
        when (sortBy) {
            "distance" -> hotspots.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            "time" -> hotspots.sortedByDescending { it.observationEpochMs }
            "age" -> hotspots.sortedBy { it.dataAgeMinutes }
            else -> hotspots.sortedByDescending { it.frp }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .testTag("hotspot_list_screen")
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari wilayah, kabupaten, satelit, koordinat...", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari", tint = TextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_hotspots"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = FireRed,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Sort Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Urutan:", color = TextSecondary, fontSize = 11.sp)
            FilterChip(
                selected = sortBy == "frp",
                onClick = { sortBy = "frp" },
                label = { Text("FRP Terbesar", fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FireRedDark,
                    selectedLabelColor = TextPrimary,
                    containerColor = DarkSurface,
                    labelColor = TextSecondary
                )
            )
            FilterChip(
                selected = sortBy == "time",
                onClick = { sortBy = "time" },
                label = { Text("Observasi Baru", fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FireRedDark,
                    selectedLabelColor = TextPrimary,
                    containerColor = DarkSurface,
                    labelColor = TextSecondary
                )
            )
            FilterChip(
                selected = sortBy == "distance",
                onClick = { sortBy = "distance" },
                label = { Text("Terdekat", fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FireRedDark,
                    selectedLabelColor = TextPrimary,
                    containerColor = DarkSurface,
                    labelColor = TextSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Menampilkan ${sortedList.size} titik panas satelit aktual",
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        if (sortedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = RiskLow,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tidak ada titik panas yang cocok",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Coba sesuaikan filter waktu observasi atau kata kunci",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("hotspot_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(sortedList, key = { it.id }) { hotspot ->
                    HotspotListItemCard(
                        hotspot = hotspot,
                        onClick = { onSelectHotspot(hotspot) }
                    )
                }
            }
        }
    }
}

@Composable
fun HotspotListItemCard(
    hotspot: FireHotspot,
    onClick: () -> Unit
) {
    val ageColor = when (hotspot.dataAgeCategory) {
        DataAgeCategory.TERBARU -> AgeTerbaru
        DataAgeCategory.NRT -> AgeNrt
        DataAgeCategory.DATA_LEBIH_LAMA -> AgeLebihLama
        DataAgeCategory.DATA_LAMA -> AgeLama
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("card_hotspot_${hotspot.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            // Header Baris 1: FRP, Wilayah, dan Badge Umur Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = FireRedDark.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", hotspot.frp)} MW",
                            color = FireOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${hotspot.province} • ${hotspot.regency}",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    color = ageColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ageColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = hotspot.dataAgeCategory.label,
                        color = ageColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Baris 2: Waktu Observasi Satelit & Umur Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Observasi: ${hotspot.timeWita}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Umur Data: ${hotspot.dataAgeFormatted} (${hotspot.satellite} ${hotspot.instrument})",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                hotspot.distanceKm?.let { dist ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", dist)} km",
                            color = GpsBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        hotspot.bearingDirection?.let { dir ->
                            Text(
                                text = "Arah $dir",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Baris 3: Koordinat & Keyakinan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Koordinat: ${String.format(Locale.US, "%.5f", hotspot.latitude)}, ${String.format(Locale.US, "%.5f", hotspot.longitude)}",
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = "Keyakinan: ${hotspot.confidence.uppercase()}",
                    color = when (hotspot.confidence) {
                        "high" -> RiskHigh
                        "nominal" -> RiskMedium
                        else -> FireYellow
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

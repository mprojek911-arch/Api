package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.core.content.ContextCompat
import com.example.data.model.DataConnectionStatus
import com.example.ui.components.AboutDataSheet
import com.example.ui.components.FilterSheet
import com.example.ui.components.HotspotDetailSheet
import com.example.ui.components.SettingsSheet
import com.example.ui.screens.HotspotListScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.StatsDashboardScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.FireMonitorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FireMonitorViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()

                // Launcher untuk Izin Lokasi
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (fineGranted || coarseGranted) {
                        viewModel.startGpsUpdates()
                    } else {
                        // Fallback ke posisi Pos Hardi Mantangai jika izin ditolak
                        viewModel.setMantangaiLocationFallback()
                    }
                }

                // Launcher untuk Izin Notifikasi Android 13+
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    // Minta izin notifikasi jika Android 13+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Otomatis cek izin lokasi saat buka aplikasi
                    val hasFine = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasFine) {
                        viewModel.startGpsUpdates()
                    }
                }

                fun toggleOrRequestGps() {
                    val hasFine = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasFine) {
                        if (uiState.isFieldGpsActive) {
                            viewModel.stopGpsUpdates()
                        } else {
                            viewModel.startGpsUpdates()
                        }
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                val hasActiveFilter = uiState.filterState.province != "all" ||
                        uiState.filterState.confidence != "all" ||
                        uiState.filterState.minFrp > 0 ||
                        uiState.filterState.timeRange != "24h" ||
                        uiState.filterState.satellite != "all" ||
                        uiState.filterState.nearbyRadiusKm > 0 ||
                        uiState.filterState.onlyLatestHotspots

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "HARDI MANTANGAI",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val statusColor = when (uiState.connectionStatus) {
                                            DataConnectionStatus.DATA_TERBARU -> AgeTerbaru
                                            DataConnectionStatus.NRT -> AgeNrt
                                            DataConnectionStatus.DATA_TERTUNDA -> AgeLebihLama
                                            DataConnectionStatus.DATA_TIDAK_TERSEDIA -> AgeLama
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(statusColor, androidx.compose.foundation.shape.CircleShape)
                                        )
                                    }
                                    Text(
                                        text = "Kalimantan Hotspot Monitoring • ${uiState.lastSyncTimeFormatted}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            },
                            actions = {
                                // Tombol GPS Posisi Lapangan
                                IconButton(
                                    onClick = { toggleOrRequestGps() },
                                    modifier = Modifier.testTag("btn_top_field_gps")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "GPS Lokasi Anda",
                                        tint = if (uiState.isFieldGpsActive) GpsBlue else TextSecondary
                                    )
                                }
                                // Tombol Filter Parameter
                                IconButton(
                                    onClick = { viewModel.setShowFilter(true) },
                                    modifier = Modifier.testTag("btn_top_filter")
                                ) {
                                    BadgedBox(badge = {
                                        if (hasActiveFilter) {
                                            Badge(containerColor = FireRed) { Text("!") }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = "Filter",
                                            tint = if (hasActiveFilter) FireOrange else TextPrimary
                                        )
                                    }
                                }
                                // Tombol Refresh Data Satelit
                                IconButton(
                                    onClick = { viewModel.refreshData() },
                                    modifier = Modifier.testTag("btn_top_refresh")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = TextPrimary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = DarkSurface,
                                titleContentColor = TextPrimary
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            contentColor = TextPrimary,
                            modifier = Modifier.testTag("bottom_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = uiState.activeTab == 0,
                                onClick = { viewModel.setActiveTab(0) },
                                icon = { Icon(Icons.Default.Map, contentDescription = "Peta") },
                                label = { Text("Peta", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_tab_map"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FireRed,
                                    selectedTextColor = FireRed,
                                    indicatorColor = FireRedDark.copy(alpha = 0.35f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                            NavigationBarItem(
                                selected = uiState.activeTab == 1,
                                onClick = { viewModel.setActiveTab(1) },
                                icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Daftar") },
                                label = { Text("Daftar (${uiState.filteredHotspots.size})", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_tab_list"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FireRed,
                                    selectedTextColor = FireRed,
                                    indicatorColor = FireRedDark.copy(alpha = 0.35f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                            NavigationBarItem(
                                selected = uiState.activeTab == 2,
                                onClick = { viewModel.setActiveTab(2) },
                                icon = { Icon(Icons.Default.Analytics, contentDescription = "Dashboard") },
                                label = { Text("Dashboard", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_tab_stats"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FireRed,
                                    selectedTextColor = FireRed,
                                    indicatorColor = FireRedDark.copy(alpha = 0.35f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (uiState.activeTab) {
                            0 -> MapScreen(
                                hotspots = uiState.filteredHotspots,
                                userLocation = uiState.userLocation,
                                connectionStatus = uiState.connectionStatus,
                                lastSyncTimeFormatted = uiState.lastSyncTimeFormatted,
                                onSelectHotspot = { viewModel.selectHotspot(it) },
                                onRecenterUser = { toggleOrRequestGps() },
                                onResetKalimantanView = { /* Reset zoom handled in canvas */ }
                            )
                            1 -> HotspotListScreen(
                                hotspots = uiState.filteredHotspots,
                                searchQuery = uiState.filterState.searchQuery,
                                onSearchChange = { q -> viewModel.updateFilter { it.copy(searchQuery = q) } },
                                onSelectHotspot = { viewModel.selectHotspot(it) }
                            )
                            2 -> StatsDashboardScreen(
                                hotspots = uiState.filteredHotspots,
                                userLocation = uiState.userLocation,
                                connectionStatus = uiState.connectionStatus,
                                lastSyncTimeFormatted = uiState.lastSyncTimeFormatted,
                                statusMessage = uiState.statusMessage,
                                isOnlyLatestActive = uiState.filterState.onlyLatestHotspots,
                                onToggleOnlyLatest = { viewModel.toggleOnlyLatest() },
                                onOpenAboutData = { viewModel.setShowAbout(true) },
                                onOpenSettings = { viewModel.setShowSettings(true) }
                            )
                        }

                        // Indikator Loading Progress Bar di bawah TopBar
                        if (uiState.isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = FireRed
                            )
                        }
                    }

                    // 1. Hotspot Detail Bottom Sheet
                    uiState.selectedHotspot?.let { hotspot ->
                        HotspotDetailSheet(
                            hotspot = hotspot,
                            onDismiss = { viewModel.selectHotspot(null) }
                        )
                    }

                    // 2. Filter Bottom Sheet
                    if (uiState.showFilterSheet) {
                        FilterSheet(
                            filterState = uiState.filterState,
                            onFilterChange = { newFilter -> viewModel.updateFilter { newFilter } },
                            onDismiss = { viewModel.setShowFilter(false) }
                        )
                    }

                    // 3. Settings Bottom Sheet
                    if (uiState.showSettingsSheet) {
                        SettingsSheet(
                            currentMapKey = viewModel.preferences.nasaFirmsMapKey,
                            currentIntervalMinutes = viewModel.preferences.autoRefreshIntervalMinutes,
                            isNotifEnabled = viewModel.preferences.isNotificationsEnabled,
                            notifRadiusKm = viewModel.preferences.notificationRadiusKm,
                            cachedCount = uiState.cachedCount,
                            onSaveSettings = { key, interval, notif, rad ->
                                viewModel.saveSettings(key, interval, notif, rad)
                            },
                            onClearCache = { viewModel.clearLocalCache() },
                            onDismiss = { viewModel.setShowSettings(false) }
                        )
                    }

                    // 4. About Data Bottom Sheet
                    if (uiState.showAboutDataSheet) {
                        AboutDataSheet(
                            onDismiss = { viewModel.setShowAbout(false) }
                        )
                    }
                }
            }
        }
    }
}

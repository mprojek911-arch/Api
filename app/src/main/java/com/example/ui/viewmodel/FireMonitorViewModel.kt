package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferences
import com.example.data.local.FireDatabase
import com.example.data.model.DataConnectionStatus
import com.example.data.model.FilterState
import com.example.data.model.FireHotspot
import com.example.data.model.UserLocation
import com.example.data.repository.HotspotRepository
import com.example.util.GeoUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UiState(
    val hotspots: List<FireHotspot> = emptyList(),
    val filteredHotspots: List<FireHotspot> = emptyList(),
    val filterState: FilterState = FilterState(),
    val userLocation: UserLocation? = null,
    val isFieldGpsActive: Boolean = false,
    val selectedHotspot: FireHotspot? = null,
    val activeTab: Int = 0, // 0: Peta, 1: Daftar, 2: Dashboard
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val connectionStatus: DataConnectionStatus = DataConnectionStatus.DATA_TERBARU,
    val lastSyncTimeFormatted: String = "Belum sinkron",
    val showFilterSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showAboutDataSheet: Boolean = false,
    val cachedCount: Int = 0
)

class FireMonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FireDatabase.getInstance(application)
    val preferences = AppPreferences(application)
    private val repository = HotspotRepository(database, preferences = preferences)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    init {
        NotificationHelper.createNotificationChannel(application)

        // Muat data dari database lokal (Room Flow)
        viewModelScope.launch {
            val lastSync = preferences.lastSuccessfulSyncEpochMs
            val initialSyncStr = if (lastSync > 0) GeoUtils.formatSyncTime(lastSync) else "Belum sinkron"
            _uiState.update { it.copy(lastSyncTimeFormatted = initialSyncStr) }

            repository.getHotspotsFlow(_uiState.value.userLocation).collect { rawList ->
                _uiState.update { current ->
                    val filtered = repository.applyFilters(rawList, current.filterState)
                    current.copy(
                        hotspots = rawList,
                        filteredHotspots = filtered,
                        cachedCount = rawList.size
                    )
                }
            }
        }

        // Lakukan penyegaran data satelit pertama kali
        refreshData()

        // Mulai jadwal auto-refresh
        startAutoRefreshLoop()
    }

    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectHotspot(hotspot: FireHotspot?) {
        _uiState.update { it.copy(selectedHotspot = hotspot) }
    }

    fun setShowFilter(show: Boolean) {
        _uiState.update { it.copy(showFilterSheet = show) }
    }

    fun setShowSettings(show: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = show) }
    }

    fun setShowAbout(show: Boolean) {
        _uiState.update { it.copy(showAboutDataSheet = show) }
    }

    fun toggleOnlyLatest() {
        _uiState.update { current ->
            val nextLatest = !current.filterState.onlyLatestHotspots
            val nextFilter = current.filterState.copy(onlyLatestHotspots = nextLatest)
            val filtered = repository.applyFilters(current.hotspots, nextFilter)
            current.copy(filterState = nextFilter, filteredHotspots = filtered)
        }
    }

    fun updateFilter(update: (FilterState) -> FilterState) {
        _uiState.update { current ->
            val nextFilter = update(current.filterState)
            val filtered = repository.applyFilters(current.hotspots, nextFilter)
            current.copy(
                filterState = nextFilter,
                filteredHotspots = filtered
            )
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Mengunduh observasi satelit NASA FIRMS...") }
            val result = repository.refreshHotspots(userLocation = _uiState.value.userLocation)

            val now = System.currentTimeMillis()
            val syncTimeStr = GeoUtils.formatSyncTime(now)

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    connectionStatus = result.connectionStatus,
                    statusMessage = result.message,
                    lastSyncTimeFormatted = if (result.isSuccess) syncTimeStr else current.lastSyncTimeFormatted
                )
            }

            // Notifikasi hotspot baru di sekitar pengguna jika diaktifkan
            if (result.isSuccess && preferences.isNotificationsEnabled) {
                val notifRadius = preferences.notificationRadiusKm
                for (newHotspot in result.newlyDetectedForNotification) {
                    val dist = newHotspot.distanceKm
                    if (dist == null || dist <= notifRadius) {
                        NotificationHelper.sendHotspotAlertNotification(getApplication(), newHotspot)
                    }
                }
            }
        }
    }

    private fun startAutoRefreshLoop() {
        autoRefreshJob?.cancel()
        val intervalMinutes = preferences.autoRefreshIntervalMinutes
        if (intervalMinutes <= 0) return // Dinonaktifkan

        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                val intervalMs = preferences.autoRefreshIntervalMinutes * 60 * 1000L
                if (intervalMs <= 0) break
                delay(intervalMs)
                Log.d("FireMonitorVM", "Auto-refreshing satellite telemetry...")
                refreshData()
            }
        }
    }

    fun saveSettings(mapKey: String, intervalMinutes: Int, notifEnabled: Boolean, notifRadius: Int) {
        preferences.nasaFirmsMapKey = mapKey
        preferences.autoRefreshIntervalMinutes = intervalMinutes
        preferences.isNotificationsEnabled = notifEnabled
        preferences.notificationRadiusKm = notifRadius

        startAutoRefreshLoop()
        refreshData()
    }

    fun clearLocalCache() {
        viewModelScope.launch {
            database.hotspotDao().clearAll()
            _uiState.update {
                it.copy(
                    hotspots = emptyList(),
                    filteredHotspots = emptyList(),
                    cachedCount = 0,
                    statusMessage = "Cache lokal berhasil dibersihkan"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startGpsUpdates() {
        val context = getApplication<Application>()
        if (locationManager == null) {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        }

        val lm = locationManager ?: return

        try {
            // Ambil lokasi terakhir yang diketahui segera
            val lastGps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = when {
                lastGps != null && lastNet != null -> if (lastGps.time > lastNet.time) lastGps else lastNet
                lastGps != null -> lastGps
                else -> lastNet
            }

            if (best != null) {
                updateLocationFromAndroid(best)
            }

            if (locationListener == null) {
                locationListener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        updateLocationFromAndroid(loc)
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
            }

            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, locationListener!!)
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, locationListener!!)
            }

            _uiState.update { it.copy(isFieldGpsActive = true) }
        } catch (e: SecurityException) {
            Log.w("FireMonitorVM", "Izin lokasi belum diberikan: ${e.message}")
        }
    }

    fun stopGpsUpdates() {
        locationListener?.let {
            locationManager?.removeUpdates(it)
        }
        _uiState.update { it.copy(isFieldGpsActive = false) }
    }

    private fun updateLocationFromAndroid(loc: Location) {
        val userLoc = UserLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracy = loc.accuracy,
            timestamp = loc.time
        )
        _uiState.update { current ->
            val updated = current.hotspots.map { h ->
                val dist = GeoUtils.calculateHaversineDistanceKm(loc.latitude, loc.longitude, h.latitude, h.longitude)
                val bearing = GeoUtils.calculateBearing(loc.latitude, loc.longitude, h.latitude, h.longitude).direction
                h.copy(distanceKm = dist, bearingDirection = bearing)
            }
            val filtered = repository.applyFilters(updated, current.filterState)
            current.copy(
                userLocation = userLoc,
                isFieldGpsActive = true,
                hotspots = updated,
                filteredHotspots = filtered
            )
        }
    }

    fun setMantangaiLocationFallback() {
        // Digunakan jika GPS perangkat nonaktif untuk memusatkan ke pos pantau Hardi Mantangai
        val loc = UserLocation(
            latitude = GeoUtils.HARDI_MANTANGAI_LAT,
            longitude = GeoUtils.HARDI_MANTANGAI_LON,
            accuracy = 10f
        )
        _uiState.update { current ->
            current.copy(
                isFieldGpsActive = false,
                statusMessage = "GPS tidak aktif — Posko Karhutla Hardi Mantangai sebagai referensi"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
        locationListener?.let { locationManager?.removeUpdates(it) }
    }
}

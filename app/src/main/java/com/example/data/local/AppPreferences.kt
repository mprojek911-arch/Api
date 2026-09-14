package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kalimantan_fire_prefs", Context.MODE_PRIVATE)

    var nasaFirmsMapKey: String
        get() = prefs.getString(KEY_MAP_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MAP_KEY, value.trim()).apply()

    var autoRefreshIntervalMinutes: Int
        get() = prefs.getInt(KEY_REFRESH_INTERVAL, 15) // Default 15 menit sesuai mandat
        set(value) = prefs.edit().putInt(KEY_REFRESH_INTERVAL, value).apply()

    var lastSuccessfulSyncEpochMs: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    var lastSyncStatus: String
        get() = prefs.getString(KEY_LAST_SYNC_STATUS, "Belum pernah sinkronisasi") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SYNC_STATUS, value).apply()

    var isLastSyncSuccess: Boolean
        get() = prefs.getBoolean(KEY_LAST_SYNC_SUCCESS, false)
        set(value) = prefs.edit().putBoolean(KEY_LAST_SYNC_SUCCESS, value).apply()

    var notificationRadiusKm: Int
        get() = prefs.getInt(KEY_NOTIF_RADIUS, 25)
        set(value) = prefs.edit().putInt(KEY_NOTIF_RADIUS, value).apply()

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ENABLED, value).apply()

    companion object {
        private const val KEY_MAP_KEY = "nasa_firms_map_key"
        private const val KEY_REFRESH_INTERVAL = "auto_refresh_interval"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_LAST_SYNC_STATUS = "last_sync_status"
        private const val KEY_LAST_SYNC_SUCCESS = "last_sync_success"
        private const val KEY_NOTIF_RADIUS = "notif_radius"
        private const val KEY_NOTIF_ENABLED = "notif_enabled"
    }
}

package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.FireHotspot
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_ID = "fire_alerts_channel"
    private const val CHANNEL_NAME = "Peringatan Titik Panas Karhutla"
    private const val CHANNEL_DESC = "Pemberitahuan darurat saat titik api satelit terdeteksi di sekitar pos pemantauan"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Mengirim notifikasi darurat saat terdeteksi hotspot baru yang terverifikasi.
     */
    fun sendHotspotAlertNotification(context: Context, hotspot: FireHotspot) {
        // Cek izin notifikasi pada Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            hotspot.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distText = hotspot.distanceKm?.let {
            "${String.format(Locale.US, "%.1f", it)} km (${hotspot.bearingDirection ?: "Arah Pos"})"
        } ?: "Wilayah ${hotspot.province}"

        val title = "🚨 HOTSPOT BARU TERDETEKSI — ${hotspot.province}"
        val content = "Jarak: $distText | Satelit: ${hotspot.satellite} | FRP: ${String.format(Locale.US, "%.1f", hotspot.frp)} MW | Waktu: ${hotspot.timeWita}"

        val bigText = """
            🔥 Titik Api Satelit Baru Terdeteksi
            • Lokasi: ${hotspot.province} (${String.format(Locale.US, "%.4f", hotspot.latitude)}, ${String.format(Locale.US, "%.4f", hotspot.longitude)})
            • Jarak: $distText
            • Waktu Observasi: ${hotspot.timeWita} / ${hotspot.timeWib}
            • Satelit: ${hotspot.satellite} (${hotspot.instrument})
            • Keyakinan: ${hotspot.confidence.uppercase()} (${hotspot.confidenceRaw})
            • Daya Radiasi Termal (FRP): ${String.format(Locale.US, "%.1f", hotspot.frp)} MW
            • Umur Data: ${hotspot.dataAgeFormatted} (${hotspot.dataAgeCategory.label})
            
            Segera lakukan verifikasi lapangan!
        """.trimIndent()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(hotspot.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }
}

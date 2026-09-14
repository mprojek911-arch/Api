package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FireHotspot
import com.example.util.GeoUtils

@Entity(tableName = "fire_hotspots")
data class FireHotspotEntity(
    @PrimaryKey
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val acqDate: String,
    val acqTime: String,
    val satellite: String,
    val instrument: String,
    val confidence: String,
    val confidenceRaw: String,
    val frp: Double,
    val scan: Double,
    val track: Double,
    val dayNight: String,
    val version: String,
    val source: String,
    val retrievedAt: Long = System.currentTimeMillis(),
    val observationEpochMs: Long = 0L,
    val province: String = "Kalimantan Tengah",
    val regency: String = ""
) {
    fun toDomainModel(
        distanceKm: Double? = null,
        bearing: String? = null,
        nowEpochMs: Long = System.currentTimeMillis()
    ): FireHotspot {
        val timeInfo = GeoUtils.parseSatelliteUtcTime(acqDate, acqTime)
        val epochMs = if (observationEpochMs > 0L) observationEpochMs else timeInfo.epochMs
        val (ageMin, ageFormatted, ageCategory) = GeoUtils.calculateDataAge(epochMs, nowEpochMs)

        return FireHotspot(
            id = id,
            latitude = latitude,
            longitude = longitude,
            acqDate = acqDate,
            acqTime = acqTime,
            satellite = satellite,
            instrument = instrument,
            confidence = confidence,
            confidenceRaw = confidenceRaw,
            frp = frp,
            scan = scan,
            track = track,
            dayNight = dayNight,
            version = version,
            source = source,
            retrievedAt = retrievedAt,
            observationEpochMs = epochMs,
            timeWita = timeInfo.witaFormatted,
            timeWib = timeInfo.wibFormatted,
            dataAgeMinutes = ageMin,
            dataAgeFormatted = ageFormatted,
            dataAgeCategory = ageCategory,
            province = province,
            regency = regency,
            distanceKm = distanceKm,
            bearingDirection = bearing
        )
    }

    companion object {
        fun fromDomainModel(model: FireHotspot): FireHotspotEntity {
            return FireHotspotEntity(
                id = model.id,
                latitude = model.latitude,
                longitude = model.longitude,
                acqDate = model.acqDate,
                acqTime = model.acqTime,
                satellite = model.satellite,
                instrument = model.instrument,
                confidence = model.confidence,
                confidenceRaw = model.confidenceRaw,
                frp = model.frp,
                scan = model.scan,
                track = model.track,
                dayNight = model.dayNight,
                version = model.version,
                source = model.source,
                retrievedAt = model.retrievedAt,
                observationEpochMs = model.observationEpochMs,
                province = model.province,
                regency = model.regency
            )
        }
    }
}

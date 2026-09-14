package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

/**
 * Service API resmi NASA FIRMS (Fire Information for Resource Management System).
 * Menyediakan akses ke observasi anomali termal aktif sensor satelit:
 * - VIIRS (Suomi-NPP, NOAA-20, NOAA-21) resolusi 375m
 * - MODIS (Terra & Aqua) resolusi 1km
 */
interface FirmsApiService {

    /**
     * Area Query resmi NASA FIRMS menggunakan MAP_KEY terdaftar.
     * Bounding box Kalimantan: minLon,minLat,maxLon,maxLat (108.0,-4.5,119.2,4.3)
     */
    @GET("api/area/csv/{mapKey}/{source}/{area}/{range}")
    suspend fun getAreaCsv(
        @Path("mapKey") mapKey: String,
        @Path("source") source: String,
        @Path("area") area: String,
        @Path("range") range: Int
    ): Response<ResponseBody>

    /**
     * Mengunduh feed CSV NRT publik terbuka NASA FIRMS untuk Asia Tenggara.
     * Tidak memerlukan API key, selalu tersedia dan terpercaya langsung dari server NASA.
     */
    @GET
    suspend fun getDirectCsv(@Url url: String): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://firms.modaps.eosdis.nasa.gov/"

        // URL Feed NRT South East Asia resmi NASA FIRMS
        const val FEED_VIIRS_NOAA21 = "https://firms.modaps.eosdis.nasa.gov/data/active_fire/noaa-21-viirs-c2/csv/J2_VIIRS_C2_SouthEast_Asia_24h.csv"
        const val FEED_VIIRS_NOAA20 = "https://firms.modaps.eosdis.nasa.gov/data/active_fire/noaa-20-viirs-c2/csv/J1_VIIRS_C2_SouthEast_Asia_24h.csv"
        const val FEED_VIIRS_SNPP = "https://firms.modaps.eosdis.nasa.gov/data/active_fire/suomi-npp-viirs-c2/csv/SUOMI_VIIRS_C2_SouthEast_Asia_24h.csv"
        const val FEED_MODIS = "https://firms.modaps.eosdis.nasa.gov/data/active_fire/modis-c6.1/csv/MODIS_C6_1_SouthEast_Asia_24h.csv"

        fun create(): FirmsApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(35, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .build()
                .create(FirmsApiService::class.java)
        }
    }
}

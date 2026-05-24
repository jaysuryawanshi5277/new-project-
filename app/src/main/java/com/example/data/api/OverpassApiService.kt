package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    @Json(name = "elements") val elements: List<OsmElement> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OsmElement(
    @Json(name = "id") val id: Long,
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double,
    @Json(name = "tags") val tags: Map<String, String>? = null
)

interface OverpassApiService {
    @POST("api/interpreter")
    suspend fun getNearbyPharmacies(
        @Body body: RequestBody
    ): OverpassResponse
}

object OverpassRetrofitClient {
    private const val BASE_URL = "https://overpass-api.de/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val overpassApiService: OverpassApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OverpassApiService::class.java)
    }
}

data class PharmacyUiModel(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val isOpenStr: String?, // "Open", "Closed", or null
    val vicinity: String?,
    val phoneNumber: String?,
    val latitude: Double,
    val longitude: Double
)

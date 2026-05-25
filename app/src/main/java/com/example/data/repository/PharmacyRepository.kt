package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.example.data.api.OverpassRetrofitClient
import com.example.data.api.PharmacyUiModel
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume
import kotlin.math.*

class PharmacyRepository(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Haversine formula to compute distance in km between two lat/lng pairs
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return r * c
    }

    @SuppressLint("MissingPermission")
    suspend fun getUserLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    if (continuation.isActive) continuation.resume(task.result)
                } else {
                    fusedLocationClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnCompleteListener { currentTask ->
                        if (continuation.isActive) {
                            if (currentTask.isSuccessful && currentTask.result != null) {
                                continuation.resume(currentTask.result)
                            } else {
                                continuation.resume(null)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(null)
        }
    }

    suspend fun fetchNearbyPharmacies(lat: Double, lng: Double): List<PharmacyUiModel> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val queryText = """
            [out:json];
            node["amenity"="pharmacy"](around:3000,$lat,$lng);
            out body;
        """.trimIndent()

        val requestBody = queryText.toRequestBody("text/plain".toMediaTypeOrNull())
        val response = OverpassRetrofitClient.overpassApiService.getNearbyPharmacies(requestBody)

        response.elements.map { element ->
            val tags = element.tags ?: emptyMap()
            
            // Extract attributes from OSM element tags
            val name = tags["name"] ?: tags["brand"] ?: "OSM Pharmacy"
            val phone = tags["phone"] ?: tags["contact:phone"]
            val openingHours = tags["opening_hours"]
            val distance = calculateDistanceKm(lat, lng, element.lat, element.lon)
            
            // Show custom hours status if present
            val hours = if (!openingHours.isNullOrEmpty()) {
                 "Hours: $openingHours"
            } else {
                 null
            }

            val street = tags["addr:street"] ?: ""
            val housenumber = tags["addr:housenumber"] ?: ""
            val city = tags["addr:city"] ?: ""
            var address = if (street.isNotEmpty()) "$housenumber $street, $city".trim() else ""
            if (address.isEmpty()) {
                val suburb = tags["addr:suburb"] ?: ""
                address = if (suburb.isNotEmpty()) "$suburb Area" else "Near lat: ${String.format(java.util.Locale.US, "%.4f", element.lat)}"
            }

            PharmacyUiModel(
                id = element.id.toString(),
                name = name,
                distanceKm = distance,
                isOpenStr = hours,
                vicinity = address,
                phoneNumber = phone,
                latitude = element.lat,
                longitude = element.lon
            )
        }.sortedBy { it.distanceKm }
    }
}

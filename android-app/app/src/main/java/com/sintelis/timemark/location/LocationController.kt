package com.sintelis.timemark.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.sintelis.timemark.domain.LocationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationController(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var activeListener: LocationListener? = null

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startUpdates(onLocationFix: (Double, Double) -> Unit) {
        if (!hasLocationPermission()) return
        stopUpdates()

        // Emit best last known fix immediately for responsive UI
        bestLastKnownLocation()?.let { loc ->
            onLocationFix(loc.latitude, loc.longitude)
        }

        val listener = LocationListener { location ->
            onLocationFix(location.latitude, location.longitude)
        }
        activeListener = listener

        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            runCatching {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.requestLocationUpdates(
                        provider,
                        10_000L,
                        15f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            }
        }
    }

    fun stopUpdates() {
        activeListener?.let { listener ->
            runCatching { locationManager.removeUpdates(listener) }
        }
        activeListener = null
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        val candidates = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        return candidates.maxByOrNull { it.time }
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            runCatching {
                val geocoder = Geocoder(
                    context,
                    Locale.Builder().setLanguage("id").setRegion("ID").build()
                )
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var resolved: List<Address>? = null
                    val latch = java.util.concurrent.CountDownLatch(1)
                    geocoder.getFromLocation(lat, lng, 1) { list ->
                        resolved = list
                        latch.countDown()
                    }
                    latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
                    resolved
                } else {
                    geocoder.getFromLocation(lat, lng, 1)
                }
                val addr = addresses?.firstOrNull() ?: return@runCatching null
                formatTwoLineAddress(addr)
            }.getOrNull()
        }

    private fun formatTwoLineAddress(address: Address): Pair<String, String> {
        val full = address.getAddressLine(0)?.trim().orEmpty()
        if (full.isNotBlank()) {
            val parts = full.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 4) {
                val splitIdx = (parts.size + 1) / 2
                val line1 = parts.take(splitIdx).joinToString(", ") + ","
                val line2 = parts.drop(splitIdx).joinToString(", ")
                return line1 to line2
            }
        }
        val line1Parts = listOfNotNull(
            address.thoroughfare,
            address.subLocality,
            address.locality
        ).filter { it.isNotBlank() }
        val line2Parts = listOfNotNull(
            address.subAdminArea,
            address.adminArea,
            address.postalCode
        ).filter { it.isNotBlank() }

        val line1 = line1Parts.joinToString(", ").ifBlank { "Area Jalur KA" }
        val line2 = line2Parts.joinToString(", ").ifBlank { "Indonesia" }
        return line1 to line2
    }

    fun applyGpsFixIfUnlocked(
        current: LocationState,
        lat: Double,
        lng: Double,
        resolvedAddress: Pair<String, String>?
    ): LocationState {
        if (current.isManualLocked) return current
        return current.copy(
            latitude = lat,
            longitude = lng,
            addressLine1 = resolvedAddress?.first ?: current.addressLine1,
            addressLine2 = resolvedAddress?.second ?: current.addressLine2,
            hasGpsFix = true
        )
    }
}

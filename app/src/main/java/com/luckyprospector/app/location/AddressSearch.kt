package com.luckyprospector.app.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.luckyprospector.app.domain.GeoPoint
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class AddressResult(val label: String, val point: GeoPoint)

/**
 * Turns an address or a place name ("Parco Ducale, Parma") into coordinates for a
 * finding added by hand. Uses the phone's own geocoder, which on most devices sends
 * the query to Google; the resulting position is stored only on the phone.
 */
class AddressSearch(private val context: Context) {

    val available: Boolean get() = Geocoder.isPresent()

    suspend fun search(query: String, maxResults: Int = 5): List<AddressResult> {
        if (query.isBlank() || !available) return emptyList()
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(query, maxResults, object : Geocoder.GeocodeListener {
                    override fun onGeocode(results: MutableList<Address>) { continuation.resume(results) }
                    override fun onError(errorMessage: String?) { continuation.resume(emptyList()) }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocationName(query, maxResults).orEmpty() }.getOrDefault(emptyList())
            }
        }
        return addresses.filter { it.hasLatitude() && it.hasLongitude() }.map { address ->
            val label = (0..address.maxAddressLineIndex).mapNotNull(address::getAddressLine).joinToString(", ")
                .ifBlank { listOfNotNull(address.featureName, address.locality, address.countryName).joinToString(", ") }
            AddressResult(label, GeoPoint(address.latitude, address.longitude, accuracyMeters = null))
        }
    }
}

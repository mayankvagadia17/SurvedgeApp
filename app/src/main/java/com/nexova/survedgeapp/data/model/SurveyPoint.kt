package com.nexova.survedgeapp.data.model

import org.osmdroid.util.GeoPoint

/**
 * Represents a survey point with coordinates and metadata
 */
data class SurveyPoint(
    val id: String,
    val name: String,
    val code: String = "",
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val isHighlighted: Boolean = false
) {
    /**
     * Convert to osmdroid GeoPoint for rendering
     */
    fun toGeoPoint(): GeoPoint {
        return GeoPoint(latitude, longitude)
    }
}


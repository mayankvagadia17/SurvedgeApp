package com.nexova.survedgeapp.data.model

import org.maplibre.geojson.Point

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
     * Convert to MapLibre Point for rendering
     */
    fun toMapLibrePoint(): Point {
        return Point.fromLngLat(longitude, latitude)
    }
}


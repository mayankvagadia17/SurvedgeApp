package com.nexova.survedgeapp.data.model

import org.maplibre.geojson.LineString

/**
 * Represents a survey line connecting multiple points
 */
data class SurveyLine(
    val id: String,
    val name: String,
    val code: String = "",
    val points: List<SurveyPoint>,
    val isClosed: Boolean = false
) {
    /**
     * Convert to MapLibre LineString for rendering
     */
    fun toMapLibreLineString(): LineString {
        val coordinates = points.map { it.toMapLibrePoint() }
        // If closed, add first point at the end
        val finalCoordinates = if (isClosed && coordinates.isNotEmpty()) {
            coordinates + coordinates.first()
        } else {
            coordinates
        }
        return LineString.fromLngLats(finalCoordinates)
    }
}


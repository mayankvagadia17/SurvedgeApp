package com.nexova.survedgeapp.data.model

import org.osmdroid.util.GeoPoint

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
     * Convert to osmdroid GeoPoint list for rendering
     */
    fun toGeoPoints(): List<GeoPoint> {
        val geoPoints = points.map { it.toGeoPoint() }
        // If closed, add first point at the end
        return if (isClosed && geoPoints.isNotEmpty()) {
            geoPoints + geoPoints.first()
        } else {
            geoPoints
        }
    }
}


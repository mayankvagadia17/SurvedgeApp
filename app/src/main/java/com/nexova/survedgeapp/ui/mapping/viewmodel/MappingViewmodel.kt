package com.nexova.survedgeapp.ui.mapping.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexova.survedgeapp.data.model.SurveyLine
import com.nexova.survedgeapp.data.model.SurveyPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MappingViewModel : ViewModel() {

    private val _points = MutableLiveData<List<SurveyPoint>>(emptyList())
    val points: LiveData<List<SurveyPoint>> = _points

    private val _lines = MutableLiveData<List<SurveyLine>>(emptyList())
    val lines: LiveData<List<SurveyLine>> = _lines

    private val _isGeneratingPoints = MutableLiveData<Boolean>(false)
    val isGeneratingPoints: LiveData<Boolean> = _isGeneratingPoints

    /**
     * Generate dummy points similar to the screenshot
     * Creates points P1-P11, B1-B4, T1, RF1, RF3
     * Based on coordinates around Delhi area (28.7041, 77.1025)
     */
    fun generateDummyPoints() {
        viewModelScope.launch {
            _isGeneratingPoints.value = true
            withContext(Dispatchers.Default) {
                val generatedPoints = mutableListOf<SurveyPoint>()
                
                // Base coordinates (Delhi area - matching screenshot pattern)
                val baseLat = 28.7041
                val baseLng = 77.1025
                
                // Generate P1-P11 points in a specific polygonal shape
                // Creating a more structured polygon pattern
                val polygonPoints = listOf(
                    Pair(baseLat + 0.0010, baseLng + 0.0000),  // P1
                    Pair(baseLat + 0.0009, baseLng + 0.0005),  // P2
                    Pair(baseLat + 0.0014, baseLng + 0.0015),  // P3
                    Pair(baseLat + 0.0011, baseLng + 0.0025),  // P4
                    Pair(baseLat + 0.0004, baseLng + 0.0030),  // P5
                    Pair(baseLat - 0.0006, baseLng + 0.0027),  // P6
                    Pair(baseLat - 0.0013, baseLng + 0.0023),  // P7
                    Pair(baseLat - 0.0016, baseLng + 0.0013),  // P8
                    Pair(baseLat - 0.0011, baseLng + 0.0003),  // P9
                    Pair(baseLat - 0.0006, baseLng - 0.0005),  // P10
                    Pair(baseLat - 0.0001, baseLng - 0.0007)   // P11
                )
                
                polygonPoints.forEachIndexed { index, point ->
                    generatedPoints.add(
                        SurveyPoint(
                            id = "P${index + 1}",
                            name = "P${index + 1}",
                            code = "BLD1",
                            latitude = point.first,
                            longitude = point.second
                        )
                    )
                }
                
                // Generate B1-B4 points near P10 and P11 area
                val bPoints = listOf(
                    Pair(baseLat - 0.0003, baseLng - 0.0010),  // B1
                    Pair(baseLat - 0.0008, baseLng - 0.0012),  // B2
                    Pair(baseLat - 0.0011, baseLng - 0.0007),  // B3
                    Pair(baseLat - 0.0006, baseLng - 0.0002)   // B4
                )
                
                bPoints.forEachIndexed { index, point ->
                    generatedPoints.add(
                        SurveyPoint(
                            id = "B${index + 1}",
                            name = "B${index + 1}",
                            code = "BLD1",
                            latitude = point.first,
                            longitude = point.second
                        )
                    )
                }
                
                // Generate T1 point (Tree) - positioned outside the polygon
                generatedPoints.add(
                    SurveyPoint(
                        id = "T1",
                        name = "T1",
                        code = "TRE",
                        latitude = baseLat + 0.0015,
                        longitude = baseLng - 0.0005
                    )
                )
                
                // Generate RF1 and RF3 points
                generatedPoints.add(
                    SurveyPoint(
                        id = "RF1",
                        name = "RF1",
                        code = "RF",
                        latitude = baseLat + 0.0004,
                        longitude = baseLng + 0.0005
                    )
                )
                
                generatedPoints.add(
                    SurveyPoint(
                        id = "RF3",
                        name = "RF3",
                        code = "RF",
                        latitude = baseLat - 0.0011,
                        longitude = baseLng + 0.0030
                    )
                )
                
                // Generate line connecting P1-P11 (closed polygon)
                val polygonLinePoints = polygonPoints.mapIndexed { index, point ->
                    SurveyPoint(
                        id = "P${index + 1}",
                        name = "P${index + 1}",
                        code = "BLD1",
                        latitude = point.first,
                        longitude = point.second
                    )
                }
                
                val polygonLine = SurveyLine(
                    id = "LINE_P1_P11",
                    name = "Polygon Line",
                    code = "BLD1",
                    points = polygonLinePoints,
                    isClosed = true
                )
                
                withContext(Dispatchers.Main) {
                    _points.value = generatedPoints
                    _lines.value = listOf(polygonLine)
                    _isGeneratingPoints.value = false
                }
            }
        }
    }
    
    /**
     * Clear all points and lines
     */
    fun clearAll() {
        _points.value = emptyList()
        _lines.value = emptyList()
    }
    
    /**
     * Add a single point
     */
    fun addPoint(point: SurveyPoint) {
        val currentPoints = _points.value?.toMutableList() ?: mutableListOf()
        currentPoints.add(point)
        _points.value = currentPoints
    }
    
    /**
     * Remove a point by ID
     */
    fun removePoint(pointId: String) {
        val currentPoints = _points.value?.toMutableList() ?: mutableListOf()
        currentPoints.removeAll { it.id == pointId }
        _points.value = currentPoints
    }
}
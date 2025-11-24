package com.nexova.survedgeapp.ui.mapping.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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

    private val _currentLocation = MutableLiveData<Location?>(null)
    val currentLocation: LiveData<Location?> = _currentLocation

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

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
     * Generate N points randomly scattered and connect them
     * Some points will be highlighted for better visibility
     * Points are generated near the current device location
     * @param numberOfPoints Number of points to generate (minimum 3)
     */
    fun generatePoints(numberOfPoints: Int) {
        if (numberOfPoints < 3) {
            return // Need at least 3 points to form a polygon
        }
        
        viewModelScope.launch {
            _isGeneratingPoints.value = true
            withContext(Dispatchers.Default) {
                val generatedPoints = mutableListOf<SurveyPoint>()
                
                // Get current location or use default (Delhi area)
                val currentLocation = _currentLocation.value
                val baseLat: Double
                val baseLng: Double
                
                if (currentLocation != null) {
                    // Use current location as base
                    baseLat = currentLocation.latitude
                    baseLng = currentLocation.longitude
                    android.util.Log.d("MappingViewModel", "Generating points near current location: lat=$baseLat, lng=$baseLng")
                } else {
                    // Fallback to default location if current location is not available
                    baseLat = 28.7041
                    baseLng = 77.1025
                    android.util.Log.d("MappingViewModel", "Current location not available, using default location: lat=$baseLat, lng=$baseLng")
                }
                
                // Define area bounds for random distribution
                // Using a larger area to ensure points are well spread and visible
                val latRange = 0.0030  // ~330 meters north-south
                val lngRange = 0.0030  // ~330 meters east-west
                
                // Use a seeded random for consistent results if needed, or system random for variety
                val random = Random(System.currentTimeMillis())
                
                // Calculate how many points should be highlighted (approximately 30% of points)
                val highlightCount = maxOf(1, (numberOfPoints * 0.3).toInt())
                val highlightIndices = (0 until numberOfPoints).shuffled(random).take(highlightCount).toSet()
                
                // Generate points randomly scattered within the area
                for (i in 0 until numberOfPoints) {
                    // Generate random offset from base coordinates
                    val latOffset = random.nextDouble(-latRange, latRange)
                    val lngOffset = random.nextDouble(-lngRange, lngRange)
                    
                    val lat = baseLat + latOffset
                    val lng = baseLng + lngOffset
                    
                    // Mark some points as highlighted
                    val isHighlighted = highlightIndices.contains(i)
                    
                    generatedPoints.add(
                        SurveyPoint(
                            id = "P${i + 1}",
                            name = "P${i + 1}",
                            code = "BLD1",
                            latitude = lat,
                            longitude = lng,
                            isHighlighted = isHighlighted
                        )
                    )
                }
                
                // Create a line connecting all points in order (closed polygon)
                // Points are connected in the order they were generated
                val polygonLine = SurveyLine(
                    id = "LINE_P1_P$numberOfPoints",
                    name = "Polygon Line",
                    code = "BLD1",
                    points = generatedPoints,
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

    /**
     * Start tracking device location
     */
    fun startLocationTracking(context: Context) {
        if (locationManager != null) {
            android.util.Log.d("MappingViewModel", "Location tracking already started")
            return // Already tracking
        }

        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Check if location permission is granted
        val hasPermission = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            android.util.Log.w("MappingViewModel", "Location permission not granted")
            return
        }

        // Check if location services are enabled
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            android.util.Log.w("MappingViewModel", "Location services not enabled")
            return
        }

        android.util.Log.d("MappingViewModel", "Starting location tracking...")

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                android.util.Log.d("MappingViewModel", "Location updated: lat=${location.latitude}, lng=${location.longitude}")
                _currentLocation.postValue(location)
            }

            override fun onProviderEnabled(provider: String) {
                android.util.Log.d("MappingViewModel", "Provider enabled: $provider")
            }
            
            override fun onProviderDisabled(provider: String) {
                android.util.Log.d("MappingViewModel", "Provider disabled: $provider")
            }
        }

        try {
            // Try to get last known location first
            val lastKnownLocation = if (isGpsEnabled) {
                locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null
                ?: if (isNetworkEnabled) {
                    locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } else null
                
            if (lastKnownLocation != null) {
                android.util.Log.d("MappingViewModel", "Using last known location: lat=${lastKnownLocation.latitude}, lng=${lastKnownLocation.longitude}")
                _currentLocation.postValue(lastKnownLocation)
            } else {
                android.util.Log.d("MappingViewModel", "No last known location available")
            }

            // Request location updates
            if (isGpsEnabled) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L, // Update every 1 second
                    5f, // Update if moved 5 meters
                    locationListener!!
                )
                android.util.Log.d("MappingViewModel", "Requested GPS location updates")
            }
            
            if (isNetworkEnabled) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L, // Update every 1 second
                    5f, // Update if moved 5 meters
                    locationListener!!
                )
                android.util.Log.d("MappingViewModel", "Requested Network location updates")
            }
        } catch (e: SecurityException) {
            // Permission not granted
            android.util.Log.e("MappingViewModel", "SecurityException: Permission not granted", e)
            e.printStackTrace()
        } catch (e: Exception) {
            android.util.Log.e("MappingViewModel", "Error starting location tracking", e)
            e.printStackTrace()
        }
    }

    /**
     * Stop tracking device location
     */
    fun stopLocationTracking() {
        locationListener?.let { listener ->
            locationManager?.removeUpdates(listener)
        }
        locationListener = null
        locationManager = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationTracking()
    }
}
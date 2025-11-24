package com.nexova.survedgeapp.ui.mapping.fragment

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nexova.survedgeapp.R
import com.nexova.survedgeapp.databinding.FragmentMappingBinding
import com.nexova.survedgeapp.ui.mapping.viewmodel.MappingViewModel
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.util.concurrent.atomic.AtomicBoolean

class MappingFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMappingBinding? = null
    private val binding get() = _binding!!

    protected lateinit var viewModel: MappingViewModel
    private var mapLibreMap: MapLibreMap? = null
    private val isMapReady = AtomicBoolean(false)
    private var hasCenteredOnLocation = false
    
    // Sources and layers IDs
    private val POINTS_SOURCE_ID = "points-source"
    private val LINES_SOURCE_ID = "lines-source"
    private val CURRENT_LOCATION_SOURCE_ID = "current-location-source"
    private val POINTS_LAYER_ID = "points-layer"
    private val POINTS_LABEL_LAYER_ID = "points-label-layer"
    private val LINES_LAYER_ID = "lines-layer"
    private val CURRENT_LOCATION_LAYER_ID = "current-location-layer"
    
    companion object {
        // Using CartoDB Positron style - a reliable free tile server that works with MapLibre
        // This provides good map visibility at all zoom levels
        private const val MAP_STYLE_JSON = """
        {
            "version": 8,
            "sources": {
                "carto": {
                    "type": "raster",
                    "tiles": [
                        "https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
                        "https://b.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
                        "https://c.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png"
                    ],
                    "tileSize": 256,
                    "attribution": "© OpenStreetMap contributors © CARTO"
                }
            },
            "layers": [{
                "id": "carto-layer",
                "type": "raster",
                "source": "carto",
                "minzoom": 0,
                "maxzoom": 18
            }]
        }
        """
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMappingBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[MappingViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize map
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        
        // Setup zoom controls
        setupZoomControls()
        
        // Setup generate points button
        setupGenerateButton()
        
        // Observe ViewModel
        observeViewModel()
        
        // Start location tracking
        startLocationTracking()
    }

    override fun onMapReady(map: MapLibreMap) {
        this.mapLibreMap = map
        
        android.util.Log.d("MappingFragment", "Map is ready, loading map style")
        
        // Load map style - try CartoDB tiles first, fallback to demo tiles if needed
        // Using a reliable tile server that works without API key
        try {
            map.setStyle(
                Style.Builder()
                    .fromJson(MAP_STYLE_JSON)
            ) { style ->
                android.util.Log.d("MappingFragment", "CartoDB style loaded successfully")
                onStyleLoaded(map, style)
            }
        } catch (e: Exception) {
            android.util.Log.e("MappingFragment", "Error loading CartoDB style, using demo tiles", e)
            // Fallback to MapLibre demo tiles which are guaranteed to work
            map.setStyle(
                Style.Builder()
                    .fromUri("https://demotiles.maplibre.org/style.json")
            ) { style ->
                android.util.Log.d("MappingFragment", "Demo tiles style loaded successfully")
                onStyleLoaded(map, style)
            }
        }
    }
    
    private fun onStyleLoaded(map: MapLibreMap, style: Style) {
        isMapReady.set(true)
        
        // Initialize sources
        initializeSources(style)
        
        // Initialize layers
        initializeLayers(style)
        
        // Set zoom limits to match tile server capabilities
        // CartoDB supports up to zoom 18, so limit to 18 to prevent black screen
        map.setMinZoomPreference(1.0)
        map.setMaxZoomPreference(18.0)
        
        // Enable smooth gestures for better UX
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true
        map.uiSettings.isTiltGesturesEnabled = true
        
        // Load existing points if any
        updateMapWithPoints()
        
        // Try to center on current location if available, otherwise use default
        centerOnCurrentLocationOrDefault()
    }

    private fun initializeSources(style: Style) {
        // Points source
        style.addSource(GeoJsonSource(POINTS_SOURCE_ID))
        
        // Lines source
        style.addSource(GeoJsonSource(LINES_SOURCE_ID))
        
        // Current location source
        style.addSource(GeoJsonSource(CURRENT_LOCATION_SOURCE_ID))
    }

    private fun initializeLayers(style: Style) {
        // Points circle layer - optimized for performance
        // Use conditional expressions to highlight some points
        val circleLayer = CircleLayer(POINTS_LAYER_ID, POINTS_SOURCE_ID)
            .withProperties(
                // Larger radius for highlighted points (14f), smaller for regular (8f)
                PropertyFactory.circleRadius(
                    Expression.switchCase(
                        Expression.eq(Expression.get("isHighlighted"), Expression.literal(true)),
                        Expression.literal(14f),
                        Expression.literal(8f)
                    )
                ),
                // Different color for highlighted points (red), black for regular
                PropertyFactory.circleColor(
                    Expression.switchCase(
                        Expression.eq(Expression.get("isHighlighted"), Expression.literal(true)),
                        Expression.literal(Color.parseColor("#FF5722")), // Red/Orange for highlighted
                        Expression.literal(Color.BLACK)
                    )
                ),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(Color.WHITE)
            )
        style.addLayer(circleLayer)
        
        // Points label layer - optimized for performance
        val symbolLayer = SymbolLayer(POINTS_LABEL_LAYER_ID, POINTS_SOURCE_ID)
            .withProperties(
                PropertyFactory.textField(Expression.get("name")),
                PropertyFactory.textSize(12f),
                PropertyFactory.textColor(Color.WHITE),
                PropertyFactory.textHaloColor(Color.BLACK),
                PropertyFactory.textHaloWidth(1f),
                PropertyFactory.textOffset(arrayOf(0f, 1.5f)),
                PropertyFactory.textAnchor("top")
            )
        style.addLayer(symbolLayer)
        
        // Lines layer - optimized for performance
        val lineLayer = LineLayer(LINES_LAYER_ID, LINES_SOURCE_ID)
            .withProperties(
                PropertyFactory.lineColor(Color.parseColor("#4A4A4A")),
                PropertyFactory.lineWidth(3f),
                PropertyFactory.lineCap("round"),
                PropertyFactory.lineJoin("round")
            )
        style.addLayer(lineLayer)
        
        // Current location layer - distinct blue pin (added last so it appears on top)
        val currentLocationLayer = CircleLayer(CURRENT_LOCATION_LAYER_ID, CURRENT_LOCATION_SOURCE_ID)
            .withProperties(
                PropertyFactory.circleRadius(12f), // Larger than regular points
                PropertyFactory.circleColor(Color.parseColor("#2196F3")), // Blue color
                PropertyFactory.circleStrokeWidth(3f),
                PropertyFactory.circleStrokeColor(Color.WHITE)
            )
        // Add location layer after all other layers so it appears on top
        style.addLayer(currentLocationLayer)
    }

    private fun setupZoomControls() {
        binding.btnZoomIn.setOnClickListener {
            mapLibreMap?.let { map ->
                val currentZoom = map.cameraPosition.zoom
                // Limit max zoom to 18 to match tile server capabilities
                if (currentZoom < 18.0) {
                    // Use easeCamera for smoother zoom without animation lag
                    map.easeCamera(
                        CameraUpdateFactory.zoomTo(currentZoom + 1.0)
                    )
                }
            }
        }
        
        binding.btnZoomOut.setOnClickListener {
            mapLibreMap?.let { map ->
                val currentZoom = map.cameraPosition.zoom
                // Limit min zoom for performance
                if (currentZoom > 1.0) {
                    // Use easeCamera for smoother zoom without animation lag
                    map.easeCamera(
                        CameraUpdateFactory.zoomTo(currentZoom - 1.0)
                    )
                }
            }
        }
        
        binding.btnCenter.setOnClickListener {
            mapLibreMap?.let { map ->
                // Center on all points if available
                val points = viewModel.points.value
                if (points?.isNotEmpty() == true) {
                    val bounds = org.maplibre.android.geometry.LatLngBounds.Builder()
                    points.forEach { point ->
                        bounds.include(LatLng(point.latitude, point.longitude))
                    }
                    map.easeCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            bounds.build(),
                            100
                        )
                    )
                } else {
                    // Default center (Delhi area matching dummy points)
                    map.easeCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(28.7041, 77.1025),
                            13.0
                        )
                    )
                }
            }
        }
    }

    private fun setupGenerateButton() {
        binding.btnGeneratePoints.setOnClickListener {
            showGeneratePointsDialog()
        }
    }
    
    private fun showGeneratePointsDialog() {
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Enter number of points (minimum 3)"
        // Add padding to the EditText
        val padding = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding, padding, padding)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Generate Points")
            .setMessage("Enter the number of points you want to generate:")
            .setView(input)
            .setPositiveButton("Generate") { _, _ ->
                val inputText = input.text.toString()
                if (inputText.isNotEmpty()) {
                    try {
                        val numberOfPoints = inputText.toInt()
                        if (numberOfPoints >= 3) {
                            viewModel.generatePoints(numberOfPoints)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Please enter at least 3 points",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(
                            requireContext(),
                            "Please enter a valid number",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please enter a number",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.points.observe(viewLifecycleOwner) { points ->
            updateMapWithPoints()
        }
        
        viewModel.lines.observe(viewLifecycleOwner) { lines ->
            updateMapWithLines()
        }
        
        viewModel.isGeneratingPoints.observe(viewLifecycleOwner) { isGenerating ->
            binding.btnGeneratePoints.isEnabled = !isGenerating
            binding.btnGeneratePoints.text = if (isGenerating) {
                "Generating..."
            } else {
                "Generate Points"
            }
        }
        
        viewModel.currentLocation.observe(viewLifecycleOwner) { location ->
            updateCurrentLocationPin(location)
            // Center on location when it becomes available (only once)
            // Add a small delay to ensure the pin is rendered first
            if (location != null && !hasCenteredOnLocation && isMapReady.get()) {
                view?.postDelayed({
                    centerOnLocation(location)
                    hasCenteredOnLocation = true
                }, 300) // Small delay to ensure pin is rendered
            }
        }
    }

    private fun updateMapWithPoints() {
        if (!isMapReady.get()) return
        
        val map = mapLibreMap ?: return
        val style = map.style ?: return
        
        val points = viewModel.points.value ?: return
        
        // Optimize: Only update if there are points to render
        if (points.isEmpty()) {
            val source = style.getSourceAs<GeoJsonSource>(POINTS_SOURCE_ID)
            source?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }
        
        // Create features from points - optimized batch creation
        val features = ArrayList<Feature>(points.size)
        points.forEach { point ->
            val feature = Feature.fromGeometry(
                Point.fromLngLat(point.longitude, point.latitude)
            )
            feature.addStringProperty("name", point.name)
            feature.addStringProperty("code", point.code)
            feature.addBooleanProperty("isHighlighted", point.isHighlighted)
            features.add(feature)
        }
        
        // Batch update source
        val source = style.getSourceAs<GeoJsonSource>(POINTS_SOURCE_ID)
        source?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private fun updateMapWithLines() {
        if (!isMapReady.get()) return
        
        val map = mapLibreMap ?: return
        val style = map.style ?: return
        
        val lines = viewModel.lines.value ?: return
        
        // Optimize: Only update if there are lines to render
        if (lines.isEmpty()) {
            val source = style.getSourceAs<GeoJsonSource>(LINES_SOURCE_ID)
            source?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }
        
        // Create features from lines - optimized batch creation
        val features = ArrayList<Feature>(lines.size)
        lines.forEach { line ->
            val lineString = line.toMapLibreLineString()
            val feature = Feature.fromGeometry(lineString)
            feature.addStringProperty("name", line.name)
            feature.addStringProperty("code", line.code)
            features.add(feature)
        }
        
        // Batch update source
        val source = style.getSourceAs<GeoJsonSource>(LINES_SOURCE_ID)
        source?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        // Restart location tracking if it was stopped
        startLocationTracking()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        // Optionally pause location tracking to save battery
        // viewModel.stopLocationTracking()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop location tracking
        viewModel.stopLocationTracking()
        binding.mapView.onDestroy()
        _binding = null
    }

    private fun startLocationTracking() {
        // Start location tracking in ViewModel
        context?.let {
            viewModel.startLocationTracking(it)
        }
    }

    private fun updateCurrentLocationPin(location: android.location.Location?) {
        if (!isMapReady.get()) {
            android.util.Log.d("MappingFragment", "Map not ready, skipping location pin update")
            return
        }

        val map = mapLibreMap ?: return
        val style = map.style ?: return

        val source = style.getSourceAs<GeoJsonSource>(CURRENT_LOCATION_SOURCE_ID)
        if (source == null) {
            android.util.Log.e("MappingFragment", "Current location source not found!")
            return
        }

        if (location != null) {
            val lat = location.latitude
            val lng = location.longitude
            android.util.Log.d("MappingFragment", "Updating location pin: lat=$lat, lng=$lng")
            // Create a feature for the current location
            // Point.fromLngLat expects (longitude, latitude) order
            val feature = Feature.fromGeometry(
                Point.fromLngLat(lng, lat)
            )
            feature.addStringProperty("name", "Current Location")
            // Use FeatureCollection even for a single feature
            source.setGeoJson(FeatureCollection.fromFeatures(listOf(feature)))
            
            // Log to verify coordinates match
            android.util.Log.d("MappingFragment", "Location pin set at: lng=$lng, lat=$lat")
        } else {
            android.util.Log.d("MappingFragment", "Location is null, clearing location pin")
            // Clear the location pin if location is null
            source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }
    }

    private fun centerOnCurrentLocationOrDefault() {
        val location = viewModel.currentLocation.value
        if (location != null) {
            // Update the location pin first, then center after a small delay
            updateCurrentLocationPin(location)
            view?.postDelayed({
                if (!hasCenteredOnLocation) {
                    centerOnLocation(location)
                    hasCenteredOnLocation = true
                }
            }, 500) // Delay to ensure pin is rendered
        } else {
            // Default center (Delhi area) if location is not available
            val defaultPosition = CameraPosition.Builder()
                .target(LatLng(28.7041, 77.1025))
                .zoom(13.0)
                .build()
            mapLibreMap?.cameraPosition = defaultPosition
        }
    }

    private fun centerOnLocation(location: android.location.Location) {
        mapLibreMap?.let { map ->
            val lat = location.latitude
            val lng = location.longitude
            // LatLng constructor expects (latitude, longitude) order
            val targetLatLng = LatLng(lat, lng)
            android.util.Log.d("MappingFragment", "Centering map on location: lat=$lat, lng=$lng")
            
            // Use moveCamera for immediate and precise positioning
            // This ensures the camera is exactly on the location pin coordinates
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    targetLatLng,
                    15.0 // Zoom level for current location
                )
            )
            
            android.util.Log.d("MappingFragment", "Map centered at: lat=$lat, lng=$lng")
        }
    }
}
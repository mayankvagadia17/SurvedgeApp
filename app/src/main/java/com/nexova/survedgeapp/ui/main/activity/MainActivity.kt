package com.nexova.survedgeapp.ui.main.activity

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.nexova.survedgeapp.R
import com.nexova.survedgeapp.databinding.ActivityMainBinding
import com.nexova.survedgeapp.ui.main.viewmodel.LocationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var locationViewModel: LocationViewModel
    private var locationDialog: AlertDialog? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            checkLocationEnabled()
        } else {
            Toast.makeText(
                this,
                "Location permission is required for this app to function properly",
                Toast.LENGTH_LONG
            ).show()
            requestLocationPermission()
        }
    }

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check location status after returning from settings
        checkLocationEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize ViewModel
        locationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]

        // Observe location state changes
        observeLocationState()

        // Check permissions and location status on app start
        checkLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        // Re-check location status when app resumes
        if (locationViewModel.checkLocationPermission(this)) {
            checkLocationEnabled()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationDialog?.dismiss()
    }

    /**
     * Check location permission status
     */
    private fun checkLocationPermission() {
        if (!locationViewModel.checkLocationPermission(this)) {
            // Permission not granted, request it
            requestLocationPermission()
        } else {
            // Permission granted, check if location is enabled
            checkLocationEnabled()
        }
    }

    /**
     * Request location permissions
     */
    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * Check if location services are enabled
     */
    private fun checkLocationEnabled() {
        val isEnabled = locationViewModel.checkLocationEnabled()
        if (!isEnabled) {
            // Location is disabled, show non-cancelable dialog
            showLocationEnableDialog()
        } else {
            // Location is enabled, dismiss dialog if showing
            locationDialog?.dismiss()
        }
    }

    /**
     * Show non-cancelable dialog to enable location
     */
    private fun showLocationEnableDialog() {
        // Dismiss existing dialog if any
        locationDialog?.dismiss()

        locationDialog = AlertDialog.Builder(this)
            .setTitle("Location Required")
            .setMessage("This app requires location services to be enabled. Please enable location in settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openLocationSettings()
            }
            .setCancelable(false) // Non-cancelable dialog
            .create()

        locationDialog?.setCanceledOnTouchOutside(false) // Prevent dismissal by touching outside
        locationDialog?.show()
    }

    /**
     * Open location settings
     */
    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        locationSettingsLauncher.launch(intent)
    }

    /**
     * Observe location state changes
     */
    private fun observeLocationState() {
        locationViewModel.shouldShowLocationDialog.observe(this) { shouldShow ->
            if (shouldShow) {
                showLocationEnableDialog()
            }
        }

        // Monitor location state periodically
        lifecycleScope.launch {
            while (true) {
                if (locationViewModel.checkLocationPermission(this@MainActivity)) {
                    val isEnabled = locationViewModel.checkLocationEnabled()
                    if (!isEnabled) {
                        showLocationEnableDialog()
                    } else {
                        locationDialog?.dismiss()
                    }
                }
                delay(2000) // Check every 2 seconds
            }
        }
    }
}
package com.nexova.survedgeapp.ui.base.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.nexova.survedgeapp.ui.base.viewmodel.LocationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var locationViewModel: LocationViewModel
    private var locationDialog: AlertDialog? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted =
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            checkLocationEnabled()
        } else {
            onLocationPermissionDenied()
        }
    }

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkLocationEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]
        setupLocationMonitoring()
        checkLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        if (locationViewModel.checkLocationPermission(this)) {
            checkLocationEnabled()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationDialog?.dismiss()
    }

    private fun setupLocationMonitoring() {
        observeLocationState()
    }

    protected fun checkLocationPermission() {
        if (!locationViewModel.checkLocationPermission(this)) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            ) {
                showPermissionRationale()
            } else {
                requestLocationPermission()
            }
        } else {
            checkLocationEnabled()
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage(getLocationPermissionRationaleMessage())
            .setPositiveButton("Grant Permission") { _, _ ->
                requestLocationPermission()
            }
            .setNegativeButton("Cancel") { _, _ ->
                onLocationPermissionDenied()
            }
            .setCancelable(false)
            .show()
    }

    protected open fun getLocationPermissionRationaleMessage(): String {
        return "This app needs location permission to function properly. Please grant location permission to continue."
    }

    protected fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    protected fun checkLocationEnabled() {
        val isEnabled = locationViewModel.checkLocationEnabled()
        if (!isEnabled) {
            showLocationEnableDialog()
        } else {
            locationDialog?.dismiss()
            onLocationServiceEnabled()
        }
    }

    private fun showLocationEnableDialog() {
        locationDialog?.dismiss()

        locationDialog = AlertDialog.Builder(this)
            .setTitle(getLocationDialogTitle())
            .setMessage(getLocationDialogMessage())
            .setPositiveButton(getLocationDialogPositiveButtonText()) { _, _ ->
                openLocationSettings()
            }
            .setCancelable(isLocationDialogCancelable())
            .create()

        locationDialog?.setCanceledOnTouchOutside(false)
        locationDialog?.show()
    }

    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        locationSettingsLauncher.launch(intent)
    }

    private fun observeLocationState() {
        locationViewModel.shouldShowLocationDialog.observe(this) { shouldShow ->
            if (shouldShow) {
                showLocationEnableDialog()
            }
        }

        lifecycleScope.launch {
            while (true) {
                if (locationViewModel.checkLocationPermission(this@BaseActivity)) {
                    val isEnabled = locationViewModel.checkLocationEnabled()
                    if (!isEnabled) {
                        showLocationEnableDialog()
                    } else {
                        locationDialog?.dismiss()
                        onLocationServiceEnabled()
                    }
                }
                delay(2000)
            }
        }
    }

    protected open fun onLocationPermissionDenied() {
    }


    protected open fun onLocationServiceEnabled() {
        // Override in child activities if needed
    }

    protected open fun getLocationDialogTitle(): String {
        return "Location Required"
    }

    protected open fun getLocationDialogMessage(): String {
        return "This app requires location services to be enabled. Please enable location in settings."
    }

    protected open fun getLocationDialogPositiveButtonText(): String {
        return "Open Settings"
    }

    protected open fun isLocationDialogCancelable(): Boolean {
        return false
    }

    protected fun hasLocationPermission(): Boolean {
        return locationViewModel.checkLocationPermission(this)
    }

    protected fun isLocationServiceEnabled(): Boolean {
        return locationViewModel.checkLocationEnabled()
    }
}
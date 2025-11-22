package com.nexova.survedgeapp.ui.main.viewmodel

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLocationEnabled = MutableLiveData<Boolean>()
    val isLocationEnabled: LiveData<Boolean> = _isLocationEnabled

    private val _isPermissionGranted = MutableLiveData<Boolean>()
    val isPermissionGranted: LiveData<Boolean> = _isPermissionGranted

    private val _shouldShowLocationDialog = MutableLiveData<Boolean>()
    val shouldShowLocationDialog: LiveData<Boolean> = _shouldShowLocationDialog

    private val locationManager: LocationManager =
        application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun checkLocationEnabled(): Boolean {
        val isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        _isLocationEnabled.value = isEnabled
        return isEnabled
    }

    fun checkLocationPermission(context: Context): Boolean {
        val fineLocation = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        
        val isGranted = fineLocation || coarseLocation
        _isPermissionGranted.value = isGranted
        return isGranted
    }

    fun updateLocationState(isEnabled: Boolean) {
        _isLocationEnabled.value = isEnabled
        if (!isEnabled) {
            _shouldShowLocationDialog.value = true
        }
    }

    fun resetDialogState() {
        _shouldShowLocationDialog.value = false
    }
}


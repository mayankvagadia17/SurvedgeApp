package com.nexova.survedgeapp

import android.app.Application
import org.maplibre.android.MapLibre

class SurvedgeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize MapLibre
        // Using the simple initialization without API key for basic usage
        // If you have a MapLibre API key, use: MapLibre.getInstance(this, "your-api-key", WellKnownTileServer.MAPLIBRE)
        MapLibre.getInstance(this)
    }
}


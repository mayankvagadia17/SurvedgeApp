package com.nexova.survedgeapp

import android.app.Application
import android.os.Build
import android.os.Environment
import org.osmdroid.config.Configuration
import java.io.File

class SurvedgeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize osmdroid with proper configuration for Android 11+
        initializeOsmdroid()
    }
    
    private fun initializeOsmdroid() {
        // Load configuration
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        // Set user agent to avoid getting blocked by tile servers
        Configuration.getInstance().userAgentValue = packageName
        
        // Configure cache directory for Android 11+ (scoped storage)
        val osmdroidBasePath: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+): Use app-specific directory (scoped storage)
            // This works on Android 11+ without requiring WRITE_EXTERNAL_STORAGE permission
            File(applicationContext.getExternalFilesDir(null), "osmdroid")
        } else {
            // Android 9 and below: Use external storage
            File(Environment.getExternalStorageDirectory(), "osmdroid")
        }
        
        // Create directory if it doesn't exist
        if (!osmdroidBasePath.exists()) {
            osmdroidBasePath.mkdirs()
        }
        
        // Set osmdroid base path
        Configuration.getInstance().osmdroidBasePath = osmdroidBasePath
        
        // Set tile cache path
        val tileCachePath = File(osmdroidBasePath, "tiles")
        if (!tileCachePath.exists()) {
            tileCachePath.mkdirs()
        }
        Configuration.getInstance().osmdroidTileCache = tileCachePath
        
        // Set cache size - number of tiles to cache (not MB)
        // Increased to 500 for better tile quality and caching at high zoom levels
        Configuration.getInstance().cacheMapTileCount = 500
        
        // Disable tile download expiration for better quality (tiles stay cached longer)
        Configuration.getInstance().tileDownloadMaxQueueSize = 200
        
        // Enable tile file system cache for better performance
        Configuration.getInstance().tileFileSystemMaxQueueSize = 200
    }
}


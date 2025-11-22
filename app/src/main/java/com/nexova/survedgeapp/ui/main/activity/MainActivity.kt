package com.nexova.survedgeapp.ui.main.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.nexova.survedgeapp.R
import com.nexova.survedgeapp.databinding.ActivityMainBinding
import com.nexova.survedgeapp.ui.base.activity.BaseActivity
import com.nexova.survedgeapp.ui.device.fragment.DeviceFragment
import com.nexova.survedgeapp.ui.mapping.fragment.MappingFragment

class MainActivity : BaseActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupBottomNavigation()
        if (savedInstanceState == null) {
            loadFragment(DeviceFragment())
            binding.bottomNavigationView.selectedItemId = R.id.device
        }
    }

    override fun onLocationServiceEnabled() {
        super.onLocationServiceEnabled()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.device -> {
                    loadFragment(DeviceFragment())
                    true
                }

                R.id.vector -> {
                    loadFragment(MappingFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.flFragment, fragment)
            .commit()

    }
}
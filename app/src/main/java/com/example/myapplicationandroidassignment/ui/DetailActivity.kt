package com.example.myapplicationandroidassignment.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplicationandroidassignment.R

import com.example.myapplicationandroidassignment.network.NetworkClient
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {
    
    private lateinit var networkClient: NetworkClient
    
    private lateinit var tvDeviceName: TextView
    private lateinit var tvDeviceIp: TextView
    private lateinit var tvPublicIp: TextView
    private lateinit var tvCity: TextView
    private lateinit var tvRegion: TextView
    private lateinit var tvCountry: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvOrganization: TextView
    private lateinit var tvTimezone: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutGeoInfo: LinearLayout
    private lateinit var tvError: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }
        networkClient = NetworkClient()
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        
        initViews()
        loadDeviceInfo()
        loadPublicIpInfo()
    }
    
    private fun initViews() {
        tvDeviceName = findViewById(R.id.tvDeviceName)
        tvDeviceIp = findViewById(R.id.tvDeviceIp)
        tvPublicIp = findViewById(R.id.tvPublicIp)
        tvCity = findViewById(R.id.tvCity)
        tvRegion = findViewById(R.id.tvRegion)
        tvCountry = findViewById(R.id.tvCountry)
        tvLocation = findViewById(R.id.tvLocation)
        tvOrganization = findViewById(R.id.tvOrganization)
        tvTimezone = findViewById(R.id.tvTimezone)
        progressBar = findViewById(R.id.progressBar)
        layoutGeoInfo = findViewById(R.id.layoutGeoInfo)
        tvError = findViewById(R.id.tvError)
    }
    
    private fun loadDeviceInfo() {
        val deviceName = intent.getStringExtra("device_name") ?: "Unknown"
        val deviceIp = intent.getStringExtra("device_ip") ?: "Unknown"
        
        tvDeviceName.text = deviceName
        tvDeviceIp.text = deviceIp
    }
    
    private fun loadPublicIpInfo() {
        progressBar.visibility = View.VISIBLE
        layoutGeoInfo.visibility = View.GONE
        tvError.visibility = View.GONE
        
        lifecycleScope.launch {
            // First, get public IP
            val ipResult = networkClient.getPublicIp()
            
            if (ipResult.isSuccess) {
                val publicIp = ipResult.getOrNull()?.ip ?: return@launch
                
                // Then get geo info for the public IP
                val geoResult = networkClient.getIpGeoInfo(publicIp)
                
                if (geoResult.isSuccess) {
                    val geoInfo = geoResult.getOrNull()
                    
                    progressBar.visibility = View.GONE
                    layoutGeoInfo.visibility = View.VISIBLE
                    
                    tvPublicIp.text = geoInfo?.ip ?: "-"
                    tvCity.text = geoInfo?.city ?: "-"
                    tvRegion.text = geoInfo?.region ?: "-"
                    tvCountry.text = geoInfo?.country ?: "-"
                    tvLocation.text = geoInfo?.loc ?: "-"
                    tvOrganization.text = geoInfo?.org ?: "-"
                    tvTimezone.text = geoInfo?.timezone ?: "-"
                } else {
                    showError("Failed to load geo information")
                }
            } else {
                showError("Failed to load public IP")
            }
        }
    }
    
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        layoutGeoInfo.visibility = View.GONE
        tvError.visibility = View.VISIBLE
        tvError.text = message
    }
}
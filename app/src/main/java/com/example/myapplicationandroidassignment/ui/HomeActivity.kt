package com.example.myapplicationandroidassignment.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplicationandroidassignment.R

import com.example.myapplicationandroidassignment.auth.AuthManager
import com.example.myapplicationandroidassignment.data.AppDatabase
import com.example.myapplicationandroidassignment.data.Device
import com.example.myapplicationandroidassignment.data.DeviceRepository
import com.example.myapplicationandroidassignment.service.MDnsDiscoveryService
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    
    private lateinit var authManager: AuthManager
    private lateinit var repository: DeviceRepository
    private lateinit var mdnsService: MDnsDiscoveryService
    private lateinit var adapter: DeviceAdapter
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvScanStatus: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvUserInfo: TextView
    private lateinit var btnScan: MaterialButton
    private lateinit var btnLogout: MaterialButton
    
    private var isScanning = false
    private var multicastLock: WifiManager.MulticastLock? = null
    
    companion object {
        private const val TAG = "HomeActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }
        authManager = AuthManager(this)
        val database = AppDatabase.getDatabase(this)
        repository = DeviceRepository(database.deviceDao())
        mdnsService = MDnsDiscoveryService(this)
        
        initViews()
        setupRecyclerView()
        checkPermissions()
        loadDevicesFromDatabase()
        displayUserInfo()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvScanStatus = findViewById(R.id.tvScanStatus)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvUserInfo = findViewById(R.id.tvUserInfo)
        btnScan = findViewById(R.id.btnScan)
        btnLogout = findViewById(R.id.btnLogout)
        
        btnScan.setOnClickListener {
            if (isScanning) {
                stopScanning()
            } else {
                startScanning()
            }
        }
        
        btnLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun displayUserInfo() {
        val email = authManager.getUserEmail()
        val name = authManager.getUserName()
        tvUserInfo.text = "Logged in as: ${name ?: email ?: "User"}"
    }
    
    private fun setupRecyclerView() {
        adapter = DeviceAdapter { device ->
            navigateToDetail(device)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        lifecycleScope.launch {
            repository.allDevices.collect { devices ->
                adapter.submitList(devices)
                updateEmptyState(devices.isEmpty())
            }
        }
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun loadDevicesFromDatabase() {
        lifecycleScope.launch {
            val devices = repository.getAllDevicesOnce()
            if (devices.isNotEmpty()) {
                // Update device status based on current discovery
                updateDeviceStatuses(devices)
            }
        }
    }
    
    private fun updateDeviceStatuses(devices: List<Device>) {
        lifecycleScope.launch {
            devices.forEach { device ->
                repository.updateDeviceStatus(device.ipAddress, false)
            }
        }
    }
    
    private fun startScanning() {
        isScanning = true
        btnScan.text = "Stop Scanning"
        progressBar.visibility = View.VISIBLE
        tvScanStatus.text = "Scanning for devices..."
        
        acquireMulticastLock()
        
        mdnsService.startDiscovery { device ->
            lifecycleScope.launch {
                repository.insertDevice(device)
                Log.d(TAG, "Device discovered: ${device.deviceName} at ${device.ipAddress}")
            }
        }
        
        // Stop scanning after 30 seconds
        lifecycleScope.launch {
            kotlinx.coroutines.delay(30000)
            if (isScanning) {
                stopScanning()
            }
        }
    }
    
    private fun stopScanning() {
        isScanning = false
        btnScan.text = "Scan Devices"
        progressBar.visibility = View.GONE
        tvScanStatus.text = "Scan complete"
        
        mdnsService.stopDiscovery()
        releaseMulticastLock()
    }
    
    private fun acquireMulticastLock() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        multicastLock = wifiManager.createMulticastLock("mdns_lock").apply {
            setReferenceCounted(true)
            acquire()
        }
    }
    
    private fun releaseMulticastLock() {
        multicastLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        multicastLock = null
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmptyState.visibility = if (isEmpty && !isScanning) View.VISIBLE else View.GONE
    }
    
    private fun navigateToDetail(device: Device) {
        val intent = Intent(this, DetailActivity::class.java).apply {
            putExtra("device_ip", device.ipAddress)
            putExtra("device_name", device.deviceName)
        }
        startActivity(intent)
    }
    
    private fun logout() {
        authManager.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isScanning) {
            stopScanning()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions required for device discovery", Toast.LENGTH_LONG).show()
            }
        }
    }
}
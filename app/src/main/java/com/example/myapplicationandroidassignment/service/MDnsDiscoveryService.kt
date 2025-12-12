package com.example.myapplicationandroidassignment.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.myapplicationandroidassignment.data.Device

class MDnsDiscoveryService(context: Context) {
    
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val discoveredDevices = mutableMapOf<String, Device>()
    private var onDeviceDiscovered: ((Device) -> Unit)? = null
    
    companion object {
        private const val TAG = "MDnsDiscovery"
        private const val SERVICE_TYPE = "_services._dns-sd._udp"
        
        // Common mDNS service types
        private val COMMON_SERVICES = listOf(
            "_http._tcp",
            "_airplay._tcp",
            "_raop._tcp",
            "_googlecast._tcp",
            "_printer._tcp",
            "_scanner._tcp",
            "_ipp._tcp",
            "_smartview._tcp",
            "_androidtvremote._tcp"
        )
    }
    
    fun startDiscovery(onDeviceFound: (Device) -> Unit) {
        onDeviceDiscovered = onDeviceFound
        
        // Discover multiple service types
        COMMON_SERVICES.forEach { serviceType ->
            discoverService(serviceType)
        }
    }
    
    private fun discoverService(serviceType: String) {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery failed for $serviceType: Error code: $errorCode")
            }
            
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed for $serviceType: Error code: $errorCode")
            }
            
            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "Discovery started for $serviceType")
            }
            
            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "Discovery stopped for $serviceType")
            }
            
            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let {
                    Log.d(TAG, "Service found: ${it.serviceName}")
                    resolveService(it)
                }
            }
            
            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let {
                    Log.d(TAG, "Service lost: ${it.serviceName}")
                }
            }
        }
        
        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            discoveryListener = listener
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery for $serviceType", e)
        }
    }
    
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "Resolve failed: $errorCode")
            }
            
            override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let {
                    val ipAddress = it.host?.hostAddress ?: return
                    val deviceName = it.serviceName
                    
                    Log.d(TAG, "Service resolved: $deviceName at $ipAddress")
                    
                    if (!discoveredDevices.containsKey(ipAddress)) {
                        val device = Device(
                            ipAddress = ipAddress,
                            deviceName = deviceName,
                            isOnline = true,
                            lastSeen = System.currentTimeMillis()
                        )
                        discoveredDevices[ipAddress] = device
                        onDeviceDiscovered?.invoke(device)
                    }
                }
            }
        }
        
        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve service", e)
        }
    }
    
    fun stopDiscovery() {
        try {
            discoveryListener?.let {
                nsdManager.stopServiceDiscovery(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop discovery", e)
        }
        discoveryListener = null
        discoveredDevices.clear()
    }
}
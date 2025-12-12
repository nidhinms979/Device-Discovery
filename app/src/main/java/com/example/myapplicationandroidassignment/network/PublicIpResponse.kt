package com.example.myapplicationandroidassignment.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PublicIpResponse(val ip: String)

data class IpGeoInfo(
    val ip: String,
    val city: String?,
    val region: String?,
    val country: String?,
    val loc: String?,
    val org: String?,
    val postal: String?,
    val timezone: String?
)

class NetworkClient {
    
    companion object {
        private const val TAG = "NetworkClient"
        private const val IPIFY_URL = "https://api.ipify.org?format=json"
        private const val IPINFO_URL = "https://ipinfo.io"
    }
    
    suspend fun getPublicIp(): Result<PublicIpResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL(IPIFY_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val ip = json.getString("ip")
                Result.success(PublicIpResponse(ip))
            } else {
                Result.failure(Exception("HTTP error code: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching public IP", e)
            Result.failure(e)
        }
    }
    
    suspend fun getIpGeoInfo(ipAddress: String): Result<IpGeoInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$IPINFO_URL/$ipAddress/geo")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val geoInfo = IpGeoInfo(
                    ip = json.optString("ip", ipAddress),
                    city = json.optString("city", ""),
                    region = json.optString("region", ""),
                    country = json.optString("country", ""),
                    loc = json.optString("loc", ""),
                    org = json.optString("org", ""),
                    postal = json.optString("postal", ""),
                    timezone = json.optString("timezone", "")
                )
                Result.success(geoInfo)
            } else {
                Result.failure(Exception("HTTP error code: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching geo info", e)
            Result.failure(e)
        }
    }
}
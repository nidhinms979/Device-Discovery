package com.example.myapplicationandroidassignment.auth

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.edit

class AuthManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    companion object {
        private const val KEY_TOKEN = "oauth_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"
    }
    
    fun saveToken(token: String, email: String?, name: String?, userId: String?) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_ID, userId)
            apply()
        }
    }
    
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    
    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()
    
    fun logout() {
        prefs.edit { clear() }
    }
    
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    fun shouldForceLogout(): Boolean {
        return isLoggedIn() && !isNetworkAvailable()
    }
}
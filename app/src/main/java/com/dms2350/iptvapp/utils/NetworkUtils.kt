package com.dms2350.iptvapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object NetworkUtils {
    
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    suspend fun testApiConnectivity(): ApiConnectivityResult = withContext(Dispatchers.IO) {
        try {
            println("IPTV: === PROBANDO CONECTIVIDAD API ===")
            
            // Test 1: Conectividad básica a internet
            println("IPTV: Test 1 - Conectividad básica a Google...")
            val googleTest = testConnection("https://www.google.com", 5000)
            println("IPTV: Google test: ${if (googleTest) "OK" else "FAIL"}")
            
            if (!googleTest) {
                return@withContext ApiConnectivityResult.NO_INTERNET
            }
            
            // Test 2: Conectividad al dominio Railway
            println("IPTV: Test 2 - Conectividad a Railway...")
            val railwayTest = testConnection("https://railway.app", 10000)
            println("IPTV: Railway test: ${if (railwayTest) "OK" else "FAIL"}")
            
            // Test 3: Conectividad específica a nuestra API
            println("IPTV: Test 3 - Conectividad a nuestra API...")
            val apiTest = testConnection("https://playtv-production.up.railway.app/", 15000)
            println("IPTV: API test: ${if (apiTest) "OK" else "FAIL"}")
            
            if (!apiTest) {
                return@withContext if (railwayTest) {
                    ApiConnectivityResult.API_DOWN
                } else {
                    ApiConnectivityResult.RAILWAY_BLOCKED
                }
            }
            
            // Test 4: Endpoint específico de canales
            println("IPTV: Test 4 - Endpoint de canales...")
            val channelsTest = testConnection("https://playtv-production.up.railway.app/channels", 15000)
            println("IPTV: Channels endpoint test: ${if (channelsTest) "OK" else "FAIL"}")
            
            return@withContext if (channelsTest) {
                ApiConnectivityResult.ALL_OK
            } else {
                ApiConnectivityResult.ENDPOINT_ERROR
            }
            
        } catch (e: Exception) {
            println("IPTV: Error en test de conectividad: ${e.message}")
            return@withContext ApiConnectivityResult.UNKNOWN_ERROR
        }
    }
    
    private fun testConnection(urlString: String, timeoutMs: Int): Boolean {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            
            val responseCode = connection.responseCode
            println("IPTV: $urlString -> Response: $responseCode")
            
            connection.disconnect()
            responseCode in 200..299 || responseCode == 404 // 404 también indica que el servidor responde
            
        } catch (e: Exception) {
            println("IPTV: Error conectando a $urlString: ${e.message}")
            false
        }
    }
    
    enum class ApiConnectivityResult {
        ALL_OK,
        NO_INTERNET,
        RAILWAY_BLOCKED,
        API_DOWN,
        ENDPOINT_ERROR,
        UNKNOWN_ERROR
    }
    
    fun getConnectivityMessage(result: ApiConnectivityResult): String {
        return when (result) {
            ApiConnectivityResult.ALL_OK -> "Conectividad perfecta"
            ApiConnectivityResult.NO_INTERNET -> "Sin conexión a internet"
            ApiConnectivityResult.RAILWAY_BLOCKED -> "Railway.app bloqueado (firewall/ISP)"
            ApiConnectivityResult.API_DOWN -> "API temporalmente caída"
            ApiConnectivityResult.ENDPOINT_ERROR -> "Problema con endpoint /channels"
            ApiConnectivityResult.UNKNOWN_ERROR -> "Error desconocido"
        }
    }
}
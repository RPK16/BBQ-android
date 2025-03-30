package com.rpk16.bbqcontroller

import okhttp3.FormBody
import okhttp3.OkHttpClient
import android.util.Base64
import okhttp3.Request
import okhttp3.Response
import okhttp3.Callback
import okhttp3.Call
import java.io.IOException

class ControllerRepository {

    // Create an OkHttpClient that allows cleartext traffic for specific IPs
    private fun createOkHttpClientForIp(ip: String): OkHttpClient {
        val clientBuilder = OkHttpClient.Builder()

        // Add an interceptor that checks if the IP uses HTTP (cleartext), and allow it if so
        clientBuilder.addInterceptor { chain ->
            val request = chain.request()

            // Check if the request is using HTTP (cleartext) and if it's the desired IP
            if (request.url.host == ip && request.url.scheme == "http") {
                return@addInterceptor chain.proceed(request)
            } else {
                return@addInterceptor chain.proceed(request)
            }
        }

        return clientBuilder.build()
    }

    fun sendNetSetRequest(ip: String, ssid: String, password: String, callback: (Boolean) -> Unit) {
        val client = createOkHttpClientForIp(ip)

        val encodedSsid = base64UrlEncode(ssid)
        val encodedPassword = base64UrlEncode(password)

        val formBody = FormBody.Builder()
            .add("ssid", encodedSsid)
            .add("pass", encodedPassword)
            .add("user", "")
            .build()

        val request = Request.Builder()
            .url("http://$ip/cgi-bin/netset")
            .post(formBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }

    fun startCooking(ip: String, cookingSession: CookingSession, callback: (String) -> Unit) {
        val client = createOkHttpClientForIp(ip)

        val currentTime = System.currentTimeMillis() / 1000
        val formBody = FormBody.Builder()
            .add("acs", "1") // Active Cook Session
            .add("csid", cookingSession.sessionGuid ?: "") // Using sessionGuid from CookingSession
            .add("tpt", cookingSession.targetPitTemp?.toString() ?: "400") // Target Pit Temp from CookingSession
            .add("sce", cookingSession.endTime?.toString() ?: "") // End time of the cooking session
            .add("p", "1" ?: "1") // Unknown
            .add("tft", cookingSession.targetFoodTemp?.toString() ?: "400") // Target Food Temp from CookingSession
            .add("as", "0" ?: "0") // Target Food Temp from CookingSession
            .add("ct", currentTime.toString() ?: "") // Current time
            .build()

        val request = Request.Builder()
            .url("http://$ip/cgi-bin/cook")
            .post(formBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Failed to start cooking: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback("Cooking started successfully")
                } else {
                    callback("Failed to start cooking: ${response.message}")
                }
            }
        })
    }

    fun stopCooking(ip: String, callback: (String) -> Unit) {
        val client = createOkHttpClientForIp(ip)

        val currentTime = System.currentTimeMillis() / 1000
        val formBody = FormBody.Builder()
            .add("acs", "0") // Active Cook Session
            .add("sce", currentTime.toString() ?: "0") // End time of the cooking session
            .build()

        val request = Request.Builder()
            .url("http://$ip/cgi-bin/cook") // Device IP and endpoint (using HTTP)
            .post(formBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Failed to stop cooking: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback("Cooking stopped successfully")
                } else {
                    callback("Failed to stop cooking: ${response.message}")
                }
            }
        })
    }

    fun getState(ip: String, callback: (State?) -> Unit) {
        val client = createOkHttpClientForIp(ip)

        // Create the GET request to retrieve the state
        val request = Request.Builder()
            .url("http://$ip/cgi-bin/data") // Device state endpoint (using HTTP)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val state = parseState(responseData) // Parse the response into the State object
                    callback(state)
                } else {
                    callback(null)
                }
            }
        })
    }

    private fun parseState(response: String?): State? {
        if (response.isNullOrEmpty()) return null

        // Assuming the response is key=value&key2=value2
        val keyValuePairs = response.split('&')

        val stateMap = mutableMapOf<String, String>()

        for (pair in keyValuePairs) {
            val keyValue = pair.split('=')
            if (keyValue.size == 2) {
                val key = keyValue[0]
                val value = keyValue[1]
                stateMap[key] = value
            }
        }

        return State(
            fanSpeed = stateMap["dc"]?.toInt() ?: 0,
            probePitTemp = stateMap["pt"]?.toInt() ?: 0,
            probe1Temp = stateMap["t1"]?.toInt() ?: 0,
            probe2Temp = stateMap["t2"]?.toInt() ?: 0,
            probe3Temp = stateMap["t3"]?.toInt() ?: 0,
            targetPitTemp = stateMap["tpt"]?.toInt() ?: 0,
            targetFoodTemp = stateMap["tft"]?.toInt() ?: 0,
            activeCookSession = stateMap["acs"]?.toInt() ?: 0,
            timestamp = stateMap["time"]?.toInt() ?: 0,
            cookSessionGuid = stateMap["csid"] // Cook session ID
        )
    }

    private fun base64UrlEncode(input: String): String {
        return Base64.encodeToString(input.toByteArray(), Base64.NO_WRAP)
    }
}

package com.rpk16.bbqcontroller

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class ConnectionMonitor(
    private val ip: String,
    private val intervalMs: Long = 3000L,
    private val onStatusChanged: (Boolean) -> Unit
) {
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var lastStatus: Boolean? = null

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkConnection()
            if (isRunning) {
                handler.postDelayed(this, intervalMs)
            }
        }
    }

    fun start(delay: Long = 1000L) {
        if (!isRunning) {
            isRunning = true
            handler.postDelayed(checkRunnable, delay)
        }
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(checkRunnable)
    }

    private fun checkConnection() {
        val request = Request.Builder()
            .url("http://$ip/cgi-bin/data")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                if (lastStatus != false) {
                    lastStatus = false
                    onStatusChanged(false)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val isSuccess = response.isSuccessful
                if (lastStatus != isSuccess) {
                    lastStatus = isSuccess
                    onStatusChanged(isSuccess)
                }
                response.close()
            }
        })
    }
}
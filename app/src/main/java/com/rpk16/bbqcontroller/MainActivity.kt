package com.rpk16.bbqcontroller

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    lateinit var viewPager: ViewPager2
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var connectionMonitor: ConnectionMonitor
    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        viewPagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Cook"
                1 -> tab.text = "Heat it up"
                2 -> tab.text = "Monitor"
                3 -> tab.text = "Setup Wifi"
            }
        }.attach()
    }

    fun updateConnectionStatus(isConnected: Boolean) {
        val dot = findViewById<View>(R.id.connection_status_dot)
        val drawable = if (isConnected) R.drawable.status_dot_green else R.drawable.status_dot_red
        dot.setBackgroundResource(drawable)
    }

    fun setDeviceName(name: String) {
        findViewById<TextView>(R.id.device_name_text).text = name
    }

    override fun onResume() {
        super.onResume()
        startControllerDiscovery()
    }

    override fun onStop() {
        super.onStop()
        if (::connectionMonitor.isInitialized) {
            connectionMonitor.stop()
        }
    }

    private fun scanForController(subnet: String, onFound: (String?) -> Unit) {
        val range = 1..254
        val client = OkHttpClient()
        var found = false

        for (i in range) {
            val ip = "$subnet$i"
            Thread {
                try {
                    val request = Request.Builder()
                        .url("http://$ip/cgi-bin/data")
                        .build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()
                    response.close()

                    if (!found && response.isSuccessful && body?.contains("pt=") == true) {
                        Log.e("WifiSetup", "BBQ Controller found on $ip ")
                        found = true
                        onFound(ip)
                    }
                } catch (_: Exception) {
                    // Skip failed IPs silently
                }
            }.start()
        }

        Thread {
            Thread.sleep(10000)
            if (!found) {
                onFound(null)
            }
        }.start()
    }

    fun startControllerDiscovery() {
        Log.e("WifiSetup", "Started BBQ controller discovery on home wifi.")
        val subnet = getLocalSubnet()

        scanForController(subnet) { ip ->
            runOnUiThread {
                Log.e("WifiSetup", "Scanning for controller")
                if (ip != null) {
                    setDeviceName(" Device connected @ $ip")
                    sharedViewModel.setControllerIp(ip)
                    connectionMonitor = ConnectionMonitor(ip) { isConnected ->
                        runOnUiThread {
                            updateConnectionStatus(isConnected)
                        }
                    }
                    connectionMonitor.start()
                } else {
                    setDeviceName("Controller not found")
                    updateConnectionStatus(false)

                    // Navigate to Setup Wifi tab
                    viewPager.currentItem = 3

                }
            }
        }
    }

    private fun getLocalSubnet(): String {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        return String.format(
            "%d.%d.%d.",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff
        )
    }
}

package com.rpk16.bbqcontroller

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.Manifest
import okhttp3.*
import java.io.IOException

class SetUpWifiFragment : Fragment(R.layout.fragment_setup_wifi) {

    private lateinit var ssidSpinner: Spinner
    private lateinit var passwordInput: EditText
    private lateinit var connectButton: Button
    private lateinit var scanButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var loadingSpinner: ProgressBar


    private val controllerRepository = ControllerRepository()
    private val client = OkHttpClient()
    private var gatewayIp: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingSpinner = view.findViewById(R.id.loading_spinner)
        ssidSpinner = view.findViewById(R.id.ssid_spinner)
        passwordInput = view.findViewById(R.id.password_input)
        connectButton = view.findViewById(R.id.connect_button)
        statusTextView = view.findViewById(R.id.status_text_view)

        scanButton = view.findViewById(R.id.scan_controller_button)

        scanButton.setOnClickListener {
            Toast.makeText(requireContext(), "Scanning for BBQ controller...", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.startControllerDiscovery()
        }

        val connectivityManager = requireContext().getSystemService(android.net.ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        gatewayIp = getGatewayIpAddress()
        Log.d("GatewayIP", "Gateway: $gatewayIp")  // Should show 192.168.X.X

        connectivityManager.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivityManager.bindProcessToNetwork(network)
                Log.d("NETWORK", "Bound to Wi-Fi network.")

                requireActivity().runOnUiThread {
                    fetchWifiList(gatewayIp)
                }
            }

            override fun onUnavailable() {
                Log.e("NETWORK", "Wi-Fi network unavailable")
            }
        })

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }

        connectButton.setOnClickListener {
            val selectedSsid = ssidSpinner.selectedItem?.toString() ?: ""
            val password = passwordInput.text.toString()

            if (selectedSsid.isEmpty() || password.isEmpty()) {
                statusTextView.text = "Please select a network and enter password."
                return@setOnClickListener
            }

            controllerRepository.sendNetSetRequest(
                ip = gatewayIp,
                ssid = selectedSsid,
                password = password
            ) { success ->
                activity?.runOnUiThread {
                    if (success) {
                        Log.e("WifiSetup", "BBQ controller is now on your home Wi-Fi. Please switch your phone to the same Wi-Fi to continue.")

                        Toast.makeText(requireContext(), "BBQ Controller is now on your home Wi-Fi." +
                                " Please switch your phone to the same Wi-Fi to continue.",
                            Toast.LENGTH_LONG).show()

                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))

                        waitForWifiConnection(selectedSsid) {
                            Log.e("WifiSetup", "Phone is now connected to home wifi.")
                            activity?.runOnUiThread {
                                (activity as? MainActivity)?.startControllerDiscovery()
                                (activity as? MainActivity)?.viewPager?.currentItem = 2
                            }
                        }
                        statusTextView.text = "Wi-Fi setup complete!"
                    } else {
                        statusTextView.text = "Failed to connect. Check credentials or try again."
                    }
                }
            }
        }
    }

    private fun fetchWifiList(ip: String) {
        loadingSpinner.visibility = View.VISIBLE

        val request = Request.Builder()
            .url("http://$ip/cgi-bin/wifi_list")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("WifiList", "Failed to load Wi-Fi list: ${e.localizedMessage}", e)
                activity?.runOnUiThread {
                    loadingSpinner.visibility = View.GONE
                    statusTextView.text = "Failed to load Wi-Fi list"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""

                Log.d("WifiList", "HTTP ${response.code} - $responseBody")

                if (response.isSuccessful) {
                    val ssidList = parseSsidList(responseBody)
                    activity?.runOnUiThread {
                        loadingSpinner.visibility = View.GONE
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            ssidList
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        ssidSpinner.adapter = adapter
                    }
                } else {
                    activity?.runOnUiThread {
                        loadingSpinner.visibility = View.GONE
                        statusTextView.text = "No networks found."
                    }
                }
            }
        })
    }

    private fun parseSsidList(response: String?): List<String> {
        if (response.isNullOrEmpty()) return emptyList()

        return response
            .split("\n")
            .mapNotNull { line ->
                // Example: ssid=Telia-B47882&rssi=-92&...
                Regex("""ssid=([^\r\n&]+)""").find(line)?.groupValues?.get(1)?.trim()
            }
            .filter { it.isNotBlank() && !it.equals("hidden", ignoreCase = true) }
            .distinct()
    }


    @SuppressLint("DefaultLocale")
    private fun getGatewayIpAddress(): String {
        val wifiManager = requireContext()
            .applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val dhcpInfo = wifiManager.dhcpInfo
        val ip = dhcpInfo.gateway
        return String.format(
            "%d.%d.%d.%d",
            (ip and 0xff),
            (ip shr 8 and 0xff),
            (ip shr 16 and 0xff),
            (ip shr 24 and 0xff)
        )
    }

    private fun waitForWifiConnection(targetSsid: String, onConnected: () -> Unit) {
        val wifiManager = requireContext()
            .applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val handler = Handler()
        val checkRunnable = object : Runnable {
            override fun run() {
                val connectedSsid = wifiManager.connectionInfo.ssid?.replace("\"", "")
                Log.d("WiFiSetup", "Currently connected to: $connectedSsid")

                if (connectedSsid == targetSsid) {
                    onConnected()
                } else {
                    handler.postDelayed(this, 3000)
                }
            }
        }
        handler.post(checkRunnable)
    }
}

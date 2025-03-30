package com.rpk16.bbqcontroller

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider

class MonitorFragment : Fragment(R.layout.fragment_monitor) {

    private lateinit var currentPitTempTextView: TextView
    private lateinit var currentFoodTempTextView: TextView
    private lateinit var currentFanSpeedTextView: TextView
    private lateinit var updateStateButton: Button
    private lateinit var endSessionButton: Button

    private val handler = Handler()
    private lateinit var statePollingRunnable: Runnable

    private val controllerRepository = ControllerRepository()
    private lateinit var cookingState: State
    private lateinit var sharedViewModel: SharedViewModel

    private var currentIp: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentPitTempTextView = view.findViewById(R.id.current_pit_temp)
        currentFoodTempTextView = view.findViewById(R.id.current_food_temp)
        currentFanSpeedTextView = view.findViewById(R.id.current_fan_speed)

        updateStateButton = view.findViewById(R.id.session_status_button)
        endSessionButton = view.findViewById(R.id.session_end_button)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        sharedViewModel.controllerIp.observe(viewLifecycleOwner) { ip ->
            currentIp = ip
            if (ip != null) {
                fetchState(ip)
            } else {
                currentPitTempTextView.text = "NaN"
                currentFoodTempTextView.text = "NaN"
                currentFanSpeedTextView.text = "NaN"
            }
        }

        updateStateButton.setOnClickListener {
            Log.e("WifiSetup", "Monitoring at $currentIp")
            currentIp?.let { ip ->
                controllerRepository.getState(ip) { state ->
                    activity?.runOnUiThread {
                        if (state != null) {
                            currentPitTempTextView.text = "${state.probePitTemp}°C"
                            currentFoodTempTextView.text = "${state.probe1Temp}°C"
                            currentFanSpeedTextView.text = "${state.fanSpeed}%"
                        } else {
                            currentPitTempTextView.text = "NaN"
                            currentFoodTempTextView.text = "NaN"
                            currentFanSpeedTextView.text = "NaN"
                        }
                    }
                }
            }
        }

        endSessionButton.setOnClickListener {
            currentIp?.let { ip ->
                controllerRepository.stopCooking(ip) {}
            }
        }

        statePollingRunnable = object : Runnable {
            override fun run() {
                currentIp?.let { ip ->
                    controllerRepository.getState(ip) { state ->
                        if (state != null) {
                            cookingState = state
                            activity?.runOnUiThread {
                                currentPitTempTextView.text = "${state.probePitTemp}°C"
                                currentFoodTempTextView.text = "${state.probe1Temp}°C"
                                currentFanSpeedTextView.text = "${state.fanSpeed}%"
                            }
                        } else {
                            activity?.runOnUiThread {
                                currentPitTempTextView.text = "NaN"
                                currentFoodTempTextView.text = "NaN"
                                currentFanSpeedTextView.text = "NaN"
                            }
                        }
                    }
                }
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(statePollingRunnable)
    }

    private fun fetchState(ip: String) {
        controllerRepository.getState(ip) { state ->
            activity?.runOnUiThread {
                if (state != null) {
                    currentPitTempTextView.text = "${state.probePitTemp}°C"
                    currentFoodTempTextView.text = "${state.probe1Temp}°C"
                    currentFanSpeedTextView.text = "${state.fanSpeed}%"
                } else {
                    currentPitTempTextView.text = "NaN"
                    currentFoodTempTextView.text = "NaN"
                    currentFanSpeedTextView.text = "NaN"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(statePollingRunnable)
    }
}

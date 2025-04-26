package com.rpk16.bbqcontroller

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider

class HeatItUpFragment : Fragment(R.layout.fragment_heat_it_up) {

    private lateinit var currentPitTempTextView: TextView
    private lateinit var startHeatingButton: Button
    private lateinit var fanRuntimeSlider: SeekBar
    private lateinit var fanRuntimeDisplayTextView: TextView
    private lateinit var cookingSession: CookingSession
    private lateinit var cookingState: State

    private val controllerRepository = ControllerRepository()

    private val handler = Handler()
    private lateinit var temperaturePollingRunnable: Runnable
    private lateinit var countdownRunnable: Runnable

    private lateinit var sharedViewModel: SharedViewModel
    private var currentIp: String? = null
    private var isHeating = false

    private var remainingTime: Long = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.controllerIp.observe(viewLifecycleOwner) { ip ->
            currentIp = ip
        }

        cookingSession = CookingSession(
            id = null,
            sessionGuid = null,
            startTime = null,
            endTime = null,
            targetFoodTemp = null,
            targetPitTemp = null,
            state = null,
        )

        currentPitTempTextView = view.findViewById(R.id.current_pit_temp)
        startHeatingButton = view.findViewById(R.id.start_fire_button)
        fanRuntimeSlider = view.findViewById(R.id.fan_runtime_slider)
        fanRuntimeDisplayTextView = view.findViewById(R.id.fan_minutes)

        fanRuntimeSlider.progress = 5

        fanRuntimeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fanRuntimeDisplayTextView.text = String.format("%02d:%02d", progress, 0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        startHeatingButton.setOnClickListener {
            val ip = currentIp
            if (ip.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "BBQ Controller not connected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isHeating) {
                isHeating = true
                startHeatingButton.text = "Stop Heating"

                cookingSession.endTime = (System.currentTimeMillis() / 1000) + (fanRuntimeSlider.progress * 60)
                controllerRepository.startCooking(ip, cookingSession) {}

                startCountdown()
            } else {
                isHeating = false
                startHeatingButton.text = "Start Heating"

                controllerRepository.stopCooking(ip) {}
                handler.removeCallbacks(countdownRunnable)
            }
        }

        temperaturePollingRunnable = object : Runnable {
            override fun run() {
                val ip = currentIp
                if (ip.isNullOrEmpty()) return

                controllerRepository.getState(ip) { state ->
                    if (state != null) {
                        cookingState = state
                        activity?.runOnUiThread {
                            currentPitTempTextView.text = "${state.probePitTemp}°C"
                        }
                    } else {
                        activity?.runOnUiThread {
                            currentPitTempTextView.text = "NaN"
                        }
                    }
                }
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(temperaturePollingRunnable)
    }

    private fun startCountdown() {
        remainingTime = cookingSession.endTime?.minus(System.currentTimeMillis() / 1000) ?: 0
        countdownRunnable = object : Runnable {
            override fun run() {
                if (remainingTime > 0) {
                    val minutes = remainingTime / 60
                    val seconds = remainingTime % 60
                    val formattedTime = String.format("%02d:%02d", minutes, seconds)

                    activity?.runOnUiThread {
                        fanRuntimeDisplayTextView.text = formattedTime
                    }

                    remainingTime -= 1
                    handler.postDelayed(this, 1000)
                } else {
                    activity?.runOnUiThread {
                        isHeating = false
                        startHeatingButton.text = "Start Heating"
                    }
                }
            }
        }
        handler.post(countdownRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(temperaturePollingRunnable)
        if (::countdownRunnable.isInitialized) {
            handler.removeCallbacks(countdownRunnable)
        }
    }
}

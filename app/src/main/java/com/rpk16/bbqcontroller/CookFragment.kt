package com.rpk16.bbqcontroller

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class CookFragment : Fragment(R.layout.fragment_cook) {

    private lateinit var targetPitTempInput: EditText
    private lateinit var targetFoodTempInput: EditText
    private lateinit var startStopCookButton: Button
    private lateinit var currentPitTempView: TextView
    private lateinit var currentFoodTempView: TextView
    private lateinit var sessionInfoLayout: LinearLayout

    private lateinit var cookingSession: CookingSession
    private val controllerRepository = ControllerRepository()
    private lateinit var sharedViewModel: SharedViewModel
    private var currentIp: String? = null
    private var isCooking = false

    private val handler = Handler()
    private lateinit var statePollingRunnable: Runnable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        sharedViewModel.controllerIp.observe(viewLifecycleOwner) { ip ->
            currentIp = ip
        }

        targetPitTempInput = view.findViewById(R.id.target_pit_temp)
        targetFoodTempInput = view.findViewById(R.id.target_food_temp)
        startStopCookButton = view.findViewById(R.id.start_stop_button)
        currentPitTempView = view.findViewById(R.id.current_pit_temp)
        currentFoodTempView = view.findViewById(R.id.current_food_temp)
        sessionInfoLayout = view.findViewById(R.id.session_info_layout)

        sessionInfoLayout.visibility = View.GONE

        cookingSession = CookingSession(
            id = null,
            sessionGuid = null,
            startTime = null,
            endTime = null,
            targetFoodTemp = null,
            targetPitTemp = null,
            state = null,
        )


        startStopCookButton.setOnClickListener {
            val ip = currentIp

            if (ip.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "BBQ Controller not connected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CookNotificationService.isFoodAlmostReadyNotified = false
            CookNotificationService.isFoodReadyNotified = false

            if (!isCooking) {
                cookingSession.endTime = (System.currentTimeMillis() / 1000) + 86400
                cookingSession.targetFoodTemp = targetFoodTempInput.text.toString().toIntOrNull() ?: 160
                cookingSession.targetPitTemp = targetPitTempInput.text.toString().toIntOrNull() ?: 250

                controllerRepository.startCooking(ip, cookingSession) { response ->
                    activity?.runOnUiThread {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
                            return@runOnUiThread
                        }

                        isCooking = true
                        startStopCookButton.text = "Stop Cooking"
                        sessionInfoLayout.visibility = View.VISIBLE
                        startPolling()

                        CookNotificationService.currentPitTemp = cookingSession.targetPitTemp ?: 250
                        CookNotificationService.currentFoodTemp = cookingSession.targetFoodTemp ?: 160

                        val notifyIntent = Intent(requireContext(), CookNotificationService::class.java).apply {
                            action = "START_COOK"
                        }
                        ContextCompat.startForegroundService(requireContext(), notifyIntent)
                    }
                }
            } else {
                controllerRepository.stopCooking(ip) { _ ->
                    activity?.runOnUiThread {
                        isCooking = false
                        startStopCookButton.text = "Start Cooking"
                        sessionInfoLayout.visibility = View.GONE
                        stopPolling()

                        val stopIntent = Intent(requireContext(), CookNotificationService::class.java).apply {
                            action = "STOP_COOK_FROM_NOTIFICATION"
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
                            return@runOnUiThread
                        }

                        requireContext().startService(stopIntent)
                    }
                }
            }
        }

        setupPollingRunnable()

        if (!CookNotificationService.isActive && isCooking) {
            isCooking = false
            startStopCookButton.text = "Start Cooking"
            sessionInfoLayout.visibility = View.GONE
            stopPolling()
        }
    }

    private fun setupPollingRunnable() {
        statePollingRunnable = object : Runnable {
            override fun run() {
                currentIp?.let { ip ->
                    controllerRepository.getState(ip) { state ->
                        state?.let {
                            activity?.runOnUiThread {
                                currentPitTempView.text = "Current Pit Temp: ${it.probePitTemp}°C"
                                currentFoodTempView.text = "Current Food Temp: ${it.probe1Temp}°C"

                                CookNotificationService.updateTemps(
                                    context = requireContext(),
                                    pitTemp = it.probePitTemp ?: 0,
                                    foodTemp = it.probe1Temp ?: 0
                                )
                                if(it.probe1Temp > 0.95 * cookingSession.targetFoodTemp!! &&
                                    !CookNotificationService.isFoodAlmostReadyNotified){
                                    CookNotificationService.sendFoodAlmostReadyNotification(requireContext())
                                    CookNotificationService.isFoodAlmostReadyNotified = true
                                } else if(it.probe1Temp >= cookingSession.targetFoodTemp!! &&
                                    !CookNotificationService.isFoodReadyNotified){
                                    CookNotificationService.sendFoodReadyNotification(requireContext())
                                    CookNotificationService.isFoodReadyNotified = true
                                    startStopCookButton.performClick();
                                }
                            }
                        }
                    }
                }
                handler.postDelayed(this, 3000)
            }
        }
    }

    private fun startPolling() {
        handler.post(statePollingRunnable)
    }

    private fun stopPolling() {
        handler.removeCallbacks(statePollingRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPolling()
    }
}

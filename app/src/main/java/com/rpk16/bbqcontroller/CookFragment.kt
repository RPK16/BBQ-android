package com.rpk16.bbqcontroller

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider

class CookFragment : Fragment(R.layout.fragment_cook) {

    private lateinit var targetPitTemparature: EditText
    private lateinit var startStopCookButton: Button
    private lateinit var targetFoodTemparature: EditText

    private lateinit var cookingSession: CookingSession
    private val controllerRepository = ControllerRepository()
    private lateinit var sharedViewModel: SharedViewModel
    private var currentIp: String? = null

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

        targetPitTemparature = view.findViewById(R.id.target_pit_temp)
        startStopCookButton = view.findViewById(R.id.start_stop_button)
        targetFoodTemparature = view.findViewById(R.id.target_food_temp)

        startStopCookButton.setOnClickListener {
            val ip = currentIp
            if (ip.isNullOrEmpty()) {
                // Show a warning or toast
                Toast.makeText(requireContext(), "BBQ Controller not connected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 24h session duration
            cookingSession.endTime = (System.currentTimeMillis() / 1000) + 86400
            cookingSession.targetFoodTemp = targetPitTemparature.text.toString().toIntOrNull() ?: 0
            cookingSession.targetPitTemp = targetFoodTemparature.text.toString().toIntOrNull() ?: 0

            controllerRepository.startCooking(ip, cookingSession) {}

            (activity as? MainActivity)?.viewPager?.currentItem = 2
        }
    }
}

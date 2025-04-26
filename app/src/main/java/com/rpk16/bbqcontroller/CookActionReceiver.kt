package com.rpk16.bbqcontroller

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CookActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "STOP_COOK_FROM_NOTIFICATION") {
            context.stopService(Intent(context, CookNotificationService::class.java))

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(101)

            val controllerRepository = ControllerRepository()
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val ip = sharedPrefs.getString("controller_ip", null)

            if (!ip.isNullOrEmpty()) {
                controllerRepository.stopCooking(ip) {
                    CookNotificationService.isActive = false
                }
            }
        }
    }
}
package com.rpk16.bbqcontroller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class CookNotificationService : Service() {

    companion object {
        var isActive = false
        var isFoodAlmostReadyNotified = false
        var isFoodReadyNotified = false
        var currentPitTemp = 0
        var currentFoodTemp = 0

        fun updateTemps(context: Context, pitTemp: Int, foodTemp: Int) {
            currentPitTemp = pitTemp
            currentFoodTemp = foodTemp

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val stopIntent = Intent(context, CookActionReceiver::class.java).apply {
                action = "STOP_COOK_FROM_NOTIFICATION"
            }
            val stopPendingIntent = PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val tapPendingIntent = PendingIntent.getActivity(context, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE)

            val notification = NotificationCompat.Builder(context, "cook_channel")
                .setContentTitle("BBQ Controller temperatures")
                .setContentText("Pit: $pitTemp°C     |     Food: $foodTemp°C")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(tapPendingIntent)
                .addAction(R.mipmap.ic_launcher_round, "Stop controller", stopPendingIntent)
                .setOngoing(true)
                .build()

            notificationManager.notify(101, notification)
        }

        fun sendFoodAlmostReadyNotification(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val tapPendingIntent = PendingIntent.getActivity(context, 1, tapIntent, PendingIntent.FLAG_IMMUTABLE)

            val notification = NotificationCompat.Builder(context, "cook_alerts")
                .setContentTitle("Food Almost Ready!")
                .setContentText("Your food is close to the target temperature 🎉")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(tapPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setLights(Color.RED, 3000, 3000)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                .build()

            notificationManager.notify(102, notification) // different ID than foreground one
        }

        fun sendFoodReadyNotification(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val tapPendingIntent = PendingIntent.getActivity(context, 1, tapIntent, PendingIntent.FLAG_IMMUTABLE)

            val notification = NotificationCompat.Builder(context, "cook_alerts")
                .setContentTitle("Food is Ready!")
                .setContentText("Your food is ready!  Turning controller off")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(tapPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setLights(Color.RED, 3000, 3000)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                .build()

            notificationManager.notify(103, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_COOK" -> {
                isActive = true
                createNotificationChannel()
                startForegroundNotification()
            }
            "STOP_COOK", "STOP_COOK_FROM_NOTIFICATION" -> {
                isActive = false
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        createNotificationChannel()

        val stopIntent = Intent(this, CookActionReceiver::class.java).apply {
            action = "STOP_COOK_FROM_NOTIFICATION"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "cook_channel")
            .setContentTitle("BBQ Cooking in Progress")
            .setContentText("Pit: ${currentPitTemp}°C | Food: ${currentFoodTemp}°C")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(tapPendingIntent)
            .addAction(R.mipmap.ic_launcher_round, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()

        startForeground(101, notification)
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Foreground service channel (silent)
            val backgroundChannel = NotificationChannel(
                "cook_channel",
                "Cooking Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Cooking service updates"
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }

            // Alerts channel (vibrates, lights up)
            val alertChannel = NotificationChannel(
                "cook_alerts",
                "Cooking Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important cooking alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                enableLights(true)
                lightColor = Color.RED
                 }

            notificationManager.createNotificationChannel(backgroundChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }


    override fun onBind(intent: Intent?): IBinder? = null
}

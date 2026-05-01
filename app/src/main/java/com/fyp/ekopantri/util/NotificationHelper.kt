package com.fyp.ekopantri.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fyp.ekopantri.R

object NotificationHelper {
    private const val CHANNEL_ID = "expiry_alerts"

    fun showExpiryNotification(context: Context, itemName: String, daysLeft: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Expiry Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val message = if (daysLeft == 0) "$itemName expires today!" else "$itemName will be expired in $daysLeft days!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Pantry Alert ⚠️")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(itemName.hashCode(), notification)
    }
}
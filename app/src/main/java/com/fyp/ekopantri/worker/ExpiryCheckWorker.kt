package com.fyp.ekopantri.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fyp.ekopantri.data.InventoryRepository
import com.fyp.ekopantri.util.NotificationHelper
import java.util.concurrent.TimeUnit

class ExpiryCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val repository = InventoryRepository()

    override suspend fun doWork(): Result {
        return try {
            // 1. Fetch inventory once from Firebase
            val items = repository.getInventoryOnce()
            val currentTime = System.currentTimeMillis()

            items.forEach { item ->
                // 2. Calculate days remaining using TimeUnit for better accuracy
                val diffInMillis = item.expiryDate - currentTime
                val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

                // 3. Check if it matches the reminder setting (1, 3 or 7)
                if (daysRemaining >= 0 && daysRemaining <= item.reminderDays) {
                    NotificationHelper.showExpiryNotification(
                        applicationContext,
                        item.name,
                        daysRemaining
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            // If something goes wrong (no internet, etc.), tell WorkManager to try again later
            Result.retry()
        }
    }
}
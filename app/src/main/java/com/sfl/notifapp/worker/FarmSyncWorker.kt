package com.sfl.notifapp.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sfl.notifapp.data.api.SflApiClient
import com.sfl.notifapp.data.engine.SflTimerEngine
import com.sfl.notifapp.data.storage.PreferencesManager
import com.sfl.notifapp.receiver.AlarmScheduler
import java.util.concurrent.TimeUnit

class FarmSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "FarmSyncWorker"
        const val PERIODIC_WORK_NAME = "sfl_periodic_farm_sync"

        fun schedulePeriodicSync(context: Context, intervalMinutes: Long = 15) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<FarmSyncWorker>(
                intervalMinutes,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
            Log.i(TAG, "Scheduled periodic sync every $intervalMinutes minutes.")
        }
    }

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)
        val farmId = prefs.farmId

        Log.i(TAG, "Starting background sync for Farm #$farmId...")

        val apiClient = SflApiClient()
        val fetchResult = apiClient.fetchFarmJson(farmId)

        return fetchResult.fold(
            onSuccess = { rootJson ->
                try {
                    val timers = SflTimerEngine.extractTimers(rootJson)
                    Log.i(TAG, "Sync complete: extracted ${timers.size} timers.")

                    prefs.saveCachedTimers(timers)
                    prefs.lastSyncTimestamp = System.currentTimeMillis()

                    // Reschedule exact alarms
                    AlarmScheduler.scheduleTimers(applicationContext, timers)
                    Result.success()
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing farm timers: ${e.message}", e)
                    Result.retry()
                }
            },
            onFailure = { error ->
                Log.w(TAG, "Sync failed: ${error.message}")
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        )
    }
}

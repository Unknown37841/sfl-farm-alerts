package com.sfl.notifapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sfl.notifapp.data.storage.PreferencesManager
import com.sfl.notifapp.worker.FarmSyncWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i("BootReceiver", "Received boot/package action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val prefs = PreferencesManager(context)
            val cachedTimers = prefs.getCachedTimers()

            if (cachedTimers.isNotEmpty()) {
                Log.i("BootReceiver", "Restoring ${cachedTimers.size} cached alarms after reboot.")
                AlarmScheduler.scheduleTimers(context, cachedTimers)
            }

            // Also trigger background network sync when device is ready
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWork = OneTimeWorkRequestBuilder<FarmSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncWork)
        }
    }
}

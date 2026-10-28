package com.sfl.notifapp.util

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object BackgroundPermissionHelper {

    /**
     * Checks if battery optimization is disabled (unrestricted background)
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
        return true
    }

    /**
     * Requests system exemption from battery optimization
     */
    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(context, "Could not open battery settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Checks if exact alarm permission is granted (Android 12+)
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            return alarmManager?.canScheduleExactAlarms() ?: true
        }
        return true
    }

    /**
     * Opens Exact Alarm settings for Android 12+
     */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                openAppDetailsSettings(context)
            }
        } else {
            Toast.makeText(context, "Exact alarms are natively supported on your Android version", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens standard App Info settings (where user can configure Autostart & Battery on Xiaomi/Samsung/etc.)
     */
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open app info", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Tries vendor-specific autostart settings (Xiaomi, Oppo, Vivo, Huawei) or falls back to App Info
     */
    fun openAutostartSettings(context: Context) {
        val autostartIntents = listOf(
            // Xiaomi MIUI / HyperOS
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.securityscan.MainActivity")),
            // Huawei EMUI
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
            // Oppo ColorOS
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            // Vivo
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            // Samsung Device Care
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"))
        )

        for (intent in autostartIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (ignored: Exception) {
            }
        }

        // Fallback: standard app details page
        openAppDetailsSettings(context)
    }

    /**
     * Displays a clean guide dialog explaining how to keep alerts running when the app is swiped away
     */
    fun showBackgroundGuideDialog(context: Context) {
        val message = """
            برای اینکه حتی وقتی برنامه را از لیست برنامه‌های باز گوشی بستید، نوتیفیکیشن‌ها دقیق و سر وقت بیایند:
            
            📱 شیائومی (Xiaomi / Poco):
            ۱. گزینه «شروع خودکار (Autostart)» را در صفحه برنامه روشن کنید.
            ۲. در بخش «صرفه‌جویی باتری (Battery Saver)» گزینه «بدون محدودیت (No restrictions)» را انتخاب کنید.
            
            📱 سامسونگ (Samsung):
            ۱. در بخش باتری برنامه، وضعیت را روی «بدون محدودیت (Unrestricted)» قرار دهید.
            ۲. مطمئن شوید برنامه در لیست «Sleeping apps» نباشد.
            
            ⚡ نکته: سیستم تایمرهای سخت‌افزاری برنامه حتی هنگام خاموش بودن صفحه و قطعی اینترنت هم در ساعت دقیق زنگ می‌زنند!
        """.trimIndent()

        MaterialAlertDialogBuilder(context)
            .setTitle("راهنمای فعالیت در پس‌زمینه")
            .setMessage(message)
            .setPositiveButton("تنظیمات برنامه (Autostart)") { _, _ ->
                openAutostartSettings(context)
            }
            .setNeutralButton("بهینه‌سازی باتری") { _, _ ->
                requestIgnoreBatteryOptimization(context)
            }
            .setNegativeButton("متوجه شدم", null)
            .show()
    }
}

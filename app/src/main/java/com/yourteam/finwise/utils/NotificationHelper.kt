package com.yourteam.finwise.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yourteam.finwise.R
import com.yourteam.finwise.receivers.DebtNotificationReceiver
import com.yourteam.finwise.ui.main.MainActivity
import java.util.Calendar

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "finwise_debt_channel"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_NAME = "Debt Payment Reminders"
        const val CHANNEL_DESCRIPTION = "Notifications for debt payment dates and financial reminders"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Create notification channel for Android 8.0 (Oreo) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(null, null) // No sound by default
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun schedulePaymentNotification(debtId: Long, personName: String, amount: Double, payDate: Long) {
        try {
            // Check if notification permission is granted (Android 13+)
            if (!hasNotificationPermission()) {
                LogUtils.d("NotificationHelper", "Notification permission not granted, skipping schedule")
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DebtNotificationReceiver::class.java).apply {
                putExtra("debt_id", debtId)
                putExtra("person_name", personName)
                putExtra("amount", amount)
                putExtra("notification_type", "payment_reminder")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                debtId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set notification for 7:00 AM on payDate
            val calendar = Calendar.getInstance().apply {
                timeInMillis = payDate
                set(Calendar.HOUR_OF_DAY, 7)  // 7 AM
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If the time has already passed today, schedule for next day
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            LogUtils.d("NotificationHelper",
                "Scheduling notification for debt $debtId at ${calendar.time}")

            // Use AlarmManager to schedule exact time
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

        } catch (e: SecurityException) {
            LogUtils.e("NotificationHelper", "Security exception scheduling notification", e)
        } catch (e: Exception) {
            LogUtils.e("NotificationHelper", "Error scheduling notification", e)
        }
    }

    fun showPaymentNotification(debtId: Long, personName: String, amount: Double) {
        // Check notification permission for Android 13+
        if (!hasNotificationPermission()) {
            LogUtils.d("NotificationHelper", "Cannot show notification - permission denied")
            return
        }

        try {
            // Create intent to open app when notification is tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("open_debts", true) // Optional: flag to open debts tab
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build the notification
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("💰 Debt Payment Due Today!")
                .setContentText("Payment of $${String.format("%.2f", amount)} to $personName is due today")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            // Show the notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(debtId.toInt(), notification)

            LogUtils.d("NotificationHelper", "Showed payment notification for debt $debtId")

        } catch (e: Exception) {
            LogUtils.e("NotificationHelper", "Error showing payment notification", e)
        }
    }

    fun cancelPaymentNotification(debtId: Long) {
        try {
            // Cancel alarm
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DebtNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                debtId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)

            // Cancel any displayed notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(debtId.toInt())

            LogUtils.d("NotificationHelper", "Cancelled notification for debt ID: $debtId")

        } catch (e: Exception) {
            LogUtils.e("NotificationHelper", "Error cancelling notification", e)
        }
    }

    fun showGeneralNotification(title: String, message: String, notificationId: Int = NOTIFICATION_ID) {
        if (!hasNotificationPermission()) {
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)

        } catch (e: Exception) {
            LogUtils.e("NotificationHelper", "Error showing general notification", e)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        // For Android 13 (Tiramisu) and above, check runtime permission
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below Android 13, notifications don't require runtime permission
            true
        }
    }
}

// Simple logging utility
object LogUtils {
    fun d(tag: String, message: String) {
        android.util.Log.d(tag, message)
    }

    fun e(tag: String, message: String, e: Exception? = null) {
        android.util.Log.e(tag, message, e)
    }
}
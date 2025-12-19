package com.yourteam.finwise.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yourteam.finwise.utils.NotificationHelper

class DebtNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val debtId = intent.getLongExtra("debt_id", -1)
        val personName = intent.getStringExtra("person_name") ?: "Unknown"
        val amount = intent.getDoubleExtra("amount", 0.0)
        val notificationType = intent.getStringExtra("notification_type")

        if (notificationType == "payment_reminder") {
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showPaymentNotification(debtId, personName, amount)
        }
    }
}
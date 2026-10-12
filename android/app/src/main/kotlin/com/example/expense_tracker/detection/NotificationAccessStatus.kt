package com.example.expense_tracker.detection

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object NotificationAccessStatus {
    fun isEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val serviceName = ComponentName(
            context,
            PaymentNotificationListenerService::class.java,
        ).flattenToString()

        return enabledListeners.split(':').any { it.equals(serviceName, ignoreCase = true) }
    }
}

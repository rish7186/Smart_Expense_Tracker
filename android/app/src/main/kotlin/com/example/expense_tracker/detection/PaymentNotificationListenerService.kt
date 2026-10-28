package com.example.expense_tracker.detection

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.database.sqlite.SQLiteException
import android.util.Log
import com.example.expense_tracker.ExpenseTrackerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PaymentNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processingLock = Mutex()

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications.orEmpty().forEach(::processNotification)
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        processNotification(statusBarNotification)
    }

    private fun processNotification(statusBarNotification: StatusBarNotification) {
        val notification = statusBarNotification.notification
        val extras = notification.extras ?: return
        val text = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.joinToString(" ") { it.toString() },
        ).distinct().joinToString(" ").takeIf { it.isNotBlank() }
        val parsed = PaymentNotificationParser.parseAll(
            PaymentNotification(
                packageName = statusBarNotification.packageName,
                notificationKey = statusBarNotification.key,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                text = text,
                postedAtEpochMillis = statusBarNotification.postTime,
            ),
        )
        if (parsed.isEmpty()) return

        serviceScope.launch {
            processingLock.withLock {
                try {
                    val application = application as ExpenseTrackerApplication
                    parsed.forEach { transaction ->
                        application.repository.addDetectedTransactionIfNew(transaction)
                    }
                } catch (error: IllegalArgumentException) {
                    Log.e(TAG, "Rejected an invalid payment notification.", error)
                } catch (error: SQLiteException) {
                    Log.e(TAG, "Could not save a detected payment notification.", error)
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "PaymentNotification"
    }
}

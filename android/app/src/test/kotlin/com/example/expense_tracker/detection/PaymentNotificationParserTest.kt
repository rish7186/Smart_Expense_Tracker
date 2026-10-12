package com.example.expense_tracker.detection

import com.example.expense_tracker.data.TransactionSource
import com.example.expense_tracker.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentNotificationParserTest {
    @Test
    fun parsesCompletedDebitNotificationFromSupportedApp() {
        val transaction = PaymentNotificationParser.parse(
            notification(
                text = "You paid Rs. 125.50 to Swiggy using UPI",
                postedAt = 1234L,
            ),
        )

        assertEquals(12_550L, transaction?.amountMinor)
        assertEquals(TransactionType.EXPENSE, transaction?.type)
        assertEquals("Food and Dining", transaction?.categoryId)
        assertEquals(TransactionSource.NOTIFICATION, transaction?.source)
        assertFalse(transaction?.isReviewed ?: true)
    }

    @Test
    fun parsesCreditAndRefundNotifications() {
        val credit = PaymentNotificationParser.parse(
            notification(text = "You received ₹2,000 from Alex"),
        )
        val refund = PaymentNotificationParser.parse(
            notification(text = "Refund of INR 250.00 received for order"),
        )

        assertEquals(TransactionType.INCOME, credit?.type)
        assertEquals(200_000L, credit?.amountMinor)
        assertEquals(TransactionType.REFUND, refund?.type)
    }

    @Test
    fun parsesBankDebitSmsNotificationWithoutManualEntry() {
        val transaction = PaymentNotificationParser.parse(
            notification(
                packageName = PaymentNotificationParser.GOOGLE_MESSAGES_PACKAGE,
                title = "VM-HDFCBK",
                text = "Rs. 1,250.00 debited from A/c XX1234 to Swiggy. UPI Ref No: 123456789012",
                postedAt = 1234L,
            ),
        )

        assertEquals(125_000L, transaction?.amountMinor)
        assertEquals(TransactionType.EXPENSE, transaction?.type)
        assertEquals("Food and Dining", transaction?.categoryId)
        assertEquals("Swiggy", transaction?.merchant)
        assertEquals("notification:com.google.android.apps.messaging:123456789012", transaction?.referenceId)
    }

    @Test
    fun parsesBankCreditSmsAndIgnoresNonTransactionSms() {
        val credit = PaymentNotificationParser.parse(
            notification(
                packageName = PaymentNotificationParser.SAMSUNG_MESSAGES_PACKAGE,
                title = "VM-ICICIB",
                text = "INR 25,000 credited to your account XX6789",
            ),
        )

        assertEquals(TransactionType.INCOME, credit?.type)
        assertEquals(2_500_000L, credit?.amountMinor)
        assertNull(
            PaymentNotificationParser.parse(
                notification(
                    packageName = PaymentNotificationParser.GOOGLE_MESSAGES_PACKAGE,
                    text = "Your account balance is Rs. 25,000",
                ),
            ),
        )
        assertNull(
            PaymentNotificationParser.parse(
                notification(
                    packageName = PaymentNotificationParser.GOOGLE_MESSAGES_PACKAGE,
                    text = "Your OTP for transaction is 123456",
                ),
            ),
        )
    }

    @Test
    fun ignoresUnsupportedAppsFailedAlertsAndBalanceOnlyNotices() {
        assertNull(
            PaymentNotificationParser.parse(
                notification(packageName = "com.example.other", text = "Paid ₹500"),
            ),
        )
        assertNull(
            PaymentNotificationParser.parse(
                notification(text = "Payment failed: you paid ₹500 to Alex"),
            ),
        )
        assertNull(
            PaymentNotificationParser.parse(
                notification(text = "Account balance is ₹500"),
            ),
        )
    }

    @Test
    fun usesStableFingerprintForDuplicateNotificationDelivery() {
        val original = notification(text = "Paid ₹500 to Alex", postedAt = 100L)
        val first = PaymentNotificationParser.parse(original)
        val repeated = PaymentNotificationParser.parse(original)
        val laterPayment = PaymentNotificationParser.parse(original.copy(postedAtEpochMillis = 101L))

        assertEquals(first?.referenceId, repeated?.referenceId)
        assertNotEquals(first?.referenceId, laterPayment?.referenceId)
        assertTrue(first?.referenceId?.startsWith("notification:") == true)
    }

    private fun notification(
        packageName: String = PaymentNotificationParser.GOOGLE_PAY_PACKAGE,
        title: String? = null,
        text: String,
        postedAt: Long = 1_000L,
    ) = PaymentNotification(
        packageName = packageName,
        notificationKey = "test-key",
        title = title,
        text = text,
        postedAtEpochMillis = postedAt,
    )
}

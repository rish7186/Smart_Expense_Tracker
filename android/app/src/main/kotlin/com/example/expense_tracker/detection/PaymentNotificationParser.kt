package com.example.expense_tracker.detection

import com.example.expense_tracker.data.ExpenseCategories
import com.example.expense_tracker.data.PaymentMethod
import com.example.expense_tracker.data.TransactionEntity
import com.example.expense_tracker.data.TransactionSource
import com.example.expense_tracker.data.TransactionStatus
import com.example.expense_tracker.data.TransactionType
import com.example.expense_tracker.data.Money
import java.security.MessageDigest
import java.util.Locale

data class PaymentNotification(
    val packageName: String,
    val notificationKey: String,
    val title: String?,
    val text: String?,
    val postedAtEpochMillis: Long,
)

object PaymentNotificationParser {
    const val GOOGLE_PAY_PACKAGE = "com.google.android.apps.nbu.paisa.user"
    const val PHONEPE_PACKAGE = "com.phonepe.app"
    const val PAYTM_PACKAGE = "net.one97.paytm"
    const val GOOGLE_MESSAGES_PACKAGE = "com.google.android.apps.messaging"
    const val ANDROID_MESSAGES_PACKAGE = "com.android.messaging"
    const val AOSP_MESSAGES_PACKAGE = "com.android.mms"
    const val SAMSUNG_MESSAGES_PACKAGE = "com.samsung.android.messaging"

    private val paymentAppPackages = setOf(
        GOOGLE_PAY_PACKAGE,
        PHONEPE_PACKAGE,
        PAYTM_PACKAGE,
    )
    private val messagingAppPackages = setOf(
        GOOGLE_MESSAGES_PACKAGE,
        ANDROID_MESSAGES_PACKAGE,
        AOSP_MESSAGES_PACKAGE,
        SAMSUNG_MESSAGES_PACKAGE,
    )
    private val amountPattern = Regex(
        """(?:₹|Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:₹|INR|Rs\.?)""",
        RegexOption.IGNORE_CASE,
    )
    private val referencePattern = Regex(
        """\b(?:utr|rrn|ref(?:erence)?(?:\s*(?:no|number|id))?|txn(?:\s*id)?|transaction\s*id)\s*[:#-]?\s*([A-Za-z0-9-]{6,32})\b""",
        RegexOption.IGNORE_CASE,
    )
    private val stopMerchantWords = setOf(
        "via",
        "using",
        "upi",
        "ref",
        "reference",
        "on",
        "for",
        "to",
        "from",
        "account",
        "acct",
        "a/c",
        "ac",
    )
    private val merchantPattern = Regex(
        """\b(?:to|from|at|towards)\s+([A-Za-z0-9][A-Za-z0-9&().'-]*(?:\s+[A-Za-z0-9&().'-]+){0,3})""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(notification: PaymentNotification): TransactionEntity? {
        val isPaymentApp = notification.packageName in paymentAppPackages
        val isMessagingApp = notification.packageName in messagingAppPackages
        if (!isPaymentApp && !isMessagingApp) return null
        val content = listOfNotNull(notification.title, notification.text)
            .joinToString(" ")
            .trim()
        if (content.isBlank()) return null

        val normalizedContent = content.lowercase(Locale.ROOT)
        if (listOf("failed", "declined", "pending", "processing", "cancelled", "canceled")
                .any(normalizedContent::contains)
        ) {
            return null
        }

        val amountMatch = amountPattern.find(content) ?: return null
        val amountText = amountMatch.groups[1]?.value ?: amountMatch.groups[2]?.value
            ?: return null
        val amountMinor = Money.parseMinorUnits(amountText) ?: return null

        val type = when {
            listOf("refund", "cashback", "reversed").any(normalizedContent::contains) ->
                TransactionType.REFUND
            listOf("received", "credited", "deposited").any(normalizedContent::contains) ->
                TransactionType.INCOME
            listOf(
                "paid",
                "sent",
                "debited",
                "debit",
                "withdrawn",
                "deducted",
                "spent",
                "payment to",
            ).any(normalizedContent::contains) ->
                TransactionType.EXPENSE
            else -> return null
        }

        val merchant = extractMerchant(content)
        val reference = referencePattern.find(content)?.groupValues?.get(1)
        return TransactionEntity(
            amountMinor = amountMinor,
            type = type,
            categoryId = categoryFor(type, normalizedContent),
            merchant = merchant,
            description = when (type) {
                TransactionType.INCOME -> merchant?.let { "Payment received from $it" }
                    ?: "Payment received"
                TransactionType.REFUND -> merchant?.let { "Refund from $it" } ?: "Payment refund"
                else -> merchant?.let { "Payment to $it" } ?: "UPI payment"
            },
            transactionDateEpochMillis = notification.postedAtEpochMillis,
            paymentMethod = if (normalizedContent.contains("upi")) {
                PaymentMethod.UPI
            } else {
                PaymentMethod.BANK_TRANSFER
            },
            source = TransactionSource.NOTIFICATION,
            referenceId = reference?.let { "notification:${notification.packageName}:$it" }
                ?: fingerprint(notification),
            status = TransactionStatus.COMPLETED,
            isReviewed = false,
        )
    }

    private fun extractMerchant(content: String): String? {
        return merchantPattern.findAll(content)
            .map { match ->
                match.groupValues[1]
                    .split(Regex("\\s+"))
                    .takeWhile { it.lowercase(Locale.ROOT) !in stopMerchantWords }
                    .joinToString(" ")
                    .trim()
                    .trimEnd('.', ',', ':', ';')
            }
            .firstOrNull { it.length in 2..60 }
    }

    private fun categoryFor(type: TransactionType, normalizedContent: String): String {
        if (type == TransactionType.REFUND) return "Refunds"
        if (type == TransactionType.INCOME) return "Other Income"
        return when {
            listOf("swiggy", "zomato", "restaurant", "food").any(normalizedContent::contains) ->
                "Food and Dining"
            listOf("uber", "ola", "metro", "bus", "taxi").any(normalizedContent::contains) ->
                "Transportation"
            listOf("amazon", "flipkart", "myntra").any(normalizedContent::contains) ->
                "Shopping"
            else -> ExpenseCategories.expenses.last()
        }
    }

    private fun fingerprint(notification: PaymentNotification): String {
        val input = listOf(
            notification.packageName,
            notification.notificationKey,
            notification.postedAtEpochMillis.toString(),
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "notification:$digest"
    }
}

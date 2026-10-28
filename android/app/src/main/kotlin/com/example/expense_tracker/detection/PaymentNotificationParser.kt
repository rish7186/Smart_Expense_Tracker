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
    private data class Statement(val text: String, val startIndex: Int)

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
    private val statementBoundaryPattern = Regex(
        """[;\n!?]+|(?<=[.!?])\s+(?=[A-Z])""",
    )
    private val excludedAmountContextPattern = Regex(
        """\b(?:account balance|available balance|closing balance|minimum due|total due|credit limit|loan limit)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val mandateContextPattern = Regex(
        """\b(?:auto[\s-]?pay|e[- ]?mandate|mandate|standing instructions?)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val mandateSetupPattern = Regex(
        """\b(?:created|registered|activated|set\s+up|configured)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val unsuccessfulTransactionPattern = Regex(
        """\b(?:failed|declined|pending|processing|cancelled|canceled)\b""",
    )
    private val typeSignals = listOf(
        TransactionType.REFUND to Regex(
            """\b(?:refund|cashback|reversal|reversed)\b""",
            RegexOption.IGNORE_CASE,
        ),
        TransactionType.EXPENSE to Regex(
            """\b(?:debited|debit|paid|sent|withdrawn|deducted|spent|purchase|charged)\b|payment\s+to\b""",
            RegexOption.IGNORE_CASE,
        ),
        TransactionType.INCOME to Regex(
            """\b(?:credited|credit|received|deposited)\b""",
            RegexOption.IGNORE_CASE,
        ),
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
        return parseAll(notification).firstOrNull()
    }

    fun parseAll(notification: PaymentNotification): List<TransactionEntity> {
        val isPaymentApp = notification.packageName in paymentAppPackages
        val isMessagingApp = notification.packageName in messagingAppPackages
        if (!isPaymentApp && !isMessagingApp) return emptyList()
        val content = listOfNotNull(notification.title, notification.text)
            .joinToString(" ")
            .trim()
        if (content.isBlank()) return emptyList()
        val normalizedContent = content.lowercase(Locale.ROOT)

        return splitStatements(content)
            .asSequence()
            .flatMap { statement ->
                val normalizedStatement = statement.text.lowercase(Locale.ROOT)
                if (unsuccessfulTransactionPattern.containsMatchIn(normalizedStatement)) {
                    return@flatMap emptySequence()
                }

                amountPattern.findAll(statement.text).mapNotNull { amountMatch ->
                    val amountText = amountMatch.groups[1]?.value
                        ?: amountMatch.groups[2]?.value
                        ?: return@mapNotNull null
                    val amountMinor = Money.parseMinorUnits(amountText)
                        ?: return@mapNotNull null
                    val contextBeforeAmount = statement.text.substring(
                        0,
                        amountMatch.range.first,
                    ).takeLast(120)
                    if (excludedAmountContextPattern.containsMatchIn(contextBeforeAmount) ||
                        (
                            mandateContextPattern.containsMatchIn(contextBeforeAmount) &&
                                mandateSetupPattern.containsMatchIn(contextBeforeAmount)
                            )
                    ) {
                        return@mapNotNull null
                    }

                    val type = closestTypeSignal(statement.text, amountMatch.range)
                        ?: return@mapNotNull null
                    val merchant = extractMerchant(statement.text)
                    val reference = referencePattern.find(statement.text)?.groupValues?.get(1)
                        ?: nearestFollowingReference(
                            content,
                            statement.startIndex + amountMatch.range.last,
                        )
                    TransactionEntity(
                        amountMinor = amountMinor,
                        type = type,
                        categoryId = categoryFor(type, normalizedStatement),
                        merchant = merchant,
                        description = when (type) {
                            TransactionType.INCOME ->
                                merchant?.let { "Payment received from $it" } ?: "Payment received"
                            TransactionType.REFUND ->
                                merchant?.let { "Refund from $it" } ?: "Payment refund"
                            else -> merchant?.let { "Payment to $it" } ?: "UPI payment"
                        },
                        transactionDateEpochMillis = notification.postedAtEpochMillis,
                        paymentMethod = if (normalizedContent.contains("upi")) {
                            PaymentMethod.UPI
                        } else {
                            PaymentMethod.BANK_TRANSFER
                        },
                        source = TransactionSource.NOTIFICATION,
                        referenceId = reference?.let {
                            "notification:${notification.packageName}:$it"
                        } ?: fingerprint(notification, statement.text),
                        status = TransactionStatus.COMPLETED,
                        isReviewed = false,
                    )
                }
            }
            .distinctBy { it.referenceId }
            .toList()
    }

    private fun splitStatements(content: String): List<Statement> {
        val statements = mutableListOf<Statement>()
        var statementStart = 0

        fun addStatement(endIndex: Int) {
            val rawStatement = content.substring(statementStart, endIndex)
            val firstContentIndex = rawStatement.indexOfFirst { !it.isWhitespace() }
            if (firstContentIndex >= 0) {
                val text = rawStatement.trim()
                statements += Statement(text, statementStart + firstContentIndex)
            }
        }

        statementBoundaryPattern.findAll(content).forEach { boundary ->
            addStatement(boundary.range.first)
            statementStart = boundary.range.last + 1
        }
        addStatement(content.length)
        return statements
    }

    private fun nearestFollowingReference(content: String, amountEndIndex: Int): String? {
        val reference = referencePattern.findAll(content)
            .filter { it.range.first > amountEndIndex }
            .minByOrNull { it.range.first - amountEndIndex }
            ?: return null
        return reference.groupValues[1]
            .takeIf { reference.range.first - amountEndIndex <= 160 }
    }

    private fun closestTypeSignal(
        statement: String,
        amountRange: IntRange,
    ): TransactionType? {
        val signals = typeSignals.asSequence()
            .flatMap { (type, pattern) ->
                pattern.findAll(statement).map { match ->
                    val distance = when {
                        match.range.last < amountRange.first ->
                            amountRange.first - match.range.last - 1
                        match.range.first > amountRange.last ->
                            match.range.first - amountRange.last - 1
                        else -> 0
                    }
                    type to distance
                }
            }
            .filter { (_, distance) -> distance <= 80 }
            .toList()
        val nearestSignal = signals.minByOrNull { (_, distance) -> distance }
            ?: return null
        val nearbyRefund = signals
            .filter { (type, distance) ->
                type == TransactionType.REFUND &&
                    distance <= nearestSignal.second + 12
            }
            .minByOrNull { (_, distance) -> distance }
        return nearbyRefund?.first ?: nearestSignal.first
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

    private fun fingerprint(notification: PaymentNotification, statement: String): String {
        val input = listOf(
            notification.packageName,
            notification.notificationKey,
            notification.postedAtEpochMillis.toString(),
            statement.lowercase(Locale.ROOT).trim(),
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "notification:$digest"
    }
}

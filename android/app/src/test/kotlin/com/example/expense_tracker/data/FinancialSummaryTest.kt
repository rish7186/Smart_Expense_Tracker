package com.example.expense_tracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialSummaryTest {
    @Test
    fun refundsReduceCashOutflowAndTransfersDoNotCountAsIncome() {
        val transactions = listOf(
            transaction(10_000, TransactionType.INCOME),
            transaction(4_000, TransactionType.EXPENSE),
            transaction(1_000, TransactionType.REFUND),
            transaction(50_000, TransactionType.TRANSFER),
        )

        val summary = calculateFinancialSummary(transactions)

        assertEquals(10_000L, summary.incomeMinor)
        assertEquals(4_000L, summary.expenseMinor)
        assertEquals(1_000L, summary.refundMinor)
        assertEquals(7_000L, summary.netCashFlowMinor)
    }

    @Test
    fun excludedPendingAndFailedTransactionsAreNotCounted() {
        val transactions = listOf(
            transaction(1_000, TransactionType.EXPENSE, isExcluded = true),
            transaction(2_000, TransactionType.EXPENSE, status = TransactionStatus.PENDING),
            transaction(3_000, TransactionType.EXPENSE, status = TransactionStatus.FAILED),
            transaction(4_000, TransactionType.EXPENSE),
        )

        assertEquals(-4_000L, calculateFinancialSummary(transactions).netCashFlowMinor)
    }

    private fun transaction(
        amountMinor: Long,
        type: TransactionType,
        status: TransactionStatus = TransactionStatus.COMPLETED,
        isExcluded: Boolean = false,
    ) = TransactionEntity(
        amountMinor = amountMinor,
        type = type,
        categoryId = "Other",
        merchant = null,
        description = "Synthetic test transaction",
        transactionDateEpochMillis = 1,
        paymentMethod = PaymentMethod.OTHER,
        source = TransactionSource.MANUAL,
        status = status,
        isExcluded = isExcluded,
    )
}

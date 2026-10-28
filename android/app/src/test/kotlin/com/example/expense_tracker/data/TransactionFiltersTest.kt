package com.example.expense_tracker.data

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionFiltersTest {
    private val today = LocalDate.of(2026, 10, 12)
    private val thisWeekExpense = transaction(
        merchant = "Market Basket",
        category = "Groceries",
        type = TransactionType.EXPENSE,
        date = today,
    )
    private val lastWeekExpense = transaction(
        merchant = "Market Basket",
        category = "Groceries",
        type = TransactionType.EXPENSE,
        date = today.minusDays(8),
    )
    private val thisWeekIncome = transaction(
        merchant = "Salary",
        category = "Salary",
        type = TransactionType.INCOME,
        date = today.minusDays(1),
    )

    @Test
    fun combinesSearchTypeDateAndCategoryFilters() {
        val filter = TransactionFilter(
            query = "market",
            type = TransactionTypeFilter.EXPENSE,
            date = TransactionDateFilter.THIS_WEEK,
            category = "Groceries",
        )

        assertTrue(filter.matches(thisWeekExpense, today))
        assertFalse(filter.matches(lastWeekExpense, today))
        assertFalse(filter.matches(thisWeekIncome, today))
    }

    @Test
    fun refundFilterIncludesReversalsAndSearchesDescription() {
        val reversal = transaction(
            merchant = null,
            category = "Refunds",
            description = "Card payment reversed",
            type = TransactionType.REVERSAL,
            date = today,
        )
        val filter = TransactionFilter(
            query = "reversed",
            type = TransactionTypeFilter.REFUND,
            date = TransactionDateFilter.TODAY,
            category = "Refunds",
        )

        assertTrue(filter.matches(reversal, today))
        assertFalse(filter.matches(thisWeekExpense, today))
    }

    private fun transaction(
        merchant: String?,
        category: String,
        type: TransactionType,
        date: LocalDate,
        description: String = "Synthetic transaction",
    ) = TransactionEntity(
        amountMinor = 1_250,
        type = type,
        categoryId = category,
        merchant = merchant,
        description = description,
        transactionDateEpochMillis = date.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli(),
        paymentMethod = PaymentMethod.UPI,
        source = TransactionSource.MANUAL,
    )
}

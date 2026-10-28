package com.example.expense_tracker.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class TransactionTypeFilter(val label: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSE("Expense"),
    REFUND("Refund"),
    TRANSFER("Transfer"),
}

enum class TransactionDateFilter(val label: String) {
    ALL("All time"),
    TODAY("Today"),
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
}

data class TransactionFilter(
    val query: String = "",
    val type: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val date: TransactionDateFilter = TransactionDateFilter.ALL,
    val category: String? = null,
) {
    fun matches(transaction: TransactionEntity, today: LocalDate): Boolean {
        val textMatches = query.isBlank() ||
            transaction.description.contains(query.trim(), ignoreCase = true) ||
            transaction.merchant.orEmpty().contains(query.trim(), ignoreCase = true)
        val typeMatches = when (type) {
            TransactionTypeFilter.ALL -> true
            TransactionTypeFilter.INCOME -> transaction.type == TransactionType.INCOME
            TransactionTypeFilter.EXPENSE -> transaction.type == TransactionType.EXPENSE
            TransactionTypeFilter.REFUND -> transaction.type == TransactionType.REFUND ||
                transaction.type == TransactionType.REVERSAL
            TransactionTypeFilter.TRANSFER -> transaction.type == TransactionType.TRANSFER
        }
        val transactionDate = transaction.transactionDateEpochMillis
            .toLocalDate()
        val dateMatches = when (date) {
            TransactionDateFilter.ALL -> true
            TransactionDateFilter.TODAY -> transactionDate == today
            TransactionDateFilter.THIS_WEEK -> transactionDate >=
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) &&
                transactionDate <= today
            TransactionDateFilter.THIS_MONTH ->
                transactionDate.year == today.year && transactionDate.month == today.month
        }
        val categoryMatches = category == null || transaction.categoryId == category

        return textMatches && typeMatches && dateMatches && categoryMatches
    }

    private fun Long.toLocalDate(): LocalDate =
        java.time.Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}

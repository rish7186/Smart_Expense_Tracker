package com.example.expense_tracker.data

data class FinancialSummary(
    val incomeMinor: Long = 0,
    val expenseMinor: Long = 0,
    val refundMinor: Long = 0,
    val netCashFlowMinor: Long = 0,
)

fun calculateFinancialSummary(transactions: List<TransactionEntity>): FinancialSummary {
    val eligible = transactions.filter {
        !it.isExcluded && it.status == TransactionStatus.COMPLETED
    }
    val income = eligible.sumOf { if (it.type == TransactionType.INCOME) it.amountMinor else 0L }
    val expenses = eligible.sumOf { if (it.type == TransactionType.EXPENSE) it.amountMinor else 0L }
    val refunds = eligible.sumOf {
        if (it.type == TransactionType.REFUND || it.type == TransactionType.REVERSAL) {
            it.amountMinor
        } else {
            0L
        }
    }

    return FinancialSummary(
        incomeMinor = income,
        expenseMinor = expenses,
        refundMinor = refunds,
        netCashFlowMinor = income + refunds - expenses,
    )
}

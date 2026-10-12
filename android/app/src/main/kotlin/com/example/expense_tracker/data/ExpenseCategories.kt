package com.example.expense_tracker.data

object ExpenseCategories {
    const val OVERALL_BUDGET = "__overall__"

    val expenses = listOf(
        "Food and Dining",
        "Groceries",
        "Shopping",
        "Transportation",
        "Fuel",
        "Bills and Utilities",
        "Rent",
        "Healthcare",
        "Education",
        "Entertainment",
        "Subscriptions",
        "Travel",
        "Cash Withdrawals",
        "Other",
    )

    val income = listOf(
        "Salary",
        "Freelance Income",
        "Business Income",
        "Refunds",
        "Other Income",
    )

    fun forType(type: TransactionType): List<String> = when (type) {
        TransactionType.INCOME, TransactionType.REFUND -> income
        TransactionType.EXPENSE -> expenses
        TransactionType.TRANSFER, TransactionType.REVERSAL -> listOf("Transfers", "Other")
    }
}

package com.example.expense_tracker.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(database: AppDatabase) {
    private val transactions = database.transactionDao()
    private val budgets = database.budgetDao()

    fun observeTransactions(): Flow<List<TransactionEntity>> = transactions.observeAll()

    fun observeBudgets(): Flow<List<BudgetEntity>> = budgets.observeAll()

    suspend fun addTransaction(transaction: TransactionEntity): Long {
        require(transaction.amountMinor > 0) { "Amount must be greater than zero." }
        require(transaction.description.isNotBlank()) { "Description is required." }
        require(transaction.categoryId.isNotBlank()) { "Category is required." }
        return transactions.insert(transaction)
    }

    suspend fun addDetectedTransactionIfNew(transaction: TransactionEntity): Boolean {
        require(transaction.source == TransactionSource.NOTIFICATION) {
            "Detected transactions must use the notification source."
        }
        require(transaction.amountMinor > 0) { "Amount must be greater than zero." }
        require(!transaction.referenceId.isNullOrBlank()) {
            "Detected transactions must have a deduplication reference."
        }
        if (transactions.hasReferenceId(transaction.referenceId)) return false
        transactions.insert(transaction)
        return true
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        require(transaction.amountMinor > 0) { "Amount must be greater than zero." }
        require(transaction.description.isNotBlank()) { "Description is required." }
        transactions.update(transaction.copy(updatedAtEpochMillis = System.currentTimeMillis()))
    }

    suspend fun deleteTransaction(id: Long) {
        transactions.deleteById(id)
    }

    suspend fun saveBudget(categoryId: String, limitMinor: Long) {
        require(categoryId.isNotBlank()) { "Category is required." }
        require(limitMinor > 0) { "Budget must be greater than zero." }
        budgets.upsert(BudgetEntity(categoryId = categoryId, limitMinor = limitMinor))
    }

    suspend fun removeBudget(categoryId: String) {
        budgets.deleteByCategory(categoryId)
    }
}

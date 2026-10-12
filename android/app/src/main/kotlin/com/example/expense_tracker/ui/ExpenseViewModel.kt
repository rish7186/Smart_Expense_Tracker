package com.example.expense_tracker.ui

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.ExpenseTrackerApplication
import com.example.expense_tracker.data.BudgetEntity
import com.example.expense_tracker.data.ExpenseRepository
import com.example.expense_tracker.data.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpenseUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val errorMessage: String? = null,
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExpenseRepository =
        (application as ExpenseTrackerApplication).repository
    private val operationError = MutableStateFlow<String?>(null)

    val state = combine(
        repository.observeTransactions(),
        repository.observeBudgets(),
        operationError,
    ) { transactions, budgets, error ->
        ExpenseUiState(transactions, budgets, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseUiState())

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                repository.addTransaction(transaction)
                operationError.value = null
            } catch (error: IllegalArgumentException) {
                operationError.value = error.message ?: "Check the transaction details."
            } catch (error: SQLiteException) {
                operationError.value = "The transaction could not be saved. Please try again."
            }
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                repository.updateTransaction(transaction)
                operationError.value = null
            } catch (error: IllegalArgumentException) {
                operationError.value = error.message ?: "Check the transaction details."
            } catch (error: SQLiteException) {
                operationError.value = "The transaction could not be updated. Please try again."
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(id)
                operationError.value = null
            } catch (error: SQLiteException) {
                operationError.value = "The transaction could not be deleted. Please try again."
            }
        }
    }

    fun saveBudget(categoryId: String, limitMinor: Long) {
        viewModelScope.launch {
            try {
                repository.saveBudget(categoryId, limitMinor)
                operationError.value = null
            } catch (error: IllegalArgumentException) {
                operationError.value = error.message ?: "Check the budget amount."
            } catch (error: SQLiteException) {
                operationError.value = "The budget could not be saved. Please try again."
            }
        }
    }

    fun removeBudget(categoryId: String) {
        viewModelScope.launch {
            try {
                repository.removeBudget(categoryId)
                operationError.value = null
            } catch (error: SQLiteException) {
                operationError.value = "The budget could not be deleted. Please try again."
            }
        }
    }

    fun clearError() {
        operationError.value = null
    }
}

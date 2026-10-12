package com.example.expense_tracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class TransactionType {
    INCOME,
    EXPENSE,
    REFUND,
    TRANSFER,
    REVERSAL,
}

enum class TransactionStatus {
    COMPLETED,
    PENDING,
    FAILED,
    REQUIRES_REVIEW,
}

enum class TransactionSource {
    MANUAL,
    SMS,
    NOTIFICATION,
    IMPORT,
}

enum class PaymentMethod {
    UPI,
    CARD,
    CASH,
    BANK_TRANSFER,
    OTHER,
}

@Entity(
    tableName = "transactions",
    indices = [
        Index("transactionDateEpochMillis"),
        Index("referenceId"),
    ],
)
@TypeConverters(TransactionConverters::class)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val type: TransactionType,
    val categoryId: String,
    val merchant: String?,
    val description: String,
    val transactionDateEpochMillis: Long,
    val paymentMethod: PaymentMethod,
    val source: TransactionSource,
    val referenceId: String? = null,
    val accountIdentifier: String? = null,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val isReviewed: Boolean = true,
    val isExcluded: Boolean = false,
    val notes: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "monthly_budgets")
data class BudgetEntity(
    @PrimaryKey val categoryId: String,
    val limitMinor: Long,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

class TransactionConverters {
    @TypeConverter
    fun transactionTypeToString(value: TransactionType): String = value.name

    @TypeConverter
    fun stringToTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun transactionStatusToString(value: TransactionStatus): String = value.name

    @TypeConverter
    fun stringToTransactionStatus(value: String): TransactionStatus =
        TransactionStatus.valueOf(value)

    @TypeConverter
    fun transactionSourceToString(value: TransactionSource): String = value.name

    @TypeConverter
    fun stringToTransactionSource(value: String): TransactionSource =
        TransactionSource.valueOf(value)

    @TypeConverter
    fun paymentMethodToString(value: PaymentMethod): String = value.name

    @TypeConverter
    fun stringToPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)
}

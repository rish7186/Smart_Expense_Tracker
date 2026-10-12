package com.example.expense_tracker.data

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetCoachTest {
    @Test
    fun warnsWhenRecordedSpendingExceedsBudget() {
        val today = LocalDate.of(2026, 10, 12)
        val insights = BudgetCoach.insights(
            budgets = listOf(budget(10_000)),
            transactions = listOf(expense(12_000, today)),
            today = today,
        )

        assertEquals(BudgetStatus.OVER_BUDGET, insights.single().status)
        assertEquals(12_000L, insights.single().spentMinor)
        assertEquals(0L, insights.single().remainingMinor)
    }

    @Test
    fun warnsEarlyWhenCurrentSpendingPaceProjectsAnOverrun() {
        val today = LocalDate.of(2026, 10, 15)
        val insights = BudgetCoach.insights(
            budgets = listOf(budget(20_000)),
            transactions = listOf(expense(12_000, today)),
            today = today,
        )

        assertEquals(BudgetStatus.PROJECTED_OVER, insights.single().status)
        assertTrue(insights.single().spentMinor < insights.single().limitMinor)
        assertTrue(insights.single().projectedMinor > insights.single().limitMinor)
    }

    @Test
    fun ignoresExcludedPendingFailedAndOtherMonthTransactions() {
        val today = LocalDate.of(2026, 10, 12)
        val transactions = listOf(
            expense(5_000, today, isExcluded = true),
            expense(5_000, today, status = TransactionStatus.PENDING),
            expense(5_000, today, status = TransactionStatus.FAILED),
            expense(5_000, today.minusMonths(1)),
        )

        val insight = BudgetCoach.insights(listOf(budget(10_000)), transactions, today).single()

        assertEquals(0L, insight.spentMinor)
        assertEquals(BudgetStatus.ON_TRACK, insight.status)
    }

    @Test
    fun categoryBudgetsOnlyCountThatCategory() {
        val today = LocalDate.of(2026, 10, 12)
        val transactions = listOf(
            expense(8_000, today, category = "Food and Dining"),
            expense(8_000, today, category = "Shopping"),
        )

        val insight = BudgetCoach.insights(
            listOf(budget(10_000, category = "Food and Dining")),
            transactions,
            today,
        ).single()

        assertEquals(8_000L, insight.spentMinor)
    }

    @Test
    fun answersBudgetQuestionWithRemainingDailyAllowance() {
        val today = LocalDate.of(2026, 10, 12)
        val expense = expense(5_000, today, category = "Food and Dining")
        val insights = BudgetCoach.insights(
            budgets = listOf(budget(20_000, category = "Food and Dining")),
            transactions = listOf(expense),
            today = today,
        )

        val answer = BudgetCoach.answer(
            question = "How can I keep Food and Dining spending under control?",
            insights = insights,
            transactions = listOf(expense),
            today = today,
        )

        assertEquals("Plan for Food and Dining", answer.title)
        assertTrue(answer.suggestions.any { it.contains("per day") })
        assertTrue(answer.message.contains("Food and Dining"))
    }

    private fun budget(limit: Long, category: String = ExpenseCategories.OVERALL_BUDGET) =
        BudgetEntity(categoryId = category, limitMinor = limit)

    private fun expense(
        amountMinor: Long,
        date: LocalDate,
        category: String = "Other",
        status: TransactionStatus = TransactionStatus.COMPLETED,
        isExcluded: Boolean = false,
    ) = TransactionEntity(
        amountMinor = amountMinor,
        type = TransactionType.EXPENSE,
        categoryId = category,
        merchant = "Test shop",
        description = "Synthetic test expense",
        transactionDateEpochMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        paymentMethod = PaymentMethod.OTHER,
        source = TransactionSource.MANUAL,
        status = status,
        isExcluded = isExcluded,
    )
}

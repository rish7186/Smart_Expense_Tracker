package com.example.expense_tracker.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class BudgetStatus {
    ON_TRACK,
    PROJECTED_OVER,
    AT_LIMIT,
    OVER_BUDGET,
}

data class BudgetInsight(
    val categoryId: String,
    val spentMinor: Long,
    val limitMinor: Long,
    val projectedMinor: Long,
    val remainingMinor: Long,
    val daysRemainingIncludingToday: Int,
    val status: BudgetStatus,
)

data class BudgetAdvice(
    val title: String,
    val message: String,
    val suggestions: List<String>,
)

object BudgetCoach {
    fun insights(
        budgets: List<BudgetEntity>,
        transactions: List<TransactionEntity>,
        today: LocalDate,
    ): List<BudgetInsight> {
        val month = YearMonth.from(today)
        val eligible = transactions.filter { transaction ->
            transaction.status == TransactionStatus.COMPLETED &&
                !transaction.isExcluded &&
                transaction.transactionDateEpochMillis
                    .toLocalDate()
                    .let { YearMonth.from(it) == month }
        }
        val daysInMonth = month.lengthOfMonth()
        val elapsedDays = today.dayOfMonth
        val daysRemaining = daysInMonth - elapsedDays + 1

        return budgets.map { budget ->
            val isOverall = budget.categoryId == ExpenseCategories.OVERALL_BUDGET
            val relevant = eligible.filter { transaction ->
                if (isOverall) {
                    transaction.type == TransactionType.EXPENSE ||
                        transaction.type == TransactionType.REFUND ||
                        transaction.type == TransactionType.REVERSAL
                } else {
                    transaction.categoryId == budget.categoryId &&
                        (transaction.type == TransactionType.EXPENSE ||
                            transaction.type == TransactionType.REFUND ||
                            transaction.type == TransactionType.REVERSAL)
                }
            }
            val spent = relevant.sumOf { transaction ->
                when (transaction.type) {
                    TransactionType.EXPENSE -> transaction.amountMinor
                    TransactionType.REFUND, TransactionType.REVERSAL -> -transaction.amountMinor
                    else -> 0L
                }
            }.coerceAtLeast(0L)
            val projected = BigDecimal.valueOf(spent)
                .multiply(BigDecimal.valueOf(daysInMonth.toLong()))
                .divide(BigDecimal.valueOf(elapsedDays.toLong()), 0, RoundingMode.CEILING)
                .longValueExact()
            val status = when {
                spent > budget.limitMinor -> BudgetStatus.OVER_BUDGET
                spent == budget.limitMinor -> BudgetStatus.AT_LIMIT
                projected > budget.limitMinor -> BudgetStatus.PROJECTED_OVER
                else -> BudgetStatus.ON_TRACK
            }

            BudgetInsight(
                categoryId = budget.categoryId,
                spentMinor = spent,
                limitMinor = budget.limitMinor,
                projectedMinor = projected,
                remainingMinor = (budget.limitMinor - spent).coerceAtLeast(0L),
                daysRemainingIncludingToday = daysRemaining,
                status = status,
            )
        }.sortedWith(
            compareBy<BudgetInsight> { statusPriority(it.status) }
                .thenByDescending {
                    BigDecimal.valueOf(it.spentMinor)
                        .divide(BigDecimal.valueOf(it.limitMinor), 4, RoundingMode.HALF_UP)
                },
        )
    }

    fun answer(
        question: String,
        insights: List<BudgetInsight>,
        transactions: List<TransactionEntity>,
        today: LocalDate,
    ): BudgetAdvice {
        val normalizedQuestion = question.trim().lowercase()
        val selected = insights.firstOrNull { insight ->
            insight.categoryId != ExpenseCategories.OVERALL_BUDGET &&
                normalizedQuestion.contains(insight.categoryId.lowercase())
        } ?: insights.firstOrNull { insight ->
            insight.categoryId == ExpenseCategories.OVERALL_BUDGET &&
                listOf("overall", "total", "all budget", "whole budget").any(normalizedQuestion::contains)
        } ?: insights.firstOrNull()

        if (selected == null) {
            return BudgetAdvice(
                title = "Set a budget to get a plan",
                message = "I can help you plan against a monthly target. Add an overall or category budget first.",
                suggestions = emptyList(),
            )
        }

        val label = if (selected.categoryId == ExpenseCategories.OVERALL_BUDGET) {
            "overall budget"
        } else {
            selected.categoryId
        }
        val largestExpense = transactions.asSequence()
            .filter {
                it.status == TransactionStatus.COMPLETED &&
                    !it.isExcluded &&
                    it.type == TransactionType.EXPENSE &&
                    it.transactionDateEpochMillis.toLocalDate().let { date ->
                        date.year == today.year && date.month == today.month
                    } &&
                    (selected.categoryId == ExpenseCategories.OVERALL_BUDGET ||
                        it.categoryId == selected.categoryId)
            }
            .maxByOrNull { it.amountMinor }
        val dailyAllowance = selected.remainingMinor / selected.daysRemainingIncludingToday
        val baseSuggestions = mutableListOf<String>()

        when (selected.status) {
            BudgetStatus.OVER_BUDGET -> {
                val overBy = selected.spentMinor - selected.limitMinor
                baseSuggestions += "You are ${Money.formatMinorUnits(overBy)} over the $label already. To avoid widening the gap, pause optional purchases in this area for the rest of the month."
                baseSuggestions += "Review upcoming bills and essentials first; move or cancel only costs you can safely defer."
            }
            BudgetStatus.AT_LIMIT -> {
                baseSuggestions += "You have reached the $label. Avoid additional non-essential spending in this area for the rest of the month."
                baseSuggestions += "Check scheduled payments now so you can plan for any unavoidable costs."
            }
            BudgetStatus.PROJECTED_OVER -> {
                baseSuggestions += "At the current pace, spending may reach about ${Money.formatMinorUnits(selected.projectedMinor)}, above the ${Money.formatMinorUnits(selected.limitMinor)} target."
                baseSuggestions += "To get back on pace, aim to spend no more than ${Money.formatMinorUnits(dailyAllowance)} per day for the remaining ${selected.daysRemainingIncludingToday} days."
            }
            BudgetStatus.ON_TRACK -> {
                baseSuggestions += "${Money.formatMinorUnits(selected.remainingMinor)} remains in the $label. A practical ceiling is about ${Money.formatMinorUnits(dailyAllowance)} per day for the rest of this month."
                baseSuggestions += "Keep a small buffer for bills or unexpected essentials instead of planning to use every rupee."
            }
        }

        if (largestExpense != null) {
            val description = largestExpense.merchant
                ?.takeIf { it.isNotBlank() }
                ?: largestExpense.description
            baseSuggestions += "Your largest recorded ${if (selected.categoryId == ExpenseCategories.OVERALL_BUDGET) "expense" else selected.categoryId + " expense"} this month was ${Money.formatMinorUnits(largestExpense.amountMinor)} at $description. Check whether a similar optional purchase can be reduced."
        }

        return BudgetAdvice(
            title = when (selected.status) {
                BudgetStatus.OVER_BUDGET -> "Budget exceeded"
                BudgetStatus.AT_LIMIT -> "Budget limit reached"
                BudgetStatus.PROJECTED_OVER -> "Overrun risk for $label"
                BudgetStatus.ON_TRACK -> "Plan for $label"
            },
            message = "Based on completed transactions recorded on this device: ${Money.formatMinorUnits(selected.spentMinor)} spent of ${Money.formatMinorUnits(selected.limitMinor)} for $label.",
            suggestions = baseSuggestions,
        )
    }

    private fun statusPriority(status: BudgetStatus): Int = when (status) {
        BudgetStatus.OVER_BUDGET -> 0
        BudgetStatus.AT_LIMIT -> 1
        BudgetStatus.PROJECTED_OVER -> 2
        BudgetStatus.ON_TRACK -> 3
    }

    private fun Long.toLocalDate(): LocalDate =
        java.time.Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}

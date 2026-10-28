package com.example.expense_tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.expense_tracker.data.BudgetInsight
import com.example.expense_tracker.data.BudgetStatus
import com.example.expense_tracker.data.BudgetAdvice
import com.example.expense_tracker.data.BudgetCoach
import com.example.expense_tracker.data.ExpenseCategories
import com.example.expense_tracker.data.Money
import com.example.expense_tracker.data.TransactionEntity
import java.time.LocalDate

@Composable
fun BudgetCoachPanel(
    insights: List<BudgetInsight>,
    transactions: List<TransactionEntity>,
    today: LocalDate,
    hideAmounts: Boolean = false,
) {
    var question by rememberSaveable { mutableStateOf("") }
    var advice by remember { mutableStateOf<BudgetAdvice?>(null) }
    val warning = insights.firstOrNull {
        it.status != BudgetStatus.ON_TRACK
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (warning == null) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Smart Budget Coach · on-device",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when {
                    insights.isEmpty() -> "Set an overall or category budget to receive spending warnings and a personalized plan."
                    warning == null -> "You’re currently on pace for your set budgets. Ask for a plan to keep it that way."
                    else -> warningMessage(warning)
                },
                modifier = Modifier.semantics {
                    if (hideAmounts) contentDescription = "Budget status. Amounts hidden."
                },
                color = if (warning == null) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
            )

            insights.filter { it.status != BudgetStatus.ON_TRACK }.forEach { insight ->
                Text(
                    if (hideAmounts) {
                        "${budgetLabel(insight.categoryId)}: amounts hidden"
                    } else {
                        "${budgetLabel(insight.categoryId)}: ${Money.formatMinorUnits(insight.spentMinor)} of ${Money.formatMinorUnits(insight.limitMinor)} · forecast ${Money.formatMinorUnits(insight.projectedMinor)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (warning != null) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (insights.isNotEmpty()) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ask how to stay within your budget") },
                    placeholder = { Text("e.g. How can I reduce food spending?") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { advice = BudgetCoach.answer(question, insights, transactions, today) },
                    ),
                )
                Button(
                    onClick = { advice = BudgetCoach.answer(question, insights, transactions, today) },
                    enabled = question.isNotBlank(),
                ) {
                    Text("Get a plan")
                }
            }

            advice?.let { answer ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(answer.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        maskCurrency(answer.message, hideAmounts),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    answer.suggestions.forEach { suggestion ->
                        Row {
                            Text("•", modifier = Modifier.padding(end = 8.dp))
                            Text(
                                maskCurrency(suggestion, hideAmounts),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Text(
                "Suggestions use your local completed transactions and budgets; this is guidance, not a guarantee. No financial data is sent to an AI service.",
                style = MaterialTheme.typography.labelSmall,
                color = if (warning == null) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

private fun maskCurrency(text: String, hideAmounts: Boolean): String =
    if (hideAmounts) text.replace(Regex("""₹\s*[\d,]+(?:\.\d{1,2})?"""), "••••") else text

private fun warningMessage(insight: BudgetInsight): String {
    val label = budgetLabel(insight.categoryId)
    return when (insight.status) {
        BudgetStatus.OVER_BUDGET ->
            "You’ve gone over your $label. Review the plan below to limit further spending."
        BudgetStatus.AT_LIMIT ->
            "You’ve reached your $label. New optional spending will exceed your target."
        BudgetStatus.PROJECTED_OVER ->
            "At this month’s pace, you may exceed your $label. Adjust now to stay on target."
        BudgetStatus.ON_TRACK -> "You’re on pace for your $label."
    }
}

private fun budgetLabel(categoryId: String): String =
    if (categoryId == ExpenseCategories.OVERALL_BUDGET) "overall budget" else categoryId

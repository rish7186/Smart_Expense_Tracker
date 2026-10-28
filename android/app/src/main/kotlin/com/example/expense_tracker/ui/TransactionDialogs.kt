package com.example.expense_tracker.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expense_tracker.data.ExpenseCategories
import com.example.expense_tracker.data.Money
import com.example.expense_tracker.data.PaymentMethod
import com.example.expense_tracker.data.TransactionEntity
import com.example.expense_tracker.data.TransactionSource
import com.example.expense_tracker.data.TransactionType
import com.example.expense_tracker.ui.components.AppChip
import com.example.expense_tracker.ui.components.CategoryIcon
import com.example.expense_tracker.ui.theme.FintechColors
import com.example.expense_tracker.ui.theme.FintechShapes
import com.example.expense_tracker.ui.theme.FintechSpacing
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf(TransactionType.EXPENSE.name) }
    var category by rememberSaveable { mutableStateOf(ExpenseCategories.expenses.first()) }
    var paymentMethodName by rememberSaveable { mutableStateOf(PaymentMethod.UPI.name) }
    var transactionTime by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var saving by rememberSaveable { mutableStateOf(false) }
    val type = TransactionType.valueOf(typeName)
    val categories = ExpenseCategories.forType(type)
    val amountMinor = Money.parseMinorUnits(amountText)
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(FintechShapes.largeCard),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add transaction") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Transaction type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FintechSpacing.small),
                ) {
                    listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { option ->
                        AppChip(
                            label = if (option == TransactionType.EXPENSE) "Expense" else "Income",
                            selected = type == option,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                typeName = option.name
                                category = ExpenseCategories.forType(option).first()
                            },
                        )
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(FintechShapes.card),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            "AMOUNT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("₹") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary,
                            ),
                            supportingText = {
                                if (amountText.isNotBlank() && amountMinor == null) {
                                    Text("Enter a positive amount with up to 2 decimal places.")
                                }
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Merchant (optional)") },
                    singleLine = true,
                )
                Text(
                    "Choose a category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories) { option ->
                        TransactionCategoryTile(
                            category = option,
                            selected = category == option,
                            onClick = { category = option },
                        )
                    }
                }
                DropdownSelector(
                    label = "Payment method",
                    selected = PaymentMethod.valueOf(paymentMethodName).displayName,
                    options = PaymentMethod.entries.map { it.displayName },
                    onSelect = { selected ->
                        paymentMethodName = PaymentMethod.entries
                            .first { it.displayName == selected }
                            .name
                    },
                )
                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = transactionTime
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val chosenDate = Calendar.getInstance().apply {
                                    timeInMillis = transactionTime
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, day)
                                }
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        chosenDate.set(Calendar.HOUR_OF_DAY, hour)
                                        chosenDate.set(Calendar.MINUTE, minute)
                                        chosenDate.set(Calendar.SECOND, 0)
                                        chosenDate.set(Calendar.MILLISECOND, 0)
                                        transactionTime = chosenDate.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false,
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Date and time · ${DateFormat.getDateTimeInstance().format(Date(transactionTime))}")
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = amountMinor != null && description.isNotBlank() && !saving,
                onClick = {
                    val validAmount = amountMinor ?: return@Button
                    saving = true
                    onSave(
                        TransactionEntity(
                            amountMinor = validAmount,
                            type = type,
                            categoryId = category,
                            merchant = merchant.trim().ifBlank { null },
                            description = description.trim(),
                            transactionDateEpochMillis = transactionTime,
                            paymentMethod = PaymentMethod.valueOf(paymentMethodName),
                            source = TransactionSource.MANUAL,
                            notes = notes.trim().ifBlank { null },
                        ),
                    )
                },
            ) {
                Text(if (saving) "Saving…" else "Save transaction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun TransactionCategoryTile(
    category: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        label = "category-selection",
    )
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FintechShapes.control),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            CategoryIcon(category, Modifier.size(32.dp))
            Text(
                text = category,
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onSave: (categoryId: String, limitMinor: Long) -> Unit,
) {
    var category by rememberSaveable { mutableStateOf(ExpenseCategories.OVERALL_BUDGET) }
    var amountText by rememberSaveable { mutableStateOf("") }
    val amountMinor = Money.parseMinorUnits(amountText)
    val budgetOptions = listOf("Overall monthly budget" to ExpenseCategories.OVERALL_BUDGET) +
        ExpenseCategories.expenses.map { it to it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a monthly budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownSelector(
                    label = "Budget",
                    selected = budgetOptions.first { it.second == category }.first,
                    options = budgetOptions.map { it.first },
                    onSelect = { selected -> category = budgetOptions.first { it.first == selected }.second },
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monthly limit (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        if (amountText.isNotBlank() && amountMinor == null) {
                            Text("Enter a positive amount with up to 2 decimal places.")
                        }
                    },
                )
                Text(
                    "Budgets are stored on this device and can be changed any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = amountMinor != null,
                onClick = {
                    val limit = amountMinor ?: return@TextButton
                    onSave(category, limit)
                },
            ) { Text("Save budget") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("$label: $selected", maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private val PaymentMethod.displayName: String
    get() = when (this) {
        PaymentMethod.UPI -> "UPI"
        PaymentMethod.CARD -> "Card"
        PaymentMethod.CASH -> "Cash"
        PaymentMethod.BANK_TRANSFER -> "Bank transfer"
        PaymentMethod.OTHER -> "Other"
    }

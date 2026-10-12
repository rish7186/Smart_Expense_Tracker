package com.example.expense_tracker.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expense_tracker.data.BudgetEntity
import com.example.expense_tracker.data.BudgetCoach
import com.example.expense_tracker.data.ExpenseCategories
import com.example.expense_tracker.data.FinancialSummary
import com.example.expense_tracker.data.Money
import com.example.expense_tracker.data.PaymentMethod
import com.example.expense_tracker.data.TransactionEntity
import com.example.expense_tracker.data.TransactionStatus
import com.example.expense_tracker.data.TransactionType
import com.example.expense_tracker.data.calculateFinancialSummary
import com.example.expense_tracker.detection.NotificationAccessStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private data class Destination(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val destinations = listOf(
    Destination("home", "Home", Icons.Default.Home),
    Destination("transactions", "Transactions", Icons.AutoMirrored.Filled.ReceiptLong),
    Destination("analytics", "Analytics", Icons.Default.BarChart),
    Destination("budgets", "Budgets", Icons.Default.AccountBalanceWallet),
    Destination("settings", "Settings", Icons.Default.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(viewModel: ExpenseViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route ?: "home"
    val title = destinations.firstOrNull { it.route == route }?.title ?: "Smart Expenses"
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<TransactionEntity?>(null) }
    var budgetDialog by rememberSaveable { mutableStateOf(false) }
    var notificationAccessEnabled by remember {
        mutableStateOf(NotificationAccessStatus.isEnabled(context))
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessEnabled = NotificationAccessStatus.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        if (route == "home") {
                            Text(
                                "Your money, clearly.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = route == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.title) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(insets),
        ) {
            composable("home") {
                DashboardScreen(
                    state = state,
                    onAddBudget = { budgetDialog = true },
                    onOpenTransaction = { selectedTransaction = it },
                )
            }
            composable("transactions") {
                TransactionsScreen(
                    transactions = state.transactions,
                    onOpenTransaction = { selectedTransaction = it },
                    onDelete = { deleteTarget = it },
                )
            }
            composable("analytics") {
                AnalyticsScreen(transactions = state.transactions)
            }
            composable("budgets") {
                BudgetsScreen(
                    transactions = state.transactions,
                    budgets = state.budgets,
                    onAddBudget = { budgetDialog = true },
                    onRemoveBudget = viewModel::removeBudget,
                )
            }
            composable("settings") {
                SettingsScreen(
                    notificationAccessEnabled = notificationAccessEnabled,
                    onManageNotificationAccess = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                            )
                        } catch (_: ActivityNotFoundException) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Notification access settings are unavailable on this device.",
                                )
                            }
                        }
                    },
                )
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { transaction ->
                showAddDialog = false
                viewModel.addTransaction(transaction)
            },
        )
    }
    if (budgetDialog) {
        AddBudgetDialog(
            onDismiss = { budgetDialog = false },
            onSave = { category, amount ->
                budgetDialog = false
                viewModel.saveBudget(category, amount)
            },
        )
    }
    selectedTransaction?.let { transaction ->
        TransactionDetailsDialog(
            transaction = transaction,
            onDismiss = { selectedTransaction = null },
            onToggleExcluded = {
                viewModel.updateTransaction(transaction.copy(isExcluded = !transaction.isExcluded))
                selectedTransaction = null
            },
        )
    }
    deleteTarget?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete transaction?") },
            text = { Text("This permanently removes ${transaction.description} from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(transaction.id)
                    deleteTarget = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

private enum class SummaryPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
}

private fun TransactionEntity.isInPeriod(period: SummaryPeriod, today: LocalDate): Boolean {
    val date = Instant.ofEpochMilli(transactionDateEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return when (period) {
        SummaryPeriod.TODAY -> date == today
        SummaryPeriod.THIS_WEEK -> {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            date >= monday && date <= today
        }
        SummaryPeriod.THIS_MONTH -> date.year == today.year && date.month == today.month
    }
}

@Composable
private fun DashboardScreen(
    state: ExpenseUiState,
    onAddBudget: () -> Unit,
    onOpenTransaction: (TransactionEntity) -> Unit,
) {
    var period by rememberSaveable { mutableStateOf(SummaryPeriod.THIS_MONTH.name) }
    val selectedPeriod = SummaryPeriod.valueOf(period)
    val today = LocalDate.now()
    val transactions = state.transactions.filter { it.isInPeriod(selectedPeriod, today) }
    val summary = calculateFinancialSummary(transactions)
    val overallBudget = state.budgets.firstOrNull {
        it.categoryId == ExpenseCategories.OVERALL_BUDGET
    }
    val budgetInsights = BudgetCoach.insights(state.budgets, state.transactions, today)
    val netSpending = (summary.expenseMinor - summary.refundMinor).coerceAtLeast(0)
    val budgetProgress = overallBudget?.let {
        (netSpending.toFloat() / it.limitMinor).coerceIn(0f, 1f)
    } ?: 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryPeriod.entries.forEach { option ->
                FilterChip(
                    selected = selectedPeriod == option,
                    onClick = { period = option.name },
                    label = { Text(option.label) },
                )
            }
        }

        CashFlowCard(summary)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = "Income",
                amount = summary.incomeMinor,
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.primary,
            )
            MetricCard(
                label = "Expenses",
                amount = summary.expenseMinor,
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.error,
            )
        }

        SectionHeading("Monthly budget")
        if (overallBudget == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Give your spending a target", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Set a monthly budget and keep an eye on your progress.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(onClick = onAddBudget) { Text("Set a budget") }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Spent", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${Money.formatMinorUnits(netSpending)} of ${Money.formatMinorUnits(overallBudget.limitMinor)}",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        if (netSpending > overallBudget.limitMinor) "You’re over your monthly target."
                        else "Monthly spending, after refunds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        BudgetCoachPanel(
            insights = budgetInsights,
            transactions = state.transactions,
            today = today,
        )

        SectionHeading("Recent transactions")
        val recent = state.transactions.take(4)
        if (recent.isEmpty()) {
            EmptyCard(
                title = "No transactions yet",
                message = "Add your first income or expense to see your cash flow here.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recent.forEach { transaction ->
                    TransactionCard(transaction, onClick = { onOpenTransaction(transaction) })
                }
            }
        }
    }
}

@Composable
private fun CashFlowCard(summary: FinancialSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("NET CASH FLOW", style = MaterialTheme.typography.labelLarge)
            Text(
                Money.formatMinorUnits(summary.netCashFlowMinor),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Calculated from recorded transactions · not a bank balance",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MetricCard(label: String, amount: Long, modifier: Modifier, accent: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                Money.formatMinorUnits(amount),
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    onOpenTransaction: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf("All") }
    val types = listOf("All", "Income", "Expense", "Refund", "Transfer")
    val filtered = transactions.filter { transaction ->
        val queryMatches = search.isBlank() ||
            transaction.description.contains(search, ignoreCase = true) ||
            transaction.merchant.orEmpty().contains(search, ignoreCase = true)
        val typeMatches = when (selectedType) {
            "Income" -> transaction.type == TransactionType.INCOME
            "Expense" -> transaction.type == TransactionType.EXPENSE
            "Refund" -> transaction.type == TransactionType.REFUND
            "Transfer" -> transaction.type == TransactionType.TRANSFER
            else -> true
        }
        queryMatches && typeMatches
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search transactions") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            types.forEach { type ->
                FilterChip(
                    selected = type == selectedType,
                    onClick = { selectedType = type },
                    label = { Text(type) },
                )
            }
        }
        if (filtered.isEmpty()) {
            EmptyCard(
                title = if (transactions.isEmpty()) "Your history starts here" else "No matches",
                message = if (transactions.isEmpty()) {
                    "Transactions you add will appear here."
                } else {
                    "Try a different search or transaction type."
                },
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onClick = { onOpenTransaction(transaction) },
                        onDelete = { onDelete(transaction) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsScreen(transactions: List<TransactionEntity>) {
    val monthTransactions = transactions.filter {
        it.isInPeriod(SummaryPeriod.THIS_MONTH, LocalDate.now())
    }
    val summary = calculateFinancialSummary(monthTransactions)
    val categoryTotals = monthTransactions
        .filter {
            !it.isExcluded && it.status == TransactionStatus.COMPLETED &&
                (it.type == TransactionType.EXPENSE || it.type == TransactionType.REFUND)
        }
        .groupBy { it.categoryId }
        .mapValues { (_, items) ->
            items.sumOf {
                when (it.type) {
                    TransactionType.REFUND -> -it.amountMinor
                    else -> it.amountMinor
                }
            }.coerceAtLeast(0)
        }
        .filterValues { it > 0 }
        .toList()
        .sortedByDescending { it.second }
    val maxCategory = categoryTotals.maxOfOrNull { it.second } ?: 1L
    val totalSpend = (summary.expenseMinor - summary.refundMinor).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("This month", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                "Income",
                summary.incomeMinor,
                Modifier.weight(1f),
                MaterialTheme.colorScheme.primary,
            )
            MetricCard(
                "Net spending",
                totalSpend,
                Modifier.weight(1f),
                MaterialTheme.colorScheme.error,
            )
        }
        SectionHeading("Spending by category")
        if (categoryTotals.isEmpty()) {
            EmptyCard(
                title = "Nothing to chart yet",
                message = "Add a completed expense to see your category breakdown.",
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    categoryTotals.forEach { (category, amount) ->
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row {
                                Text(category, modifier = Modifier.weight(1f))
                                Text(Money.formatMinorUnits(amount), fontWeight = FontWeight.Medium)
                            }
                            LinearProgressIndicator(
                                progress = { amount.toFloat() / maxCategory },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
        SectionHeading("How calculations work")
        Text(
            "Transfers are excluded from income and spending. Refunds reduce net spending and increase cash flow. Excluded, pending, and failed transactions are not counted.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BudgetsScreen(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    onAddBudget: () -> Unit,
    onRemoveBudget: (String) -> Unit,
) {
    val currentMonth = transactions.filter {
        it.isInPeriod(SummaryPeriod.THIS_MONTH, LocalDate.now()) &&
            !it.isExcluded && it.status == TransactionStatus.COMPLETED
    }
    val categorySpending = currentMonth
        .filter {
            it.type == TransactionType.EXPENSE ||
                it.type == TransactionType.REFUND ||
                it.type == TransactionType.REVERSAL
        }
        .groupBy { it.categoryId }
        .mapValues { (_, items) ->
            items.sumOf {
                if (it.type == TransactionType.REFUND || it.type == TransactionType.REVERSAL) {
                    -it.amountMinor
                } else {
                    it.amountMinor
                }
            }.coerceAtLeast(0)
        }
    val overallSpent = currentMonth.sumOf {
        when (it.type) {
            TransactionType.EXPENSE -> it.amountMinor
            TransactionType.REFUND -> -it.amountMinor
            else -> 0L
        }
    }.coerceAtLeast(0)
    val today = LocalDate.now()
    val budgetInsights = BudgetCoach.insights(budgets, transactions, today)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Monthly limits",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = onAddBudget) { Text("Add a budget") }
        BudgetCoachPanel(
            insights = budgetInsights,
            transactions = transactions,
            today = today,
        )
        if (budgets.isEmpty()) {
            EmptyCard(
                title = "No budgets set",
                message = "Set an overall or category limit to make your plan visible.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                budgets.forEach { budget ->
                    val spent = if (budget.categoryId == ExpenseCategories.OVERALL_BUDGET) {
                        overallSpent
                    } else {
                        categorySpending[budget.categoryId] ?: 0L
                    }
                    BudgetCard(budget, spent, onRemove = { onRemoveBudget(budget.categoryId) })
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(budget: BudgetEntity, spent: Long, onRemove: () -> Unit) {
    val label = if (budget.categoryId == ExpenseCategories.OVERALL_BUDGET) {
        "Overall monthly budget"
    } else {
        budget.categoryId
    }
    val progress = (spent.toFloat() / budget.limitMinor).coerceIn(0f, 1f)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${Money.formatMinorUnits(spent)} of ${Money.formatMinorUnits(budget.limitMinor)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove $label")
                }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            if (spent > budget.limitMinor) {
                Text("Over budget", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    notificationAccessEnabled: Boolean,
    onManageNotificationAccess: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Automatic transaction detection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (notificationAccessEnabled) {
                        "Active. New bank transaction SMS notifications and payment alerts are checked on this device. Matching transactions are saved locally without manual entry."
                    } else {
                        "Off. Enable notification access once to detect bank transaction SMS notifications and supported payment alerts. After that, detection starts automatically when Android connects the listener."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = onManageNotificationAccess) {
                    Text(if (notificationAccessEnabled) "Manage notification access" else "Enable detection")
                }
                Text(
                    "Only transaction-like alerts from supported payment and messaging apps are parsed. Other app notifications are ignored. This reads notifications, not your SMS inbox.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SettingsCard(
            title = "Privacy",
            body = "Transactions and budgets are stored in the app’s local Room database. The app never accesses banking credentials or SMS. Notification access is optional and is used only to inspect supported payment-app alerts.",
        )
        SettingsCard(
            title = "Currency",
            body = "Indian Rupee (INR · ₹)",
        )
        SettingsCard(
            title = "Account balance",
            body = "The cash-flow figure is calculated from your recorded transactions. It is not a verified bank account balance.",
        )
    }
}

@Composable
private fun SettingsCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val isCredit = transaction.type == TransactionType.INCOME ||
        transaction.type == TransactionType.REFUND ||
        transaction.type == TransactionType.REVERSAL
    val sign = if (isCredit) "+" else if (transaction.type == TransactionType.EXPENSE) "−" else ""
    val amountColor = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 13.dp, bottom = 13.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    transaction.categoryId.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.merchant?.takeIf { it.isNotBlank() } ?: transaction.description,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${transaction.categoryId} · ${transaction.type.label()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$sign${Money.formatMinorUnits(transaction.amountMinor)}",
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor,
                    maxLines = 1,
                )
                Text(
                    DateFormat.getDateInstance(DateFormat.MEDIUM)
                        .format(Date(transaction.transactionDateEpochMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete transaction")
                }
            }
        }
    }
}

private fun TransactionType.label(): String = when (this) {
    TransactionType.INCOME -> "Income"
    TransactionType.EXPENSE -> "Expense"
    TransactionType.REFUND -> "Refund"
    TransactionType.TRANSFER -> "Transfer"
    TransactionType.REVERSAL -> "Reversal"
}

@Composable
private fun SectionHeading(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyCard(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TransactionDetailsDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onToggleExcluded: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(transaction.merchant?.takeIf { it.isNotBlank() } ?: transaction.description) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Amount", Money.formatMinorUnits(transaction.amountMinor))
                DetailRow("Type", transaction.type.label())
                DetailRow("Category", transaction.categoryId)
                DetailRow("Date", DateFormat.getDateTimeInstance().format(Date(transaction.transactionDateEpochMillis)))
                DetailRow("Payment method", transaction.paymentMethod.label())
                DetailRow("Source", transaction.source.name.lowercase().replaceFirstChar { it.uppercase() })
                DetailRow("Status", transaction.status.name.lowercase().replace('_', ' '))
                if (transaction.isExcluded) {
                    Text("Excluded from totals", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onToggleExcluded) {
                Text(if (transaction.isExcluded) "Include in totals" else "Exclude from totals")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, modifier = Modifier.weight(1.2f))
    }
}

private fun PaymentMethod.label(): String = when (this) {
    PaymentMethod.UPI -> "UPI"
    PaymentMethod.CARD -> "Card"
    PaymentMethod.CASH -> "Cash"
    PaymentMethod.BANK_TRANSFER -> "Bank transfer"
    PaymentMethod.OTHER -> "Other"
}

package com.example.expense_tracker.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.animation.ValueAnimator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.expense_tracker.data.TransactionDateFilter
import com.example.expense_tracker.data.TransactionFilter
import com.example.expense_tracker.data.TransactionStatus
import com.example.expense_tracker.data.TransactionType
import com.example.expense_tracker.data.TransactionTypeFilter
import com.example.expense_tracker.data.calculateFinancialSummary
import com.example.expense_tracker.detection.NotificationAccessStatus
import com.example.expense_tracker.ui.theme.FintechShapes
import com.example.expense_tracker.ui.theme.FintechColors
import com.example.expense_tracker.ui.components.AmountText
import com.example.expense_tracker.ui.components.AppCard
import com.example.expense_tracker.ui.components.AppChip
import com.example.expense_tracker.ui.components.CategoryIcon
import com.example.expense_tracker.ui.components.EmptyState
import com.example.expense_tracker.ui.components.SectionHeader
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    Destination("profile", "Profile", Icons.Default.Person),
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
    val title = when (route) {
        "profile" -> "Profile & Settings"
        else -> destinations.firstOrNull { it.route == route }?.title ?: "Smart Expenses"
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<TransactionEntity?>(null) }
    var budgetDialog by rememberSaveable { mutableStateOf(false) }
    var hideAmounts by remember { mutableStateOf(true) }
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
        containerColor = MaterialTheme.colorScheme.background,
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
                    if (route != "home") {
                        IconButton(onClick = { hideAmounts = !hideAmounts }) {
                            Icon(
                                imageVector = if (hideAmounts) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (hideAmounts) {
                                    "Show amounts"
                                } else {
                                    "Hide amounts"
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
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
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(FintechShapes.control),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
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
                    hideAmounts = hideAmounts,
                    onToggleAmounts = { hideAmounts = !hideAmounts },
                    onAddBudget = { budgetDialog = true },
                    onOpenTransaction = { selectedTransaction = it },
                    onViewAll = {
                        navController.navigate("transactions") {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable("transactions") {
                TransactionsScreen(
                    transactions = state.transactions,
                    hideAmounts = hideAmounts,
                    onOpenTransaction = { selectedTransaction = it },
                    onDelete = { deleteTarget = it },
                    onAddTransaction = { showAddDialog = true },
                )
            }
            composable("analytics") {
                AnalyticsScreen(
                    transactions = state.transactions,
                    hideAmounts = hideAmounts,
                )
            }
            composable("budgets") {
                BudgetsScreen(
                    transactions = state.transactions,
                    budgets = state.budgets,
                    hideAmounts = hideAmounts,
                    onAddBudget = { budgetDialog = true },
                    onRemoveBudget = viewModel::removeBudget,
                )
            }
            composable("profile") {
                ProfileScreen(
                    transactions = state.transactions,
                    budgetCount = state.budgets.size,
                    hideAmounts = hideAmounts,
                    notificationAccessEnabled = notificationAccessEnabled,
                    onManageBudgets = {
                        navController.navigate("budgets") {
                            launchSingleTop = true
                        }
                    },
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
            hideAmount = hideAmounts,
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
    hideAmounts: Boolean,
    onToggleAmounts: () -> Unit,
    onAddBudget: () -> Unit,
    onOpenTransaction: (TransactionEntity) -> Unit,
    onViewAll: () -> Unit,
) {
    var period by rememberSaveable { mutableStateOf(SummaryPeriod.THIS_MONTH.name) }
    val selectedPeriod = SummaryPeriod.valueOf(period)
    val today = LocalDate.now()
    val transactions = state.transactions.filter { it.isInPeriod(selectedPeriod, today) }
    val summary = calculateFinancialSummary(transactions)
    val budgetInsights = BudgetCoach.insights(state.budgets, state.transactions, today)
    val overallBudget = state.budgets.firstOrNull {
        it.categoryId == ExpenseCategories.OVERALL_BUDGET
    }
    val overallInsight = budgetInsights.firstOrNull {
        it.categoryId == ExpenseCategories.OVERALL_BUDGET
    }
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryPeriod.entries.forEach { option ->
                AppChip(
                    label = option.label,
                    selected = selectedPeriod == option,
                    onClick = { period = option.name },
                )
            }
        }

        AnimatedContent(
            targetState = summary,
            transitionSpec = {
                if (animationsEnabled) {
                    (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 10 }) togetherWith
                        fadeOut(tween(160))
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            label = "cash-flow-period",
        ) { animatedSummary ->
            CashFlowHero(
                incomeMinor = animatedSummary.incomeMinor,
                expenseMinor = animatedSummary.expenseMinor,
                cashFlowMinor = animatedSummary.netCashFlowMinor,
                periodLabel = selectedPeriod.label,
                hideAmounts = hideAmounts,
                onToggleAmounts = onToggleAmounts,
            )
        }

        IncomeExpenseChart(
            incomeMinor = summary.incomeMinor,
            expenseMinor = summary.expenseMinor,
            hideAmounts = hideAmounts,
        )
        CategorySpendingChart(
            transactions = transactions,
            hideAmounts = hideAmounts,
        )

        SectionHeader("Monthly budget")
        if (overallBudget == null) {
            EmptyState(
                title = "Give your spending a target",
                message = "Set a monthly budget and keep an eye on your progress.",
                actionLabel = "Set a budget",
                onAction = onAddBudget,
            )
        } else {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Spent this month",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            if (hideAmounts) "•••• of ••••" else
                                "${Money.formatMinorUnits(overallInsight?.spentMinor ?: 0L)} of ${Money.formatMinorUnits(overallBudget.limitMinor)}",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    LinearProgressIndicator(
                        progress = {
                            overallInsight?.let {
                                (it.spentMinor.toFloat() / it.limitMinor).coerceIn(0f, 1f)
                            } ?: 0f
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        when (overallInsight?.status) {
                            com.example.expense_tracker.data.BudgetStatus.OVER_BUDGET ->
                                "You’re over your monthly target."
                            com.example.expense_tracker.data.BudgetStatus.AT_LIMIT ->
                                "You’ve reached your monthly target."
                            com.example.expense_tracker.data.BudgetStatus.PROJECTED_OVER ->
                                "Current pace may exceed your monthly target."
                            else -> "Monthly spending, after refunds"
                        },
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
            hideAmounts = hideAmounts,
        )

        SectionHeader(
            title = "Recent transactions",
            trailing = {
                TextButton(onClick = onViewAll) { Text("View all") }
            },
        )
        val recent = state.transactions.take(4)
        if (recent.isEmpty()) {
            EmptyState(
                title = "No transactions yet",
                message = "Add your first income or expense to see your cash flow here.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recent.forEach { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onClick = { onOpenTransaction(transaction) },
                        hideAmount = hideAmounts,
                    )
                }
            }
        }
    }
}

@Composable
private fun CashFlowCard(summary: FinancialSummary, hideAmounts: Boolean) {
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
            AmountText(
                amountMinor = summary.netCashFlowMinor,
                hideAmount = hideAmounts,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Calculated from recorded transactions · not a bank balance",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    amount: Long,
    modifier: Modifier,
    accent: Color,
    hideAmount: Boolean,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AmountText(
                amountMinor = amount,
                hideAmount = hideAmount,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = accent,
            )
        }
    }
}

@Composable
private fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    hideAmounts: Boolean,
    onOpenTransaction: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    onAddTransaction: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var appliedSearch by remember { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(TransactionTypeFilter.ALL.name) }
    var selectedDate by rememberSaveable { mutableStateOf(TransactionDateFilter.ALL.name) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val isSearchFocused by interactionSource.collectIsFocusedAsState()
    val searchSurface by animateColorAsState(
        targetValue = if (isSearchFocused) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "transaction-search-focus",
    )
    val today = LocalDate.now()

    LaunchedEffect(search) {
        delay(250)
        appliedSearch = search
    }

    val types = TransactionTypeFilter.entries
    val dateFilters = TransactionDateFilter.entries
    val categories: List<String?> = listOf(null) + transactions
        .map { it.categoryId }
        .distinct()
        .sorted()
    val filter = TransactionFilter(
        query = appliedSearch,
        type = TransactionTypeFilter.valueOf(selectedType),
        date = TransactionDateFilter.valueOf(selectedDate),
        category = selectedCategory,
    )
    val filtered = transactions.filter { filter.matches(it, today) }
    val filtersActive = search.isNotBlank() ||
        selectedType != TransactionTypeFilter.ALL.name ||
        selectedDate != TransactionDateFilter.ALL.name ||
        selectedCategory != null
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader("Your activity")
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(FintechShapes.control),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = searchSurface,
                unfocusedContainerColor = searchSurface,
                focusedBorderColor = FintechColors.accentTeal,
            ),
            interactionSource = interactionSource,
            label = { Text("Search merchant or description") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (search.isNotEmpty()) {
                {
                    IconButton(onClick = { search = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            } else {
                null
            },
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            types.forEach { type ->
                AppChip(
                    label = type.label,
                    selected = type.name == selectedType,
                    onClick = { selectedType = type.name },
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            dateFilters.forEach { date ->
                AppChip(
                    label = date.label,
                    selected = date.name == selectedDate,
                    onClick = { selectedDate = date.name },
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            categories.forEach { category ->
                AppChip(
                    label = category ?: "All categories",
                    selected = category == selectedCategory,
                    onClick = { selectedCategory = category },
                )
            }
        }
        if (filtered.isEmpty()) {
            EmptyState(
                modifier = Modifier.weight(1f),
                title = if (transactions.isEmpty()) "Your history starts here" else "No transactions found",
                message = if (transactions.isEmpty()) {
                    "Add your first transaction to start building your history."
                } else {
                    "No recorded transactions match this search and filter combination."
                },
                actionLabel = if (transactions.isEmpty()) "Add transaction"
                    else if (filtersActive) "Clear filters" else null,
                onAction = if (transactions.isEmpty()) onAddTransaction else if (filtersActive) {
                    {
                        search = ""
                        appliedSearch = ""
                        selectedType = TransactionTypeFilter.ALL.name
                        selectedDate = TransactionDateFilter.ALL.name
                        selectedCategory = null
                    }
                } else {
                    null
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
                        hideAmount = hideAmounts,
                        modifier = Modifier.animateItem(
                            fadeInSpec = if (animationsEnabled) tween(180) else null,
                            placementSpec = if (animationsEnabled) tween(220) else null,
                            fadeOutSpec = if (animationsEnabled) tween(120) else null,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsScreen(
    transactions: List<TransactionEntity>,
    hideAmounts: Boolean,
) {
    var selectedPeriod by rememberSaveable { mutableStateOf(SummaryPeriod.THIS_MONTH.name) }
    val period = SummaryPeriod.valueOf(selectedPeriod)
    val periodTransactions = transactions.filter {
        it.isInPeriod(period, LocalDate.now())
    }
    val summary = calculateFinancialSummary(periodTransactions)
    val totalSpend = (summary.expenseMinor - summary.refundMinor).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Analytics overview")
        Text(
            "A clear view of where your money goes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(SummaryPeriod.THIS_WEEK, SummaryPeriod.THIS_MONTH).forEach { option ->
                AppChip(
                    label = option.label,
                    selected = period == option,
                    onClick = { selectedPeriod = option.name },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                "Income",
                summary.incomeMinor,
                Modifier.weight(1f),
                FintechColors.income,
                hideAmounts,
            )
            MetricCard(
                "Net spending",
                totalSpend,
                Modifier.weight(1f),
                FintechColors.expense,
                hideAmounts,
            )
        }
        IncomeExpenseChart(
            incomeMinor = summary.incomeMinor,
            expenseMinor = summary.expenseMinor,
            hideAmounts = hideAmounts,
        )
        SpendingTrendChart(transactions = transactions)
        CategorySpendingChart(
            transactions = periodTransactions,
            hideAmounts = hideAmounts,
        )
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionHeader("How calculations work")
                Text(
                    "Transfers are excluded from income and spending. Refunds and reversals reduce net spending and increase cash flow. Excluded, pending, and failed transactions are not counted.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun BudgetsScreen(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    hideAmounts: Boolean,
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(
            "Monthly budget",
            trailing = {
                FilledTonalButton(onClick = onAddBudget) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add")
                }
            },
        )
        val overallBudget = budgets.firstOrNull {
            it.categoryId == ExpenseCategories.OVERALL_BUDGET
        }
        if (overallBudget == null) {
            EmptyState(
                title = "Start with a monthly target",
                message = "Choose an overall limit and we'll help you keep spending on track.",
                actionLabel = "Set overall budget",
                onAction = onAddBudget,
            )
        } else {
            BudgetUsageCard(
                spent = overallSpent,
                limit = overallBudget.limitMinor,
                hideAmounts = hideAmounts,
            )
        }
        BudgetCoachPanel(
            insights = budgetInsights,
            transactions = transactions,
            today = today,
            hideAmounts = hideAmounts,
        )
        val categoryBudgets = budgets.filter {
            it.categoryId != ExpenseCategories.OVERALL_BUDGET
        }
        if (categoryBudgets.isNotEmpty()) {
            SectionHeader("Category budgets")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categoryBudgets.forEach { budget ->
                    val spent = if (budget.categoryId == ExpenseCategories.OVERALL_BUDGET) {
                        overallSpent
                    } else {
                        categorySpending[budget.categoryId] ?: 0L
                    }
                    BudgetCard(
                        budget = budget,
                        spent = spent,
                        hideAmounts = hideAmounts,
                        onRemove = { onRemoveBudget(budget.categoryId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetUsageCard(spent: Long, limit: Long, hideAmounts: Boolean) {
    val progress = (spent.toFloat() / limit).coerceIn(0f, 1f)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Your monthly budget",
                style = MaterialTheme.typography.titleMedium,
            )
            Box(
                modifier = Modifier.size(174.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    color = if (spent >= limit) FintechColors.expense
                    else MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("SPENT", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AmountText(
                        amountMinor = spent,
                        hideAmount = hideAmounts,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("MONTHLY LIMIT", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AmountText(
                        amountMinor = limit,
                        hideAmount = hideAmounts,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(
    budget: BudgetEntity,
    spent: Long,
    hideAmounts: Boolean,
    onRemove: () -> Unit,
) {
    val label = if (budget.categoryId == ExpenseCategories.OVERALL_BUDGET) {
        "Overall monthly budget"
    } else {
        budget.categoryId
    }
    val progress = (spent.toFloat() / budget.limitMinor).coerceIn(0f, 1f)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(label, Modifier.size(42.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AmountText(
                            amountMinor = spent,
                            hideAmount = hideAmounts,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(" of ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AmountText(
                            amountMinor = budget.limitMinor,
                            hideAmount = hideAmounts,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove $label")
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(FintechShapes.pill)),
                color = if (spent >= budget.limitMinor) FintechColors.expense
                else MaterialTheme.colorScheme.primary,
            )
            if (spent > budget.limitMinor) {
                Text("Over budget", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    transactions: List<TransactionEntity>,
    budgetCount: Int,
    hideAmounts: Boolean,
    notificationAccessEnabled: Boolean,
    onManageBudgets: () -> Unit,
    onManageNotificationAccess: () -> Unit,
) {
    val summary = calculateFinancialSummary(transactions)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column {
                        Text(
                            "Local profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Your personal finance overview",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ProfileMetric("Transactions", transactions.size.toString())
                    ProfileMetric("Budgets", budgetCount.toString())
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Net cash flow",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AmountText(
                            amountMinor = summary.netCashFlowMinor,
                            hideAmount = hideAmounts,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }

        ProfileSettingsGroup("Financial settings") {
            ProfileSettingRow(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Base currency",
                subtitle = "Indian Rupee (INR · ₹)",
            )
            ProfileSettingRow(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Monthly budget limit",
                subtitle = if (budgetCount == 0) {
                    "No budgets set"
                } else {
                    "$budgetCount ${if (budgetCount == 1) "budget" else "budgets"} configured"
                },
                actionLabel = "Manage",
                onClick = onManageBudgets,
            )
            ProfileSettingRow(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Linked bank accounts",
                subtitle = "Bank accounts are not linked. Transaction alerts can be enabled below.",
            )
        }

        ProfileSettingsGroup("Preferences & security") {
            ProfileSettingRow(
                icon = Icons.Default.Settings,
                title = "Dark mode",
                subtitle = "Follows your device appearance setting",
            )
            ProfileSettingRow(
                icon = Icons.Default.Settings,
                title = "Biometric lock",
                subtitle = "App lock is not available yet",
            )
            ProfileSettingRow(
                icon = Icons.Default.Settings,
                title = "Automatic transaction detection",
                subtitle = if (notificationAccessEnabled) {
                    "Active. Supported transaction alerts are checked locally; the SMS inbox is never read."
                } else {
                    "Off. Optional notification access; the SMS inbox is never read."
                },
                actionLabel = if (notificationAccessEnabled) "Manage" else "Enable",
                onClick = onManageNotificationAccess,
            )
        }

        ProfileSettingsGroup("Data & cloud sync") {
            ProfileSettingRow(
                icon = Icons.Default.Settings,
                title = "On-device storage",
                subtitle = "Transactions and budgets are stored locally. Cloud sync is not configured.",
            )
        }

        ProfileSettingsGroup("Support & legal") {
            ProfileSettingRow(
                icon = Icons.Default.Settings,
                title = "Privacy",
                subtitle = "The app does not access banking credentials or your SMS inbox. Optional detection reads supported payment notifications only.",
            )
            ProfileSettingRow(
                icon = Icons.Default.Settings,
                title = "Account balance",
                subtitle = "Cash flow is calculated from recorded transactions; it is not a verified bank balance.",
            )
        }
    }
}

@Composable
private fun ProfileMetric(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ProfileSettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title)
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun ProfileSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (actionLabel != null) {
            Text(
                actionLabel,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    hideAmount: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isCredit = transaction.type == TransactionType.INCOME ||
        transaction.type == TransactionType.REFUND ||
        transaction.type == TransactionType.REVERSAL
    val sign = if (isCredit) "+" else if (transaction.type == TransactionType.EXPENSE) "−" else ""
    val amountColor = if (isCredit) {
        com.example.expense_tracker.ui.theme.FintechColors.income
    } else {
        com.example.expense_tracker.ui.theme.FintechColors.expense
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 13.dp, bottom = 13.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIcon(transaction.categoryId)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.merchant?.takeIf { it.isNotBlank() } ?: transaction.description,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${transaction.categoryId} · ${transaction.type.label()} · ${transaction.paymentMethod.label()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sign.isNotEmpty() && !hideAmount) {
                        Text(sign, color = amountColor, fontWeight = FontWeight.SemiBold)
                    }
                    AmountText(
                        amountMinor = transaction.amountMinor,
                        hideAmount = hideAmount,
                        style = MaterialTheme.typography.titleSmall,
                        color = amountColor,
                    )
                }
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
    hideAmount: Boolean,
    onDismiss: () -> Unit,
    onToggleExcluded: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(FintechShapes.largeCard),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    transaction.merchant?.takeIf { it.isNotBlank() } ?: transaction.description,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "${transaction.type.label()} · ${transaction.categoryId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sign = when (transaction.type) {
                        TransactionType.INCOME, TransactionType.REFUND, TransactionType.REVERSAL -> "+"
                        TransactionType.EXPENSE -> "−"
                        TransactionType.TRANSFER -> ""
                    }
                    val color = if (sign == "+") {
                        com.example.expense_tracker.ui.theme.FintechColors.income
                    } else {
                        com.example.expense_tracker.ui.theme.FintechColors.expense
                    }
                    Text(sign, color = color, style = MaterialTheme.typography.headlineMedium)
                    AmountText(
                        amountMinor = transaction.amountMinor,
                        hideAmount = hideAmount,
                        style = MaterialTheme.typography.headlineMedium,
                        color = color,
                    )
                }
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

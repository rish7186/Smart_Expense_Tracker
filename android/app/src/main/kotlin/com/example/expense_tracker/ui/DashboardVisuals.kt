package com.example.expense_tracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expense_tracker.data.ExpenseCategories
import com.example.expense_tracker.data.Money
import com.example.expense_tracker.data.TransactionEntity
import com.example.expense_tracker.data.TransactionStatus
import com.example.expense_tracker.data.TransactionType
import com.example.expense_tracker.ui.components.AmountText
import com.example.expense_tracker.ui.components.AppCard
import com.example.expense_tracker.ui.components.CategoryIcon
import com.example.expense_tracker.ui.components.SectionHeader
import com.example.expense_tracker.ui.theme.FintechColors
import com.example.expense_tracker.ui.theme.FintechShapes
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun CashFlowHero(
    incomeMinor: Long,
    expenseMinor: Long,
    cashFlowMinor: Long,
    periodLabel: String,
    hideAmounts: Boolean,
    onToggleAmounts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FintechShapes.largeCard),
        colors = CardDefaults.cardColors(containerColor = FintechColors.background),
    ) {
        Box {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(FintechColors.background, Color(0xFF121D37)),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(FintechColors.accentTeal.copy(alpha = 0.23f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.12f),
                        radius = size.width * 0.62f,
                    ),
                    radius = size.width * 0.62f,
                    center = Offset(size.width * 0.88f, size.height * 0.12f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(FintechColors.accentBlue.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 0.04f, size.height * 1.08f),
                        radius = size.width * 0.7f,
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.04f, size.height * 1.08f),
                )
                val curve = Path().apply {
                    moveTo(size.width * 0.45f, size.height * 1.08f)
                    cubicTo(
                        size.width * 0.62f, size.height * 0.2f,
                        size.width * 0.74f, size.height * 1.2f,
                        size.width * 1.05f, size.height * 0.28f,
                    )
                }
                drawPath(
                    path = curve,
                    color = FintechColors.accentTeal.copy(alpha = 0.22f),
                    style = Stroke(width = size.minDimension * 0.006f, cap = StrokeCap.Round),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "NET CASH FLOW",
                            style = MaterialTheme.typography.labelMedium,
                            color = FintechColors.textSecondary,
                        )
                        Text(
                            periodLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = FintechColors.textSecondary,
                        )
                    }
                    IconButton(onClick = onToggleAmounts) {
                        Icon(
                            imageVector = if (hideAmounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (hideAmounts) "Show amounts" else "Hide amounts",
                            tint = FintechColors.textPrimary,
                        )
                    }
                }
                AmountText(
                    amountMinor = cashFlowMinor,
                    hideAmount = hideAmounts,
                    description = "Net cash flow",
                    style = MaterialTheme.typography.displaySmall,
                    color = FintechColors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    HeroMetric(
                        label = "INCOME",
                        amountMinor = incomeMinor,
                        hideAmount = hideAmounts,
                        accent = FintechColors.income,
                        modifier = Modifier.weight(1f),
                    )
                    HeroMetric(
                        label = "EXPENSES",
                        amountMinor = expenseMinor,
                        hideAmount = hideAmounts,
                        accent = FintechColors.expense,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    amountMinor: Long,
    hideAmount: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = FintechColors.textSecondary)
        AmountText(
            amountMinor = amountMinor,
            hideAmount = hideAmount,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
        )
    }
}

@Composable
fun IncomeExpenseChart(
    incomeMinor: Long,
    expenseMinor: Long,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader("Income vs expenses")
            val maximum = maxOf(incomeMinor, expenseMinor, 1L)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .semantics {
                        contentDescription = if (hideAmounts) {
                            "Income and expenses chart. Amounts hidden."
                        } else {
                            "Income ${Money.formatMinorUnits(incomeMinor)}, " +
                                "expenses ${Money.formatMinorUnits(expenseMinor)}"
                        }
                    },
            ) {
                val baseline = size.height - 8.dp.toPx()
                drawLine(
                    color = FintechColors.textSecondary.copy(alpha = 0.35f),
                    start = Offset(size.width * 0.12f, baseline),
                    end = Offset(size.width * 0.88f, baseline),
                    strokeWidth = 1.dp.toPx(),
                )
                val barWidth = size.width * 0.2f
                val maxHeight = size.height * 0.78f
                val incomeHeight = (incomeMinor.toFloat() / maximum) * maxHeight
                val expenseHeight = (expenseMinor.toFloat() / maximum) * maxHeight
                drawRoundRect(
                    color = FintechColors.income,
                    topLeft = Offset(size.width * 0.28f, baseline - incomeHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, incomeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                )
                drawRoundRect(
                    color = FintechColors.expense,
                    topLeft = Offset(size.width * 0.58f, baseline - expenseHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, expenseHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ChartLegend("Income", incomeMinor, hideAmounts, FintechColors.income, Modifier.weight(1f))
                ChartLegend("Expenses", expenseMinor, hideAmounts, FintechColors.expense, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChartLegend(
    label: String,
    amountMinor: Long,
    hideAmounts: Boolean,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
            Text(
                label,
                modifier = Modifier.padding(start = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AmountText(
            amountMinor = amountMinor,
            hideAmount = hideAmounts,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun CategorySpendingChart(
    transactions: List<TransactionEntity>,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
) {
    val allTotals = transactions.asSequence()
        .filter {
            !it.isExcluded &&
                it.status == TransactionStatus.COMPLETED &&
                (it.type == TransactionType.EXPENSE ||
                    it.type == TransactionType.REFUND ||
                    it.type == TransactionType.REVERSAL)
        }
        .groupBy { it.categoryId }
        .mapValues { (_, entries) ->
            entries.sumOf {
                if (it.type == TransactionType.EXPENSE) it.amountMinor else -it.amountMinor
            }.coerceAtLeast(0L)
        }
        .filterValues { it > 0L }
        .toList()
        .sortedByDescending { it.second }
    val totals = if (allTotals.size > 5) {
        allTotals.take(4) + ("Other categories" to allTotals.drop(4).sumOf { it.second })
    } else {
        allTotals
    }
    val chartColors = listOf(
        FintechColors.accentBlue,
        FintechColors.income,
        Color(0xFFF3AE50),
        FintechColors.expense,
        Color(0xFF42A9C7),
    )

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader("Spending by category")
            if (totals.isEmpty()) {
                Text(
                    "Completed expenses will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val total = totals.sumOf { it.second }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(176.dp).semantics {
                        contentDescription = "Spending distribution across ${totals.size} categories"
                    }) {
                        var startAngle = -90f
                        val inset = 11.dp.toPx()
                        val ringSize = androidx.compose.ui.geometry.Size(
                            width = size.width - inset * 2,
                            height = size.height - inset * 2,
                        )
                        totals.forEachIndexed { index, (_, amount) ->
                            val sweep = amount.toFloat() / total * 360f
                            drawArc(
                                color = chartColors[index % chartColors.size],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = ringSize,
                                style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Butt),
                            )
                            startAngle += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            totals.first().first,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        AmountText(
                            amountMinor = totals.first().second,
                            hideAmount = hideAmounts,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text("top category", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                totals.forEachIndexed { index, (category, amount) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(50))
                                .background(chartColors[index % chartColors.size]),
                        )
                        Text(
                            category,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 9.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "${(amount.toDouble() / total * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        AmountText(
                            amountMinor = amount,
                            hideAmount = hideAmounts,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingTrendChart(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val dailyTotals = days.map { day ->
        transactions.asSequence()
            .filter {
                !it.isExcluded &&
                    it.status == TransactionStatus.COMPLETED &&
                    (it.type == TransactionType.EXPENSE ||
                        it.type == TransactionType.REFUND ||
                        it.type == TransactionType.REVERSAL) &&
                    java.time.Instant.ofEpochMilli(it.transactionDateEpochMillis)
                        .atZone(zone)
                        .toLocalDate() == day
            }
            .sumOf {
                if (it.type == TransactionType.EXPENSE) it.amountMinor else -it.amountMinor
            }
            .coerceAtLeast(0L)
    }
    val maximum = dailyTotals.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader("Spending trend")
            Text(
                "Your daily spending over the last 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .semantics { contentDescription = "Daily spending trend for the last seven days" },
            ) {
                val left = 8.dp.toPx()
                val right = size.width - left
                val top = 12.dp.toPx()
                val bottom = size.height - 8.dp.toPx()
                repeat(3) { row ->
                    val y = top + (bottom - top) * row / 2f
                    drawLine(
                        color = guideColor,
                        start = Offset(left, y),
                        end = Offset(right, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                val points = dailyTotals.mapIndexed { index, amount ->
                    Offset(
                        x = left + (right - left) * index / (dailyTotals.lastIndex.coerceAtLeast(1)),
                        y = bottom - (amount.toFloat() / maximum) * (bottom - top),
                    )
                }
                if (points.size > 1) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, bottom)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, bottom)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(FintechColors.accentBlue.copy(alpha = 0.22f), Color.Transparent),
                            startY = top,
                            endY = bottom,
                        ),
                    )
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = linePath,
                        color = FintechColors.accentBlue,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                    points.forEachIndexed { index, point ->
                        if (index == points.lastIndex) {
                            drawCircle(
                                color = FintechColors.accentBlue.copy(alpha = 0.18f),
                                radius = 9.dp.toPx(),
                                center = point,
                            )
                            drawCircle(
                                color = FintechColors.accentBlue,
                                radius = 4.dp.toPx(),
                                center = point,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                days.forEach { day ->
                    Text(
                        day.dayOfWeek.name.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

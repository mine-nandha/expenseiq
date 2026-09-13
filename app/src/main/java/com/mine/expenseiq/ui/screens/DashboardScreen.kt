package com.mine.expenseiq.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mine.expenseiq.data.model.*
import com.mine.expenseiq.viewmodel.ExpenseViewModel
import com.mine.expenseiq.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

private fun fmtAmount(amount: Double): String =
    "₹${String.format(Locale.getDefault(), "%,.0f", amount)}"

// Scheme-aware ink: the app's accents are too light for white paper, too dark for slate-800.
@Composable
private fun incomeInk(): Color =
    if (isSystemInDarkTheme()) BentoAccentGreen else IncomeGreenDeep

@Composable
private fun warningInk(): Color =
    if (isSystemInDarkTheme()) BentoAccentOrange else Color(0xFFC2410C)

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToTab: (Int) -> Unit,
    onEditTransaction: (ExpenseTransaction) -> Unit,
    onQuickLog: (QuickLogSuggestion) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val quickLogSuggestions by viewModel.quickLogSuggestions.collectAsState()

    val currentMonthCalendar = Calendar.getInstance()
    val currentMonth = currentMonthCalendar.get(Calendar.MONTH)
    val currentYear = currentMonthCalendar.get(Calendar.YEAR)
    val monthPeriod = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

    val monthlyTransactions = transactions.filter { tx ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear && tx.tags != "Transfer"
    }

    val monthlyIncome = monthlyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val monthlyExpense = monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val netSavings = monthlyIncome - monthlyExpense
    val totalLimit = budgets.filter { it.period.equals("MONTHLY", ignoreCase = true) }
        .sumOf { it.limitAmount }
    val leftInBudget = totalLimit - monthlyExpense
    val weeksElapsed = (currentMonthCalendar.get(Calendar.DAY_OF_MONTH) / 7.0).coerceAtLeast(1.0)

    val categorySpendMap = monthlyTransactions.filter { it.type == "EXPENSE" }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    var pendingDelete by remember { mutableStateOf<ExpenseTransaction?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 90.dp, top = 12.dp)
    ) {
        // 1. Header: the cash-book page heading
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EXPENSEIQ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onBackground else Color(0xFF475569),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Cash book",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = monthPeriod,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clickable { onNavigateToTab(5) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "IQ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // 2. The monthly slip: one spent figure on a stamped rule
        item {
            MonthlySlip(
                spent = monthlyExpense,
                income = monthlyIncome,
                saved = netSavings,
                monthPeriod = monthPeriod,
                totalLimit = totalLimit,
                leftInBudget = leftInBudget,
                hasAnyBudget = budgets.isNotEmpty(),
                stampPlayed = viewModel.stampPlayed,
                onStampPlayed = { viewModel.stampPlayed = true },
                onManageBudgets = { onNavigateToTab(2) }
            )
        }

        // 3. Quick Log chits
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Repeat with one tap",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (quickLogSuggestions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickLogSuggestions.take(3).forEach { suggestion ->
                        QuickLogCard(
                            suggestion = suggestion,
                            modifier = Modifier.weight(1f),
                            onClick = { onQuickLog(suggestion) }
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Log a few transactions and repeat them here in one tap.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Recent transactions, written into the ledger
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recent_transactions_bento_block"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { onNavigateToTab(1) }) {
                            Text("See all", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    "No transactions",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No transactions yet. Tap + to log your first entry.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        val recents = transactions.take(4)
                        recents.forEachIndexed { index, tx ->
                            TransactionListItem(
                                transaction = tx,
                                categories = categories,
                                onEdit = { onEditTransaction(tx) },
                                onDelete = { pendingDelete = tx }
                            )
                            if (index < recents.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // 5. Budget tallies, ruled at the end of the page
        item {
            BudgetTallies(
                budgets = budgets,
                categorySpendMap = categorySpendMap,
                weeksElapsed = weeksElapsed,
                onManageBudgets = { onNavigateToTab(2) }
            )
        }
    }

    // Guarded delete: a ledger entry is never dismissed by a plain tap.
    val deletedTx = pendingDelete
    if (deletedTx != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Delete this entry?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This removes ${deletedTx.note.ifBlank { deletedTx.category }} (${fmtAmount(deletedTx.amount)}) from the ledger. It can't be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(deletedTx)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun MonthlySlip(
    spent: Double,
    income: Double,
    saved: Double,
    monthPeriod: String,
    totalLimit: Double,
    leftInBudget: Double,
    hasAnyBudget: Boolean,
    stampPlayed: Boolean,
    onStampPlayed: () -> Unit,
    onManageBudgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val slipAccent = MaterialTheme.colorScheme.primary
    val overAll = totalLimit > 0 && leftInBudget < 0
    val ruleColor = if (overAll) MaterialTheme.colorScheme.error else slipAccent
    val savedColor = if (saved >= 0) incomeInk() else MaterialTheme.colorScheme.error

    // The one orchestrated moment: the slip's figure and rule stamp in once per app launch.
    val figureAlpha = remember { Animatable(0f) }
    val figureRise = remember { Animatable(0f) }
    val ruleBreadth = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (stampPlayed) {
            figureAlpha.snapTo(1f)
            figureRise.snapTo(1f)
            ruleBreadth.snapTo(1f)
        } else {
            onStampPlayed()
            launch { figureAlpha.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) }
            launch { figureRise.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow)) }
            launch { ruleBreadth.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("net_worth_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent this month",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = monthPeriod.substringBefore(" ").ifBlank { monthPeriod },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.graphicsLayer {
                    alpha = figureAlpha.value
                    translationY = (1f - figureRise.value) * 14.dp.toPx()
                }
            ) {
                Text(
                    text = "\u20B9",
                    fontFamily = SpaceGrotesk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = slipAccent,
                    modifier = Modifier.padding(end = 2.dp)
                )
                Text(
                    text = String.format(Locale.getDefault(), "%,.0f", spent),
                    fontFamily = SpaceGrotesk,
                    fontSize = 44.sp,
                    lineHeight = 48.sp,
                    letterSpacing = (-1).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            // The stamp rule the figure rests on
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(3.dp)
                    .graphicsLayer {
                        scaleX = ruleBreadth.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(ruleColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = fmtAmount(income),
                        fontFamily = SpaceGrotesk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = incomeInk()
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Saved this month",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = fmtAmount(saved),
                        fontFamily = SpaceGrotesk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = savedColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val budgetText = when {
                    totalLimit > 0 && leftInBudget >= 0 ->
                        "${fmtAmount(leftInBudget)} left across your budgets this month"
                    totalLimit > 0 ->
                        "${fmtAmount(-leftInBudget)} over across your budgets this month"
                    hasAnyBudget ->
                        "Only weekly budgets set. Set a monthly limit to track totals here."
                    else ->
                        "No budgets set yet. Set one for any category."
                }
                Text(
                    text = budgetText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (overAll) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onManageBudgets) {
                    Text(if (totalLimit > 0) "Manage" else "Set budgets", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun BudgetTallies(
    budgets: List<Budget>,
    categorySpendMap: Map<String, Double>,
    weeksElapsed: Double,
    onManageBudgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budgets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onManageBudgets) {
                    Text("Manage", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (budgets.isEmpty()) {
                Text(
                    text = "No budgets yet. Give a category a monthly limit to track it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            } else {
                budgets.forEachIndexed { index, budget ->
                    BudgetTallyLine(
                        budget = budget,
                        spent = categorySpendMap[budget.categoryName] ?: 0.0,
                        toDateLimit = if (budget.period.equals("WEEKLY", ignoreCase = true)) {
                            budget.limitAmount * weeksElapsed
                        } else {
                            budget.limitAmount
                        }
                    )
                    if (index < budgets.size - 1) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetTallyLine(budget: Budget, spent: Double, toDateLimit: Double) {
    val percent = if (toDateLimit > 0) (spent / toDateLimit) * 100 else 0.0
    val fillFraction = percent.coerceIn(0.0, 100.0) / 100.0
    val barColor = when {
        percent >= 100.0 -> MaterialTheme.colorScheme.error
        percent >= 75.0 -> warningInk()
        else -> MaterialTheme.colorScheme.primary
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = budget.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${fmtAmount(spent)} of ${fmtAmount(toDateLimit)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction.toFloat())
                    .height(6.dp)
                    .background(barColor, RoundedCornerShape(3.dp))
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = when {
                percent >= 100.0 && spent > toDateLimit -> "Over by ${fmtAmount(spent - toDateLimit)}"
                percent >= 100.0 -> "Limit used up"
                else -> "${Math.round(percent)}% used"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = barColor
        )
    }
}

@Composable
fun QuickLogCard(
    suggestion: QuickLogSuggestion,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isExpense = suggestion.type == "EXPENSE"
    val amountColor = if (isExpense) MaterialTheme.colorScheme.onSurface else incomeInk()
    val prefix = if (isExpense) "-" else "+"

    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = warningInk()
                    )
                    Text(
                        text = "${suggestion.frequency}x",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = suggestion.note.ifEmpty { suggestion.category },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$prefix₹${String.format(Locale.getDefault(), "%,.0f", suggestion.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = suggestion.category,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListItem(
    transaction: ExpenseTransaction,
    categories: List<Category>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val categoryDetails = categories.firstOrNull { it.name == transaction.category }
    val themeColorHex = categoryDetails?.color ?: "#757575"
    val themeColor = Color(android.graphics.Color.parseColor(themeColorHex))

    var expandedMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}")
            .clickable { expandedMenu = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isTransfer = transaction.tags == "Transfer"

        // EMOJI / CATEGORY ICON
        val emoji = if (isTransfer) "🔄"
            else categoryDetails?.iconName?.ifBlank { null }
            ?: when (transaction.category.lowercase().trim()) {
                "food", "food & dining", "dining", "cafe", "restaurant", "starcafe" -> "☕"
                "transport", "uber", "taxi", "travel" -> "🚕"
                "shopping", "amazon", "groceries" -> "🛍️"
                "salary", "income" -> "💰"
                "savings", "investment" -> "📈"
                "housing", "rent", "bills" -> "🏠"
                else -> "💸"
            }

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (isTransfer) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.note,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isTransfer) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Transfer",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = sdf.format(Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (transaction.photoUri != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Receipt,
                        "Receipt attached",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            val isExpenses = transaction.type == "EXPENSE"
            val amountColor = if (isTransfer) MaterialTheme.colorScheme.onSurfaceVariant
                else if (isExpenses) MaterialTheme.colorScheme.onSurface
                else incomeInk()
            Text(
                text = if (isTransfer) "₹${String.format(Locale.getDefault(), "%,.0f", transaction.amount)}"
                    else if (isExpenses) "-₹${String.format(Locale.getDefault(), "%,.0f", transaction.amount)}"
                    else "+₹${String.format(Locale.getDefault(), "%,.0f", transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }

        // Dropdown menu trigger
        Box {
            DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Edit, "Edit") },
                    text = { Text("Edit") },
                    onClick = {
                        expandedMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Delete, "Delete") },
                    text = { Text("Delete") },
                    onClick = {
                        expandedMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}
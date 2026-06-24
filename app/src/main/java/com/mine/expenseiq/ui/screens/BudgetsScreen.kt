package com.mine.expenseiq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mine.expenseiq.data.model.Budget
import com.mine.expenseiq.data.model.Category
import com.mine.expenseiq.viewmodel.ExpenseViewModel
import com.mine.expenseiq.ui.theme.*
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: ExpenseViewModel,
    onAddBudgetClick: () -> Unit
) {
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // Aggregate monthly expense calculations dynamically for comparison
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val firstDayOfMonthMs = calendar.timeInMillis

    val monthlyTransactions = transactions.filter { it.date >= firstDayOfMonthMs }
    val totalSpendThisMonth = monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    // Group transaction expenses by primary category names
    val categorySpendMap = monthlyTransactions
        .filter { it.type == "EXPENSE" }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    fun getSpentForBudget(budget: Budget): Double {
        return if (budget.categoryName == "Overall") {
            totalSpendThisMonth
        } else {
            categorySpendMap[budget.categoryName] ?: 0.0
        }
    }

    fun getDaysLeft(period: String): Int {
        val today = Calendar.getInstance()
        val totalDays = when (period.lowercase()) {
            "weekly" -> {
                val endOfWeek = Calendar.getInstance()
                endOfWeek.set(Calendar.DAY_OF_WEEK, endOfWeek.getActualMaximum(Calendar.DAY_OF_WEEK))
                val diff = endOfWeek.timeInMillis - today.timeInMillis
                (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            }
            else -> {
                val endOfMonth = Calendar.getInstance()
                endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
                val diff = endOfMonth.timeInMillis - today.timeInMillis
                (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            }
        }
        return totalDays
    }

    // Determine warning levels across all budgets
    val budgetStates = budgets.map { b ->
        val spent = getSpentForBudget(b)
        val limit = b.limitAmount
        val ratio = if (limit > 0) spent / limit else 0.0
        Triple(b, spent, ratio)
    }

    val overspentCount = budgetStates.count { it.third >= 1.0 }
    val warningCount = budgetStates.count { it.third >= 0.75 && it.third < 1.0 }
    val activeBudgetCount = budgets.size

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddBudgetClick,
                icon = { Icon(Icons.Default.Add, "Configure Budget", tint = MaterialTheme.colorScheme.onPrimary) },
                text = { Text("Set Budget Limit", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier
                    .testTag("add_budget_fab")
                    .padding(bottom = 16.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("budgets_screen")
        ) {
            // Heading Group
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BUDGET HUB",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Limits & Controls",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Overall Indicator Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (overspentCount > 0) MaterialTheme.colorScheme.errorContainer 
                            else if (warningCount > 0) Color(0xFFFEF3C7) 
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (overspentCount > 0) "⚠️ $overspentCount OVER" 
                               else if (warningCount > 0) "⚡ $warningCount WARN" 
                               else "🛡️ SAFE ZONE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (overspentCount > 0) MaterialTheme.colorScheme.error 
                                else if (warningCount > 0) Color(0xFFD97706) 
                                else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // 2. Budget Advisory Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp), clip = false),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "EXPENDITURE ADVISORY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (activeBudgetCount == 0) {
                        Text(
                            text = "No financial control caps are active yet. Set limits to track and limit discretionary spend channels.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    } else {
                        val overallBudget = budgets.firstOrNull { it.categoryName == "Overall" }
                        if (overallBudget != null) {
                            val spent = getSpentForBudget(overallBudget)
                            val remaining = overallBudget.limitAmount - spent
                            val days = getDaysLeft(overallBudget.period)

                            if (remaining > 0) {
                                val dailyCap = remaining / days
                                Text(
                                    text = "To stay within your Total Ledger Budget limit, you can spend up to ₹${String.format(Locale.getDefault(), "%,.0f", dailyCap)} per day for the next $days remaining days.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                            } else {
                                Text(
                                    text = "Alert! You have exceeded your Overall Budget of ₹${overallBudget.limitAmount} by ₹${String.format(Locale.getDefault(), "%,.0f", -remaining)}. Please immediately suspend discretionary expense categories.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error,
                                    lineHeight = 20.sp
                                )
                            }
                        } else {
                            // General categorized tip
                            Text(
                                text = "You have $activeBudgetCount strict target limits set. " +
                                       (if (overspentCount > 0) "Reduce spending on categories displaying red bars to recover your balance."
                                        else "Perfect! Your budget limits are matching up with your real spending flows."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Spent This Month (Gross)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.2f", totalSpendThisMonth)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Short summary stats
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$activeBudgetCount Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 3. Budgets List / Category Section
            Text(
                text = "Target Spend Caps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 10.dp, top = 4.dp)
            )

            if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShieldMoon,
                                contentDescription = "No limits active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Strict Caps Configured",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Setup category limits (Weekly or Monthly) to analyze and trim dynamic spending habits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(budgets, key = { it.id }) { budget ->
                        val spent = getSpentForBudget(budget)
                        val daysLeft = getDaysLeft(budget.period)

                        BudgetProgressCard(
                            budget = budget,
                            spent = spent,
                            daysLeft = daysLeft,
                            categories = categories,
                            onDelete = { viewModel.deleteBudget(budget) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetProgressCard(
    budget: Budget,
    spent: Double,
    daysLeft: Int,
    categories: List<Category>,
    onDelete: () -> Unit
) {
    val limit = budget.limitAmount
    val fraction = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0) else 0.0
    val percentage = if (limit > 0) ((spent / limit) * 100).toInt() else 0

    // Match Color States nicely
    val indicatorColor = when {
        percentage >= 100 -> Color(0xFFEF4444) // Slate red
        percentage >= 80 -> Color(0xFFF59E0B)  // Slate amber
        else -> MaterialTheme.colorScheme.primary                   // Sleek indigo indigo
    }

    val categoryDetails = categories.firstOrNull { it.name == budget.categoryName }
    val themeColorHex = categoryDetails?.color ?: "#4F46E5"
    
    val categoryIcon = when (budget.categoryName.lowercase().trim()) {
        "food", "dining", "groceries", "restaurant" -> Icons.Default.Restaurant
        "transport", "uber", "travel" -> Icons.Default.DirectionsCar
        "shopping", "amazon" -> Icons.Default.ShoppingBag
        "housing", "rent", "bills" -> Icons.Default.Home
        "overall" -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.Category
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_progress_card_${budget.categoryName}")
            .shadow(2.dp, RoundedCornerShape(20.dp), clip = false),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Icon, Name and Frequency badge, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (budget.categoryName == "Overall") "Overall Wallet" else budget.categoryName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        // Period tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = budget.period,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Days tracker instruction
                    Text(
                        text = "$daysLeft days left in cycle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove cap",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { fraction.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                color = indicatorColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cash spend amounts and advice row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${spent.toInt()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " / ₹${limit.toInt()}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
                        )
                    }
                }

                val delta = limit - spent
                val capStyleText = if (delta >= 0.0) {
                    val safeDaily = delta / daysLeft
                    "₹${safeDaily.toInt()}/day left"
                } else {
                    "Overdrawn by ₹${(-delta).toInt()}"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(indicatorColor.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = capStyleText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = indicatorColor
                    )
                }
            }

            // Rollover indicator
            if (budget.isRollover) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Loop,
                        contentDescription = "Rollover active",
                        tint = BentoAccentGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Rollover active: Surplus transfers to subsequent cycle",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

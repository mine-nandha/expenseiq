package com.mine.expenseiq.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mine.expenseiq.data.model.*
import com.mine.expenseiq.viewmodel.ExpenseViewModel
import com.mine.expenseiq.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToTab: (Int) -> Unit,
    onEditTransaction: (ExpenseTransaction) -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // Aggregate summary statistics
    val netWorth = accounts.sumOf { it.balance }
    
    val currentMonthCalendar = Calendar.getInstance()
    val currentMonth = currentMonthCalendar.get(Calendar.MONTH)
    val currentYear = currentMonthCalendar.get(Calendar.YEAR)

    // Helper functions to check monthly numbers
    val monthlyTransactions = transactions.filter { tx ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
    }
    
    val monthlyIncome = monthlyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val monthlyExpense = monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    // Aggregate category-wise spending to check budget excesses
    val categorySpendMap = monthlyTransactions.filter { it.type == "EXPENSE" }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    // Detect active budget warning conditions
    val activeWarnings = budgets.mapNotNull { b ->
        val spent = categorySpendMap[b.categoryName] ?: 0.0
        val percent = if (b.limitAmount > 0) (spent / b.limitAmount) * 100 else 0.0
        if (percent >= 100.0) {
            "Budget Alert: You have exceeded your ${b.period.lowercase()} budget of ₹${b.limitAmount} for ${b.categoryName}!"
        } else if (percent >= 75.0) {
            "Budget Limit Warning: You have reached ${percent.toInt()}% of your ₹${b.limitAmount} budget for ${b.categoryName}."
        } else {
            null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 90.dp, top = 12.dp)
    ) {
        // 1. App Header (Bento Style)
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
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Good morning, Investor!",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
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

        // 2. Bento Hero: Total Spent Gradient Card (Reworked to display spent metrics instead of Net Worth)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("net_worth_card"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = BentoPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BentoPrimary, BentoPrimaryDark)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        val currentMonthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Spending · $currentMonthName",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "MONTHLY FLOW",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.2f", monthlyExpense)}",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val totalLimit = budgets.sumOf { it.limitAmount }
                        val leftInBudget = totalLimit - monthlyExpense

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "INCOME THIS MONTH",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.0f", monthlyIncome)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            val netSavings = monthlyIncome - monthlyExpense
                            val savingsColor = if (netSavings >= 0) BentoAccentGreen else Color(0xFFF87171)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "SAVINGS LEVEL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                                Text(
                                    text = if (netSavings >= 0) "₹${String.format(Locale.getDefault(), "%,.0f", netSavings)} saved" 
                                           else "₹${String.format(Locale.getDefault(), "%,.0f", -netSavings)} deficit",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = savingsColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        val budgetStatusText = if (totalLimit > 0) {
                            if (leftInBudget >= 0) {
                                "₹${String.format(Locale.getDefault(), "%,.0f", leftInBudget)} safe threshold budget remaining"
                            } else {
                                "Overdrawing limit boundaries by ₹${String.format(Locale.getDefault(), "%,.0f", -leftInBudget)}!"
                            }
                        } else {
                            "Setup target limits in Budgets to control cash leakages"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (totalLimit > 0 && leftInBudget < 0) Color.Red else BentoAccentGreen, 
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = budgetStatusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 3. active warning announcements (Dynamic Alerts)
        if (activeWarnings.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Budget Alert", tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Budget Warnings", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        activeWarnings.forEach { warning ->
                            Text("• $warning", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 2.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        // 4. Bento Side-by-Side Cells: Budget Progress & Accounts Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Bento Card: Unified Budget Health Tracking
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp)
                        .clickable { onNavigateToTab(2) },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BUDGET HEALTH",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${budgets.size} CAPS",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (budgets.isEmpty()) {
                                Text(
                                    text = "Unrestricted",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Configuring target boundaries will help prevent overspending leakages.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            } else {
                                val totalBudgetLimitsCombined = budgets.sumOf { it.limitAmount }
                                val totalBudgetSpentCombined = budgets.sumOf { b ->
                                    if (b.categoryName == "Overall") {
                                        monthlyExpense
                                    } else {
                                        categorySpendMap[b.categoryName] ?: 0.0
                                    }
                                }
                                val overspentCount = budgets.count { b ->
                                    val spent = if (b.categoryName == "Overall") monthlyExpense else (categorySpendMap[b.categoryName] ?: 0.0)
                                    spent >= b.limitAmount
                                }
                                
                                val fractionCombined = if (totalBudgetLimitsCombined > 0) (totalBudgetSpentCombined / totalBudgetLimitsCombined).coerceIn(0.0, 1.0) else 0.0
                                val percentCombined = (fractionCombined * 100).toInt()
                                
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.0f", totalBudgetSpentCombined)} Spent",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "of ₹${String.format(Locale.getDefault(), "%,.0f", totalBudgetLimitsCombined)} limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                val indicatorColor = when {
                                    percentCombined >= 100 -> Color(0xFFEF4444)
                                    percentCombined >= 80 -> Color(0xFFF59E0B)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                
                                LinearProgressIndicator(
                                    progress = { fractionCombined.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = indicatorColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = StrokeCap.Round
                                )
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val isBreached = overspentCount > 0
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (isBreached) Color(0xFFEF4444) else BentoAccentGreen, CircleShape)
                                    )
                                    Text(
                                        text = if (isBreached) "$overspentCount Limit Overdrawn" else "All targets healthy",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBreached) Color(0xFFEF4444) else BentoAccentGreen
                                    )
                                }
                            }
                        }
                        
                        if (budgets.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Set limit cap",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Right Bento Card: Primary Bank Account & Balance Representation
                val primaryAccount = accounts.maxByOrNull { it.balance }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp)
                        .clickable { onNavigateToTab(3) },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (primaryAccount != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (primaryAccount == null) {
                            Column {
                                Text(
                                    text = "ACCOUNTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No registries setup.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Add account",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "VIP ACCOUNT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                        letterSpacing = 1.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = primaryAccount.name.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.0f", primaryAccount.balance)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            val otherAccountsCount = accounts.size - 1
                            Text(
                                text = if (otherAccountsCount > 0) "+$otherAccountsCount other records" else "Primary deposit active",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }



        // 6. Navigation Quick Shortcut Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShortcutCard(
                    title = "Budgets", 
                    icon = Icons.Default.Timer, 
                    color = BentoAccentOrange, 
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigateToTab(2)
                }
                ShortcutCard(
                    title = "Accounts", 
                    icon = Icons.Default.CreditCard, 
                    color = BentoAccentBlue, 
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigateToTab(3)
                }
                ShortcutCard(
                    title = "Report", 
                    icon = Icons.Default.PieChart, 
                    color = BentoAccentGreen, 
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigateToTab(4)
                }
            }
        }

        // 7. Recent Transactions Bento Card Block
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recent_transactions_bento_block"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToTab(1) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

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
                                    "No transactions logged yet.", 
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
                                onDelete = { viewModel.deleteTransaction(tx) }
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
    }
}

@Composable
fun ShortcutCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(85.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title, 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
        // EMOJI / CATEGORY ICON
        val emoji = categoryDetails?.iconName?.ifBlank { null }
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
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
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
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
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
            Text(
                text = if (isExpenses) "-₹${String.format(Locale.getDefault(), "%,.0f", transaction.amount)}" else "+₹${String.format(Locale.getDefault(), "%,.0f", transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = if (isExpenses) MaterialTheme.colorScheme.onSurface else BentoAccentGreen
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

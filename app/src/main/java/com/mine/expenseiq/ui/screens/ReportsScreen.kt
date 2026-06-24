package com.mine.expenseiq.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mine.expenseiq.data.model.*
import com.mine.expenseiq.viewmodel.ExpenseViewModel
import com.mine.expenseiq.utils.CsvExporter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ExpenseViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val context = LocalContext.current

    // Period selector state
    val calendar = Calendar.getInstance()
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Filter transactions for chosen month + year
    val filteredHistory = remember(transactions, selectedMonth, selectedYear) {
        transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
            cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear
        }
    }

    var showExportDialog by remember { mutableStateOf(false) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            CsvExporter.writeTransactionsToUri(context, uri, filteredHistory)
        }
    }

    val totalIncome = filteredHistory.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = filteredHistory.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    // Group expenses by category
    val categoryExpMap = remember(filteredHistory) {
        filteredHistory.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    // Pie chart slices items data prep
    val pieChartSlices = remember(categoryExpMap, categories) {
        if (totalExpense <= 0) emptyList() else {
            categoryExpMap.map { (catName, amt) ->
                val catObj = categories.firstOrNull { it.name == catName }
                val colorHex = catObj?.color ?: "#9C27B0"
                val decimalColor = Color(android.graphics.Color.parseColor(colorHex))
                PieSlice(
                    name = catName,
                    amount = amt,
                    fraction = amt / totalExpense,
                    color = decimalColor
                )
            }.sortedByDescending { it.amount }
        }
    }

    // Compute weekly spends segment values
    val weeklySpending = remember(filteredHistory) {
        val weekAmounts = DoubleArray(5) { 0.0 }
        filteredHistory.filter { it.type == "EXPENSE" }.forEach { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val weekIndex = ((day - 1) / 7).coerceIn(0, 4)
            weekAmounts[weekIndex] += tx.amount
        }
        weekAmounts.toList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reports_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
    ) {
        // Heading
        item {
            Text(
                text = "Financial Insights",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Date selection filter bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Analysis Cycle: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                
                Box {
                    Button(
                        onClick = { monthDropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("${monthNames[selectedMonth]} $selectedYear")
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = monthDropdownExpanded,
                        onDismissRequest = { monthDropdownExpanded = false }
                    ) {
                        for (i in 0..11) {
                            DropdownMenuItem(
                                text = { Text("${monthNames[i]} $selectedYear") },
                                onClick = {
                                    selectedMonth = i
                                    monthDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Net Summary Row Cards
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnalyticsCompactCard(
                    title = "Total Earned",
                    amount = totalIncome,
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsCompactCard(
                    title = "Total Charged",
                    amount = totalExpense,
                    icon = Icons.Default.TrendingDown,
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Dynamic Savings Intelligence Gauge Badge
        item {
            SavingsIntelligenceCard(income = totalIncome, expense = totalExpense)
        }

        // Custom Canvas Pie Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Category Expense Ratio",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (totalExpense <= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PieChart, "Empty report", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No expenses logged for this month card.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        // Drawing dynamic Pie Chart using Compose Canvas
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                var currentStartAngle = -90f
                                pieChartSlices.forEach { slice ->
                                    val sweep = slice.fraction * 360f
                                    drawArc(
                                        color = slice.color,
                                        startAngle = currentStartAngle,
                                        sweepAngle = sweep.toFloat(),
                                        useCenter = false,
                                        size = Size(size.width, size.height),
                                        style = Stroke(width = 24.dp.toPx())
                                    )
                                    currentStartAngle += sweep.toFloat()
                                }
                            }
                            
                            // Center aggregate text info
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${totalExpense.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Legends table list
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            pieChartSlices.forEach { slice ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(slice.color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(slice.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(
                                        "₹${slice.amount.toInt()} (${(slice.fraction * 100).toInt()}%)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Canvas Bar Chart for Weekly Outflow Trends
        item {
            WeeklySpendingChart(weeklyAmounts = weeklySpending)
        }

        // Smart Budget Advisor Panel
        item {
            SmartAdvisorPanel(
                income = totalIncome,
                expense = totalExpense,
                categoryExpMap = categoryExpMap,
                categories = categories
            )
        }

        // Export Actions trigger details block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Export Statements",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // PDF Export trigger (Real active HTML print to PDF!)
                        Button(
                            onClick = {
                                generatePdfReport(context, monthNames[selectedMonth], selectedYear, filteredHistory, totalIncome, totalExpense)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF", style = MaterialTheme.typography.bodyMedium)
                        }

                        // CSV Export trigger (Uses premium multiple choice export dialog!)
                        Button(
                            onClick = {
                                if (filteredHistory.isEmpty()) {
                                    Toast.makeText(context, "No transactions to export for this period", Toast.LENGTH_SHORT).show()
                                } else {
                                    showExportDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.FileDownload, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        ExportCsvDialog(
            transactions = filteredHistory,
            onDismiss = { showExportDialog = false },
            onSystemSaveClick = {
                exportCsvLauncher.launch("expense_iq_report_${monthNames[selectedMonth]}_$selectedYear.csv")
            },
            context = context
        )
    }
}

@Composable
fun SavingsIntelligenceCard(income: Double, expense: Double) {
    val savings = income - expense
    val savingsRate = if (income > 0) (savings / income * 100).coerceAtLeast(0.0) else 0.0
    
    val badgeColor: Color
    val badgeText: String
    val adviceText: String
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    
    when {
        income == 0.0 && expense == 0.0 -> {
            badgeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            badgeText = "No Activity"
            adviceText = "Log earnings or spends in the Book page to begin automated insights processing."
            icon = Icons.Default.Info
        }
        savingsRate >= 20.0 -> {
            badgeColor = Color(0xFF2E7D32)
            badgeText = "Healthy Surplus (${savingsRate.toInt()}%)"
            adviceText = "Fabulous! You banked ${savingsRate.toInt()}% of your income. Consider routing this into high-yield deposits or investments."
            icon = Icons.Default.CheckCircle
        }
        savingsRate >= 0.0 -> {
            badgeColor = Color(0xFFE65100)
            badgeText = "Balanced Budget (${savingsRate.toInt()}%)"
            adviceText = "You're living safe but savings margins are narrow. Try streamlining minor categories to reach a 20% savings cushion."
            icon = Icons.Default.ThumbUp
        }
        else -> {
            badgeColor = MaterialTheme.colorScheme.error
            badgeText = "Budget Deficit"
            adviceText = "Warning: Outflow exceeded income by ₹${(-savings).toInt()}! Trim luxury spends and cap categories immediately."
            icon = Icons.Default.Warning
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(badgeColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Savings Quotient",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .background(badgeColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = adviceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun WeeklySpendingChart(weeklyAmounts: List<Double>) {
    val maxVal = weeklyAmounts.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Weekly Outflow Distribution",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Track outflow trends over 5 calendar segments this cycle",
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(vertical = 8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    val gridLinesCount = 3
                    val stepHeight = canvasHeight / (gridLinesCount + 1)
                    for (i in 1..gridLinesCount) {
                        val y = i * stepHeight
                        drawLine(
                            color = gridColor.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    val barCount = weeklyAmounts.size
                    val spacing = 16.dp.toPx()
                    val totalSpacing = spacing * (barCount + 1)
                    val barWidth = (canvasWidth - totalSpacing) / barCount
                    
                    weeklyAmounts.forEachIndexed { index, amt ->
                        val barHeightFraction = (amt / maxVal).coerceIn(0.0, 1.0)
                        val barHeight = (canvasHeight - 24.dp.toPx()) * barHeightFraction
                        
                        val x = spacing + index * (barWidth + spacing)
                        val y = canvasHeight - 20.dp.toPx() - barHeight
                        
                        drawRoundRect(
                            color = primaryColor.copy(alpha = if (amt > 0) 1.0f else 0.15f),
                            topLeft = Offset(x, y.toFloat()),
                            size = Size(barWidth, barHeight.toFloat().coerceAtLeast(4f)),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weeklyAmounts.forEachIndexed { index, amt ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (amt > 0) "₹${amt.toInt()}" else "-",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "W${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = labelColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartAdvisorPanel(
    income: Double,
    expense: Double,
    categoryExpMap: Map<String, Double>,
    categories: List<Category>
) {
    val topExpense = categoryExpMap.maxByOrNull { it.value }
    val topExpenseCategory = topExpense?.key
    val topExpenseAmount = topExpense?.value ?: 0.0
    val topExpensePercentage = if (expense > 0) (topExpenseAmount / expense * 100).toInt() else 0
    
    val advisoryTips = remember(income, expense, topExpenseCategory, topExpensePercentage) {
        val list = mutableListOf<String>()
        if (income == 0.0 && expense == 0.0) {
            list.add("Ready to help: Log your first transaction of any category to unlock custom advisory trends!")
            list.add("Budget wisdom: Keep your monthly discretionary spending under 50% of your earnings.")
            list.add("Emergency cushion: Try to stack 3-6 months of stable cash reserves.")
        } else {
            if (topExpenseCategory != null && topExpensePercentage > 0) {
                list.add("Your heaviest cost center is '$topExpenseCategory' taking up $topExpensePercentage% of all spends (₹${topExpenseAmount.toInt()}). Try setting a custom cap in budget!")
            }
            if (expense > income && income > 0.0) {
                list.add("Avoid credit card reliance in months with active deficits. Trim secondary spending categories.")
            }
            if (income > expense && (income - expense) > 0) {
                val surplus = income - expense
                list.add("You have ₹${surplus.toInt()} remaining this cycle. Keep 15% liquid and explore investment options.")
            }
            if (list.size < 3) {
                list.add("Smart habit: Review category guidelines periodically to ensure structured wealth accumulation.")
            }
            list.add("Export bank-ready statements directly using our dynamic printer spooler options below.")
        }
        list
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFBC02D),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Budget Advisor",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                advisoryTips.forEach { tip ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("•", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

data class PieSlice(
    val name: String,
    val amount: Double,
    val fraction: Double,
    val color: Color
)

@Composable
fun AnalyticsCompactCard(
    title: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }

            Text(
                "₹${String.format(Locale.getDefault(), "%,.0f", amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Function to generate and export real physical PDF statements via Android's print spooler
fun generatePdfReport(
    context: Context,
    monthName: String,
    year: Int,
    history: List<ExpenseTransaction>,
    income: Double,
    expense: Double
) {
    if (history.isEmpty()) {
        Toast.makeText(context, "No transactions to export for this period", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        val rowsBuilder = StringBuilder()
        history.forEach { tx ->
            val formattedDate = dateFormat.format(Date(tx.date))
            val typeClass = if (tx.type == "INCOME") "type-income" else "type-expense"
            val typePrefix = if (tx.type == "INCOME") "+" else "-"
            rowsBuilder.append("""
                <tr>
                    <td>$formattedDate</td>
                    <td class="$typeClass">$typePrefix${tx.type}</td>
                    <td>${tx.category}</td>
                    <td>${tx.note}</td>
                    <td>${tx.paymentMode}</td>
                    <td style="font-weight: bold;">₹${tx.amount}</td>
                </tr>
            """.trimIndent())
        }

        val netBalance = income - expense
        val netPrefix = if (netBalance >= 0) "+" else ""
        val netColor = if (netBalance >= 0) "#4CAF50" else "#F44336"

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8">
            <title>ExpenseIQ Monthly Report - $monthName $year</title>
            <style>
              body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 30px; color: #333; line-height: 1.4; }
              header { border-bottom: 3px solid #2196F3; padding-bottom: 12px; margin-bottom: 25px; }
              .header-title { display: flex; justify-content: space-between; align-items: center; }
              h1 { color: #2196F3; font-size: 28px; margin: 0; font-weight: bold; }
              .doc-type { background-color: #2196F3; color: white; padding: 4px 12px; border-radius: 4px; font-weight: bold; font-size: 14px; text-transform: uppercase; }
              .summary-container { display: table; width: 100%; border-spacing: 12px; margin: 20px -12px; }
              .card { display: table-cell; padding: 16px; border-radius: 8px; background-color: #fcfcfc; border: 1px solid #e0e0e0; width: 33.33%; box-sizing: border-box; }
              .card h3 { margin: 0 0 8px 0; color: #666; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; }
              .card .amount { font-size: 22px; font-weight: bold; }
              .income-card { border-left: 5px solid #4CAF50; }
              .expense-card { border-left: 5px solid #F44336; }
              .savings-card { border-left: 5px solid $netColor; }
              h2 { font-size: 18px; margin-top: 30px; margin-bottom: 15px; color: #2196F3; border-bottom: 1px solid #eee; padding-bottom: 8px; }
              table { width: 100%; border-collapse: collapse; margin-top: 15px; }
              th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #eee; font-size: 13px; }
              th { background-color: #2196F3; color: white; text-transform: uppercase; font-size: 11px; font-weight: bold; border: none; }
              tr:nth-child(even) { background-color: #f9f9f9; }
              .type-income { color: #4CAF50; font-weight: bold; }
              .type-expense { color: #F44336; font-weight: bold; }
              .footer { margin-top: 50px; text-align: center; font-size: 11px; color: #999; border-top: 1px solid #eee; padding-top: 15px; }
            </style>
            </head>
            <body>
              <header>
                <div class="header-title">
                  <h1>ExpenseIQ Statement</h1>
                  <span class="doc-type">Monthly Insights</span>
                </div>
                <div style="margin-top: 5px; color: #666; font-size: 13px;">Cycle: <strong>$monthName $year</strong> &bull; Total Transactions: <strong>${history.size}</strong></div>
              </header>
              
              <div class="summary-container">
                <div class="card income-card">
                  <h3>Total Income</h3>
                  <div class="amount" style="color: #4CAF50;">₹$income</div>
                </div>
                <div class="card expense-card">
                  <h3>Total Expense</h3>
                  <div class="amount" style="color: #F44336;">₹$expense</div>
                </div>
                <div class="card savings-card">
                  <h3>Net Savings</h3>
                  <div class="amount" style="color: $netColor;">$netPrefix₹$netBalance</div>
                </div>
              </div>
              
              <h2>Transactions History Table</h2>
              <table>
                <thead>
                  <tr>
                    <th>Date & Time</th>
                    <th>Type</th>
                    <th>Category</th>
                    <th>Note / Merchant</th>
                    <th>Account</th>
                    <th>Amount</th>
                  </tr>
                </thead>
                <tbody>
                  $rowsBuilder
                </tbody>
              </table>
              
              <div class="footer">
                Compiled dynamically via <strong>ExpenseIQ Personal Finance Tracker</strong> on $dateFormatted.
              </div>
            </body>
            </html>
        """.trimIndent()

        // Open native Android Print dialog
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post {
            try {
                val webView = android.webkit.WebView(context)
                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        try {
                            val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                            val jobName = "ExpenseIQ_Report_${monthName}_$year"
                            val printAdapter = webView.createPrintDocumentAdapter(jobName)
                            printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to start printing: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            } catch (ex: Exception) {
                Toast.makeText(context, "Failed to load WebView: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to export PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

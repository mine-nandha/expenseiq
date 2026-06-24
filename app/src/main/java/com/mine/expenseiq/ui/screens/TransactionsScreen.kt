package com.mine.expenseiq.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.mine.expenseiq.data.model.*
import com.mine.expenseiq.ui.components.SmsImportDialog
import com.mine.expenseiq.viewmodel.ExpenseViewModel
import com.mine.expenseiq.utils.CsvExporter
import com.mine.expenseiq.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: ExpenseViewModel,
    onEditTransaction: (ExpenseTransaction) -> Unit,
    onAddTransactionClick: () -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val scannedSmsList by viewModel.scannedSmsList.collectAsState()

    var showSmsImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val smsPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val readSmsGranted = permissions[Manifest.permission.READ_SMS] == true
            if (readSmsGranted) {
                viewModel.scanInboxForTransactions()
                showSmsImportDialog = true
            }
        }
    )

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, EXPENSE, INCOME
    var selectedAccountFilter by remember { mutableStateOf("ALL") } // ALL or Name of account
    var selectedSortBy by remember { mutableStateOf("NEWEST") } // NEWEST, OLDEST, HIGHEST, LOWEST

    var selectedDateFilterType by remember { mutableStateOf("ALL") } // ALL, TODAY, THIS_WEEK, THIS_MONTH, CUSTOM
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var showFiltersExpanded by remember { mutableStateOf(false) }

    // Date computation helpers
    fun getStartOfDay(timeMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMs
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getEndOfDay(timeMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMs
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun getStartOfWeek(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getStartOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // Apply filtering and sorting
    val filteredTransactions = remember(
        transactions, 
        searchQuery, 
        selectedTypeFilter, 
        selectedAccountFilter, 
        selectedSortBy,
        selectedDateFilterType,
        customStartDate,
        customEndDate
    ) {
        transactions.filter { tx ->
            // Search text matcher (note, category, or tags)
            val noteMatch = tx.note.contains(searchQuery, ignoreCase = true)
            val categoryMatch = tx.category.contains(searchQuery, ignoreCase = true)
            val tagsMatch = tx.tags.contains(searchQuery, ignoreCase = true)
            val matchesSearch = searchQuery.isEmpty() || noteMatch || categoryMatch || tagsMatch

            // Type filter matcher
            val matchesType = selectedTypeFilter == "ALL" || tx.type == selectedTypeFilter

            // Account filter matcher
            val matchesAccount = selectedAccountFilter == "ALL" || tx.paymentMode == selectedAccountFilter

            // Date filter matcher
            val matchesDate = when (selectedDateFilterType) {
                "TODAY" -> {
                    val start = getStartOfDay()
                    val end = getEndOfDay()
                    tx.date in start..end
                }
                "THIS_WEEK" -> {
                    val start = getStartOfWeek()
                    tx.date >= start
                }
                "THIS_MONTH" -> {
                    val start = getStartOfMonth()
                    tx.date >= start
                }
                "CUSTOM" -> {
                    val start = customStartDate?.let { getStartOfDay(it) } ?: 0L
                    val end = customEndDate?.let { getEndOfDay(it) } ?: Long.MAX_VALUE
                    tx.date in start..end
                }
                else -> true // "ALL"
            }

            matchesSearch && matchesType && matchesAccount && matchesDate
        }.sortedWith { a, b ->
            when (selectedSortBy) {
                "OLDEST" -> a.date.compareTo(b.date)
                "HIGHEST" -> b.amount.compareTo(a.amount)
                "LOWEST" -> a.amount.compareTo(b.amount)
                else -> b.date.compareTo(a.date) // "NEWEST"
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            CsvExporter.writeTransactionsToUri(context, uri, filteredTransactions)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransactionClick,
                icon = { Icon(Icons.Default.Add, "Log Transaction") },
                text = { Text("Log Tx") },
                modifier = Modifier.testTag("add_transaction_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("transactions_screen")
        ) {
            // Title & SMS Sync row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Book",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            val hasReadSms = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_SMS
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (hasReadSms) {
                                viewModel.scanInboxForTransactions()
                                showSmsImportDialog = true
                            } else {
                                smsPermissionsLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_SMS,
                                        Manifest.permission.RECEIVE_SMS
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sms_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = "Sync from SMS",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync SMS", style = MaterialTheme.typography.labelMedium)
                    }

                    FilledTonalButton(
                        onClick = {
                            if (filteredTransactions.isEmpty()) {
                                android.widget.Toast.makeText(context, "No transactions to export", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                showExportDialog = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export to CSV",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Search Bar & Filters Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search description, category, tags...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear Search")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_bar"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = { showFiltersExpanded = !showFiltersExpanded },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = if (showFiltersExpanded) Icons.Default.FilterListOff else Icons.Default.FilterList,
                        contentDescription = "Expand Filter Sheet"
                    )
                }
            }

            // Expanded Collapsible Filters Bar
            AnimatedVisibility(
                visible = showFiltersExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .shadow(4.dp, RoundedCornerShape(20.dp), clip = false),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header with action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Refine Ledger Book",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            // Reset active filters
                            val hasActiveFilters = selectedTypeFilter != "ALL" || 
                                                 selectedAccountFilter != "ALL" || 
                                                 selectedSortBy != "NEWEST" || 
                                                 selectedDateFilterType != "ALL"
                            
                            if (hasActiveFilters) {
                                TextButton(
                                    onClick = {
                                        selectedTypeFilter = "ALL"
                                        selectedAccountFilter = "ALL"
                                        selectedSortBy = "NEWEST"
                                        selectedDateFilterType = "ALL"
                                        customStartDate = null
                                        customEndDate = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset all",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Reset All",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 1. Transaction Type Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Transaction Type",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("ALL", "EXPENSE", "INCOME").forEach { type ->
                                    val isSelected = selectedTypeFilter == type
                                    val labelText = when (type) {
                                        "ALL" -> "All Types"
                                        "EXPENSE" -> "🔴 Expense"
                                        "INCOME" -> "🟢 Income"
                                        else -> type
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedTypeFilter = type }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = labelText,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Channel Selector Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Payment Channel",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            var accountDropdownExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { accountDropdownExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = if (selectedAccountFilter == "ALL") "All Accounts" else "💳 $selectedAccountFilter",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown, 
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                DropdownMenu(
                                    expanded = accountDropdownExpanded,
                                    onDismissRequest = { accountDropdownExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Accounts", fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            selectedAccountFilter = "ALL"
                                            accountDropdownExpanded = false
                                        }
                                    )
                                    accounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = { Text(acc.name) },
                                            onClick = {
                                                selectedAccountFilter = acc.name
                                                accountDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Date Range Filter Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Date Book Range",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Row of date filters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val dateFilters = listOf(
                                    "ALL" to "All Time", 
                                    "TODAY" to "Today", 
                                    "THIS_WEEK" to "Weekly", 
                                    "THIS_MONTH" to "Monthly", 
                                    "CUSTOM" to "Custom 📅"
                                )
                                
                                dateFilters.forEach { (typeKey, typeLabel) ->
                                    val isSelected = selectedDateFilterType == typeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(if (typeKey == "CUSTOM") 1.2f else 1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedDateFilterType = typeKey }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = typeLabel,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            
                            // Expanded selector for CUSTOM range
                            AnimatedVisibility(
                                visible = selectedDateFilterType == "CUSTOM",
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "Select boundaries to narrow down:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Start Date
                                        OutlinedCard(
                                            onClick = { showStartDatePicker = true },
                                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("START DATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = customStartDate?.let { dateFormatter.format(Date(it)) } ?: "Tap to Set",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (customStartDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // End Date
                                        OutlinedCard(
                                            onClick = { showEndDatePicker = true },
                                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("END DATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = customEndDate?.let { dateFormatter.format(Date(it)) } ?: "Tap to Set",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (customEndDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        
                                        // Clear custom range
                                        if (customStartDate != null || customEndDate != null) {
                                            IconButton(
                                                onClick = {
                                                    customStartDate = null
                                                    customEndDate = null
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.CenterVertically)
                                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                                    .size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear custom dates",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Sort By Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Chronology & Volume",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("NEWEST", "OLDEST", "HIGHEST", "LOWEST").forEach { sort ->
                                    val isSelected = selectedSortBy == sort
                                    val labelText = when (sort) {
                                        "NEWEST" -> "⏱️ Newest"
                                        "OLDEST" -> "🕰️ Oldest"
                                        "HIGHEST" -> "📈 Max Rs"
                                        "LOWEST" -> "📉 Min Rs"
                                        else -> sort
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedSortBy = sort }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = labelText,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction Count Info & Active Filter Indicators Tag Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Showing ${filteredTransactions.size} of ${transactions.size} records",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Show current active date range description as a tag
                val dateLabel = when (selectedDateFilterType) {
                    "TODAY" -> "Today Only"
                    "THIS_WEEK" -> "This Week"
                    "THIS_MONTH" -> "This Month"
                    "CUSTOM" -> {
                        val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
                        val s = customStartDate?.let { formatter.format(Date(it)) } ?: "Start"
                        val e = customEndDate?.let { formatter.format(Date(it)) } ?: "End"
                        "$s - $e"
                    }
                    else -> "All Time"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BentoPrimary.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = BentoPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary
                        )
                    }
                }
            }

            // Dynamic Transactions List
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = "Empty Search Results",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transactions matched filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try clearing search queries or choosing 'All'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                TransactionListItem(
                                    transaction = tx,
                                    categories = categories,
                                    onEdit = { onEditTransaction(tx) },
                                    onDelete = { viewModel.deleteTransaction(tx) }
                                )
                            }
                        }
                    }
                }
            }

            // Display SMS bulk import Dialog
            if (showSmsImportDialog) {
                SmsImportDialog(
                    scannedSms = scannedSmsList,
                    categories = categories,
                    accounts = accounts,
                    onToggleSelectAll = { viewModel.toggleSelectAllSms(it) },
                    onToggleSelection = { viewModel.toggleSmsSelection(it) },
                    onToggleExpansion = { viewModel.toggleSmsExpansion(it) },
                    onUpdateDetails = { id, amt, merchant, cat, accId, isExp ->
                        viewModel.updateSmsDetails(id, amt, merchant, cat, accId, isExp)
                    },
                    onDismiss = {
                        viewModel.clearScannedSmsList()
                        showSmsImportDialog = false
                    },
                    onConfirmImport = {
                        viewModel.logSelectedSmsTransactions()
                        showSmsImportDialog = false
                    }
                )
            }

            // Display Export Choice & Preview Dialog
            if (showExportDialog) {
                ExportCsvDialog(
                    transactions = filteredTransactions,
                    onDismiss = { showExportDialog = false },
                    onSystemSaveClick = {
                        exportCsvLauncher.launch("expense_iq_transactions_${System.currentTimeMillis()}.csv")
                    },
                    context = context
                )
            }

            // Display Date Pickers for custom ranges
            if (showStartDatePicker) {
                SimpleDatePickerDialog(
                    initialSelectedDateMillis = customStartDate,
                    onDateSelected = { customStartDate = it },
                    onDismiss = { showStartDatePicker = false }
                )
            }

            if (showEndDatePicker) {
                SimpleDatePickerDialog(
                    initialSelectedDateMillis = customEndDate,
                    onDateSelected = { customEndDate = it },
                    onDismiss = { showEndDatePicker = false }
                )
            }
        }
    }
}

@Composable
fun ExportCsvDialog(
    transactions: List<ExpenseTransaction>,
    onDismiss: () -> Unit,
    onSystemSaveClick: () -> Unit,
    context: android.content.Context
) {
    val csvContent = remember(transactions) {
        CsvExporter.generateCsvContent(transactions)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Export Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${transactions.size} records selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Scrollable Preview Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        LazyColumn {
                            item {
                                Text(
                                    text = csvContent,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Choose your preferred export method:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Export Options Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Option 1: Direct Save to Public Downloads (Fastest and easiest!)
                    Button(
                        onClick = {
                            val uri = CsvExporter.saveCsvToDownloads(context, csvContent)
                            if (uri != null) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_save_downloads_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save to Downloads",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save to Downloads Folder", style = MaterialTheme.typography.labelLarge)
                    }

                    // Option 2: Share Sheet File Export
                    FilledTonalButton(
                        onClick = {
                            if (CsvExporter.shareCsvFile(context, csvContent)) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_share_sheet_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share CSV",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share CSV File", style = MaterialTheme.typography.labelLarge)
                    }

                    // Option 3: Copy Raw CSV Content
                    OutlinedButton(
                        onClick = {
                            CsvExporter.copyCsvToClipboard(context, csvContent)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_copy_clip_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy to Clipboard",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy to Clipboard", style = MaterialTheme.typography.labelLarge)
                    }

                    // Option 4: Custom SAF picker (Standard Save)
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onSystemSaveClick()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_saf_picker_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Map File",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Custom Directory", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("export_dialog_close")
            ) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initialSelectedDateMillis: Long?,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis ?: System.currentTimeMillis()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    onDismiss()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

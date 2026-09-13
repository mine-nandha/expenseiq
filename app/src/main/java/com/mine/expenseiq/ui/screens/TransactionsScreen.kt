package com.mine.expenseiq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    var showRefineSheet by remember { mutableStateOf(false) }

    val hasActiveFilters = selectedTypeFilter != "ALL" ||
            selectedAccountFilter != "ALL" ||
            selectedSortBy != "NEWEST" ||
            selectedDateFilterType != "ALL" ||
            customStartDate != null ||
            customEndDate != null

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
                text = { Text("New entry") },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = onAddTransactionClick,
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
            // Header: wordmark + Book title, actions tucked right
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EXPENSEIQ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onBackground else Color(0xFF475569),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Transaction book",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

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
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("sms_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
                    placeholder = { Text("Search note, category, or tag") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear search")
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
                    onClick = { showRefineSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp).testTag("refine_book_button")
                ) {
                    Icon(
                        imageVector = if (hasActiveFilters) Icons.Default.FilterAlt else Icons.Default.FilterList,
                        contentDescription = "Refine book",
                        tint = if (hasActiveFilters) MaterialTheme.colorScheme.onPrimaryContainer else LocalContentColor.current
                    )
                }
            }

            // Refine controls live in a modal sheet, opened by the filter button above.
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${filteredTransactions.size} of ${transactions.size} entries",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val dateLabel = when (selectedDateFilterType) {
                    "TODAY" -> "Today"
                    "THIS_WEEK" -> "This week"
                    "THIS_MONTH" -> "This month"
                    "CUSTOM" -> {
                        val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
                        val s = customStartDate?.let { formatter.format(Date(it)) } ?: "Start"
                        val e = customEndDate?.let { formatter.format(Date(it)) } ?: "End"
                        "$s–$e"
                    }
                    else -> "All time"
                }

                if (hasActiveFilters) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

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
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No entries match these filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Clear the search or choose All.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    itemsIndexed(filteredTransactions, key = { _, tx -> tx.id }) { index, tx ->
                        TransactionListItem(
                            transaction = tx,
                            categories = categories,
                            onEdit = { onEditTransaction(tx) },
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                        if (index < filteredTransactions.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )
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

            // Refine sheet: filters live here, not inline in the page flow
            if (showRefineSheet) {
                RefineBookSheet(
                    selectedTypeFilter = selectedTypeFilter,
                    selectedAccountFilter = selectedAccountFilter,
                    selectedDateFilterType = selectedDateFilterType,
                    customStartDate = customStartDate,
                    customEndDate = customEndDate,
                    selectedSortBy = selectedSortBy,
                    accounts = accounts,
                    hasActiveFilters = hasActiveFilters,
                    onTypeSelected = { selectedTypeFilter = it },
                    onAccountSelected = { selectedAccountFilter = it },
                    onDateTypeSelected = { selectedDateFilterType = it },
                    onPickStartDate = { showStartDatePicker = true },
                    onPickEndDate = { showEndDatePicker = true },
                    onClearCustomDates = {
                        customStartDate = null
                        customEndDate = null
                    },
                    onSortSelected = { selectedSortBy = it },
                    onResetAll = {
                        selectedTypeFilter = "ALL"
                        selectedAccountFilter = "ALL"
                        selectedSortBy = "NEWEST"
                        selectedDateFilterType = "ALL"
                        customStartDate = null
                        customEndDate = null
                    },
                    onDismiss = { showRefineSheet = false }
                )
            }
        }
    }
}

@Composable
private fun RefineChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun TypeRefineChip(
    label: String,
    selected: Boolean,
    dotColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RefineBookSheet(
    selectedTypeFilter: String,
    selectedAccountFilter: String,
    selectedDateFilterType: String,
    customStartDate: Long?,
    customEndDate: Long?,
    selectedSortBy: String,
    accounts: List<Account>,
    hasActiveFilters: Boolean,
    onTypeSelected: (String) -> Unit,
    onAccountSelected: (String) -> Unit,
    onDateTypeSelected: (String) -> Unit,
    onPickStartDate: () -> Unit,
    onPickEndDate: () -> Unit,
    onClearCustomDates: () -> Unit,
    onSortSelected: (String) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val incomeDot = if (isSystemInDarkTheme()) BentoAccentGreen else IncomeGreenDeep

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Refine book",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasActiveFilters) {
                    TextButton(onClick = onResetAll) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset all")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            Text(
                text = "Type",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypeRefineChip(
                    label = "All",
                    selected = selectedTypeFilter == "ALL",
                    dotColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onTypeSelected("ALL") },
                    modifier = Modifier.weight(1f)
                )
                TypeRefineChip(
                    label = "Expense",
                    selected = selectedTypeFilter == "EXPENSE",
                    dotColor = MaterialTheme.colorScheme.primary,
                    onClick = { onTypeSelected("EXPENSE") },
                    modifier = Modifier.weight(1f)
                )
                TypeRefineChip(
                    label = "Income",
                    selected = selectedTypeFilter == "INCOME",
                    dotColor = incomeDot,
                    onClick = { onTypeSelected("INCOME") },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            Text(
                text = "Account",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedAccountFilter == "ALL") "All accounts" else selectedAccountFilter,
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
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("All accounts", fontWeight = FontWeight.Bold) },
                        onClick = {
                            onAccountSelected("ALL")
                            accountDropdownExpanded = false
                        }
                    )
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.name) },
                            onClick = {
                                onAccountSelected(acc.name)
                                accountDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            Text(
                text = "Date",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val dateFilters = listOf(
                    "ALL" to "All time",
                    "TODAY" to "Today",
                    "THIS_WEEK" to "This week",
                    "THIS_MONTH" to "This month",
                    "CUSTOM" to "Custom"
                )
                dateFilters.forEach { (key, label) ->
                    RefineChip(
                        label = label,
                        selected = selectedDateFilterType == key,
                        onClick = { onDateTypeSelected(key) }
                    )
                }
            }

            if (selectedDateFilterType == "CUSTOM") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedCard(
                        onClick = onPickStartDate,
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Start date",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = customStartDate?.let { dateFormatter.format(Date(it)) } ?: "Not set",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (customStartDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    OutlinedCard(
                        onClick = onPickEndDate,
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "End date",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = customEndDate?.let { dateFormatter.format(Date(it)) } ?: "Not set",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (customEndDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (customStartDate != null || customEndDate != null) {
                    TextButton(onClick = onClearCustomDates, contentPadding = PaddingValues(0.dp)) {
                        Text("Clear dates", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            Text(
                text = "Order",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("NEWEST" to "Newest first", "OLDEST" to "Oldest first").forEach { (key, label) ->
                    RefineChip(
                        label = label,
                        selected = selectedSortBy == key,
                        onClick = { onSortSelected(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("HIGHEST" to "Highest amount", "LOWEST" to "Lowest amount").forEach { (key, label) ->
                    RefineChip(
                        label = label,
                        selected = selectedSortBy == key,
                        onClick = { onSortSelected(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
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
                    text = "${transactions.size} entries selected",
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
                    text = "Choose an export option:",
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
                        Text("Save to Downloads", style = MaterialTheme.typography.labelLarge)
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

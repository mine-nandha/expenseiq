package com.mine.expenseiq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.mine.expenseiq.data.local.AppDatabase
import com.mine.expenseiq.data.model.*
import com.mine.expenseiq.data.repository.ExpenseRepository
import com.mine.expenseiq.ui.components.SmsConfirmationPrompt
import com.mine.expenseiq.ui.screens.*
import com.mine.expenseiq.ui.theme.MyApplicationTheme
import com.mine.expenseiq.viewmodel.ExpenseViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mine.expenseiq.workers.SmsSyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sync Database & Setup Constructor-injection repository pattern
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(database.expenseDao())
        
        // Setup ViewModel
        val factory = ExpenseViewModel.Factory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[ExpenseViewModel::class.java]

        // Request SMS permissions on startup
        val basePermissions = arrayOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS
        )
        val ungranted = basePermissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            requestPermissions(ungranted.toTypedArray(), 101)
        }

        // Schedule periodic background SMS sync worker (min interval allowed 15 mins)
        try {
            val syncRequest = PeriodicWorkRequestBuilder<SmsSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "SmsBackgroundSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to schedule background SMS Sync: ${e.message}", e)
        }

        setContent {
            MyApplicationTheme {
                MainAppLayout(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: ExpenseViewModel) {
    // Collect Flow States
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val pendingSms by viewModel.pendingSms.collectAsState()

    var currentTab by remember { mutableStateOf(0) }

    // Dialog state controllers
    var showAddTxDialog by remember { mutableStateOf(false) }
    var activeEditTx by remember { mutableStateOf<ExpenseTransaction?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
    NavigationBar(
        modifier = Modifier.testTag("bottom_navigation_bar"),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { currentTab = 0 },
            icon = { Icon(if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home, "Home") },
            label = { Text("Dashboard") },
            alwaysShowLabel = false,
            modifier = Modifier.testTag("tab_dashboard")
        )
        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { currentTab = 1 },
            icon = { Icon(if (currentTab == 1) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong, "Transactions") },
            label = { Text("Book") },
            alwaysShowLabel = false,
            modifier = Modifier.testTag("tab_transactions")
        )
        NavigationBarItem(
            selected = currentTab == 2,
            onClick = { currentTab = 2 },
            icon = { Icon(if (currentTab == 2) Icons.Filled.Timer else Icons.Outlined.Timer, "Budgets") },
            label = { Text("Budgets") },
            alwaysShowLabel = false,
            modifier = Modifier.testTag("tab_budgets")
        )
        NavigationBarItem(
            selected = currentTab == 3,
            onClick = { currentTab = 3 },
            icon = { Icon(if (currentTab == 3) Icons.Filled.CreditCard else Icons.Outlined.CreditCard, "Accounts") },
            label = { Text("Accounts") },
            alwaysShowLabel = false,
            modifier = Modifier.testTag("tab_accounts")
        )
        NavigationBarItem(
            selected = currentTab == 4,
            onClick = { currentTab = 4 },
            icon = { Icon(if (currentTab == 4) Icons.Filled.PieChart else Icons.Outlined.PieChart, "Reports") },
            label = { Text("Insights") },
            alwaysShowLabel = false,
            modifier = Modifier.testTag("tab_reports")
        )
    }
        },
        floatingActionButton = {
            // Dynamic FAB shortcuts depending on current Tab selection
            when (currentTab) {
                0 -> {
                    // Quick add transaction floating shortcut on Dashboard
                    FloatingActionButton(
                        onClick = { showAddTxDialog = true },
                        modifier = Modifier.testTag("dashboard_add_tx_fab")
                    ) {
                        Icon(Icons.Default.Add, "Log Transaction")
                    }
                }
                // Tabs 1 (Transactions), 2 (Budgets) and 3 (Accounts) already implement custom Floating Action buttons internally
                else -> { /* No default fallback FAB needed */ }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentTab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { currentTab = it },
                    onEditTransaction = { activeEditTx = it }
                )
                1 -> TransactionsScreen(
                    viewModel = viewModel,
                    onEditTransaction = { activeEditTx = it },
                    onAddTransactionClick = { showAddTxDialog = true }
                )
                2 -> BudgetsScreen(
                    viewModel = viewModel,
                    onAddBudgetClick = { showAddBudgetDialog = true }
                )
                3 -> AccountsScreen(
                    viewModel = viewModel,
                    onAddAccountClick = { showAddAccountDialog = true },
                    onTransferClick = { showTransferDialog = true },
                    onEditTransaction = { activeEditTx = it }
                )
                4 -> ReportsScreen(viewModel = viewModel)
                5 -> ProfileScreen(
                    viewModel = viewModel,
                    onBackClick = { currentTab = 0 }
                )
            }
        }

        // --- DIALOG OVERLAY TRIGGERS ---

        // 1. BANK RECEIVE SMS PROMPT OVERLAY
        pendingSms?.let { sms ->
            SmsConfirmationPrompt(
                parsedSms = sms,
                categories = categories,
                accounts = accounts,
                onDismiss = { viewModel.clearPendingSms() },
                onConfirm = { amt, type, cat, note, accName, accId ->
                    viewModel.addTransaction(
                        amount = amt,
                        type = type,
                        category = cat,
                        date = System.currentTimeMillis(),
                        note = note,
                        paymentMode = accName,
                        accountId = accId
                    )
                    viewModel.clearPendingSms()
                }
            )
        }

        // 2. ADD TRANSACTION OVERLAY
        if (showAddTxDialog) {
            AddTransactionDialog(
                categories = categories,
                accounts = accounts,
                onDismiss = { showAddTxDialog = false },
                onSave = { amt, type, cat, dt, nt, mode, accId, photo, recur, period, tags ->
                    viewModel.addTransaction(
                        amount = amt,
                        type = type,
                        category = cat,
                        date = dt,
                        note = nt,
                        paymentMode = mode,
                        accountId = accId,
                        photoUri = photo,
                        isRecurring = recur,
                        recurrencePeriod = period,
                        tags = tags
                    )
                    showAddTxDialog = false
                }
            )
        }

        // 3. EDIT TRANSACTION OVERLAY
        activeEditTx?.let { tx ->
            EditTransactionDialog(
                transaction = tx,
                categories = categories,
                accounts = accounts,
                onDismiss = { activeEditTx = null },
                onSave = { updatedTx ->
                    viewModel.updateTransaction(updatedTx)
                    activeEditTx = null
                }
            )
        }

        // 4. CONFIGURE BUDGET CONTROL OVERLAY
        if (showAddBudgetDialog) {
            AddBudgetDialog(
                categories = categories,
                onDismiss = { showAddBudgetDialog = false },
                onSave = { catName, limit, period, rollover ->
                    viewModel.addBudget(
                        categoryName = catName,
                        limitAmount = limit,
                        period = period,
                        isRollover = rollover
                    )
                    showAddBudgetDialog = false
                }
            )
        }

        // 5. CREATE ACCOUNT / CARD OVERLAY
        if (showAddAccountDialog) {
            AddAccountDialog(
                onDismiss = { showAddAccountDialog = false },
                onSave = { name, type, balance, color ->
                    viewModel.addAccount(
                        name = name,
                        type = type,
                        balance = balance,
                        color = color
                    )
                    showAddAccountDialog = false
                }
            )
        }

        // 6. TRANSFER FUNDS OVERLAY
        if (showTransferDialog) {
            TransferFundsDialog(
                accounts = accounts,
                onDismiss = { showTransferDialog = false },
                onTransfer = { fromId, toId, amt ->
                    viewModel.executeTransfer(
                        fromAccountId = fromId,
                        toAccountId = toId,
                        amount = amt
                    )
                    showTransferDialog = false
                }
            )
        }
    }
}

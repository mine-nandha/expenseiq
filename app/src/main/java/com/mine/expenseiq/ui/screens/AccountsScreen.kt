package com.mine.expenseiq.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mine.expenseiq.data.model.*
import com.mine.expenseiq.viewmodel.ExpenseViewModel
import com.mine.expenseiq.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: ExpenseViewModel,
    onAddAccountClick: () -> Unit,
    onTransferClick: () -> Unit,
    onEditTransaction: (ExpenseTransaction) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    
    // Search fields
    var walletSearchQuery by remember { mutableStateOf("") }
    var txSearchQuery by remember { mutableStateOf("") }
    
    // Type Filter
    var selectedTypeFilter by remember { mutableStateOf("All") }
    val walletTypes = listOf("All", "BANK", "CREDIT_CARD", "CASH", "WALLET")

    // Set default selected account if none selected
    LaunchedEffect(accounts) {
        if (selectedAccountId == null && accounts.isNotEmpty()) {
            selectedAccountId = accounts.first().id
        }
    }

    // Filter accounts based on query and type filter
    val filteredAccounts = accounts.filter { acc ->
        val matchesQuery = acc.name.contains(walletSearchQuery, ignoreCase = true) || 
                           acc.type.contains(walletSearchQuery, ignoreCase = true)
        val matchesType = selectedTypeFilter == "All" || acc.type.equals(selectedTypeFilter, ignoreCase = true)
        matchesQuery && matchesType
    }

    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
    
    // Filter transactions specifically for the selected account based on search query
    val accountTransactions = transactions.filter { it.accountId == selectedAccountId }
    val filteredAccountTransactions = accountTransactions.filter { tx ->
        tx.note.contains(txSearchQuery, ignoreCase = true) ||
        tx.category.contains(txSearchQuery, ignoreCase = true) ||
        tx.tags.contains(txSearchQuery, ignoreCase = true) ||
        tx.amount.toString().contains(txSearchQuery)
    }

    // Summary aggregates
    val totalNetWorth = accounts.sumOf { it.balance }
    val bankSum = accounts.filter { it.type == "BANK" }.sumOf { it.balance }
    val creditSum = accounts.filter { it.type == "CREDIT_CARD" }.sumOf { it.balance }
    val cashAndWalletSum = accounts.filter { it.type == "CASH" || it.type == "WALLET" }.sumOf { it.balance }

    Scaffold(
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Quick Transfer Funds Action FAB
                SmallFloatingActionButton(
                    onClick = onTransferClick,
                    modifier = Modifier
                        .testTag("transfer_funds_fab")
                        .shadow(6.dp, CircleShape),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Transfer Funds",
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Add Account/Wallet Card FAB
                ExtendedFloatingActionButton(
                    onClick = onAddAccountClick,
                    icon = { 
                        Icon(
                            imageVector = Icons.Default.AddCard, 
                            contentDescription = "Create Account", 
                            modifier = Modifier.size(20.dp)
                        ) 
                    },
                    text = { Text("Log Wallet", fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp) },
                    modifier = Modifier
                        .testTag("add_account_fab")
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("accounts_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            
            // 1. Elegant Header Title
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Wealth Hub",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Search, track balances, and manage wallets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 2. High-Fidelity Stats / Net Worth Breakdown Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp), clip = false),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Total balance title
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NET HOLDINGS VALUE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Secure holdings",
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            // Networth amount
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.2f", totalNetWorth)}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f), thickness = 1.dp)

                            // Horizontal Grid-like asset quick metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 1. Banks Sum
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalance,
                                            null,
                                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Banks",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%,.0f", bankSum)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                // 2. Credit limit sum / liability
                                Column(
                                    modifier = Modifier.weight(1.3f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CreditCard,
                                            null,
                                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Credit Balance",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%,.0f", creditSum)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                // 3. Wallets & Cash
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Wallet,
                                            null,
                                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Cash/Wallet",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%,.0f", cashAndWalletSum)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Search & Filter Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Wallets Search Box
                    OutlinedTextField(
                        value = walletSearchQuery,
                        onValueChange = { walletSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp), clip = false)
                            .testTag("wallets_search_input"),
                        shape = RoundedCornerShape(20.dp),
                        placeholder = { 
                            Text(
                                "Search wallet name, bank, or type...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal
                            ) 
                        },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Search, 
                                contentDescription = "Search icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        trailingIcon = {
                            if (walletSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { walletSearchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear, 
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )

                    // Scrolling Category Filter Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(walletTypes) { type ->
                            val isSelected = type == selectedTypeFilter
                            val displayName = when (type) {
                                "All" -> "All Accounts"
                                "BANK" -> "🏦 Banks"
                                "CREDIT_CARD" -> "💳 Credit Cards"
                                "CASH" -> "💵 Cash"
                                "WALLET" -> "📱 Digits / Wallets"
                                else -> type
                            }
                            
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTypeFilter = type },
                                label = { 
                                    Text(
                                        text = displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ) 
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                            )
                        }
                    }
                }
            }

            // 4. Accounts Sliding Collection (or Empty State)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "YOUR WALLET OPTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    if (filteredAccounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .padding(horizontal = 20.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = "No accounts found",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No matching wallets are active.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Try modifying search filters or add a new wallet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Force keep state highlights clean
                        var sliderSelId = selectedAccountId
                        if (sliderSelId != null && !filteredAccounts.any { it.id == sliderSelId }) {
                            // If selected account is filtered out, select first available on filter list
                            sliderSelId = filteredAccounts.first().id
                            selectedAccountId = sliderSelId
                        }
                        
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredAccounts, key = { it.id }) { acc ->
                                val isSelected = acc.id == selectedAccountId
                                AccountCardItem(
                                    account = acc,
                                    isSelected = isSelected,
                                    onClick = { selectedAccountId = acc.id }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Transaction list header & detailed search for the focused account
            if (selectedAccount != null) {
                // Set of info about focused account
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedAccount.name.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Transaction Archives",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Danger zone: Delete selected account
                            IconButton(
                                onClick = {
                                    if (accounts.size > 1) {
                                        viewModel.deleteAccount(selectedAccount)
                                        // Pick another account as active default
                                        selectedAccountId = accounts.firstOrNull { it.id != selectedAccount.id }?.id
                                        Toast.makeText(context, "${selectedAccount.name} closed safely.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "At least one digital wallet is required.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Delete Account",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Compact transactions search specifically inside this account!
                        OutlinedTextField(
                            value = txSearchQuery,
                            onValueChange = { txSearchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(1.dp, RoundedCornerShape(14.dp), clip = false),
                            shape = RoundedCornerShape(14.dp),
                            placeholder = { 
                                Text(
                                    "Search transactions in ${selectedAccount.name}...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                ) 
                            },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.FilterList, 
                                    contentDescription = "Filter transactions",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                ) 
                            },
                            trailingIcon = {
                                if (txSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { txSearchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear, 
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Listing entries
                if (filteredAccountTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = "No Tx Found",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (txSearchQuery.isNotEmpty()) "No transactions found matching \"$txSearchQuery\"" else "No transactions logged in this wallet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Add transactions manually in dashboard or via SMS triggers to start tracking.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(filteredAccountTransactions, key = { it.id }) { tx ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
            } else {
                // Edge case: No cards at all
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCard,
                                contentDescription = "Get Started",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Create Your First Wallet Account!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Click logging button below to set up digital wallets, credit cards, or cash accounts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountCardItem(
    account: Account,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(account.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(150.dp)
            .clickable(onClick = onClick)
            .testTag("account_card_${account.id}")
            .shadow(
                elevation = if (isSelected) 10.dp else 2.dp,
                shape = RoundedCornerShape(22.dp),
                clip = false
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            cardColor,
                            cardColor.copy(alpha = 0.85f),
                            cardColor.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = account.name,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (account.type.uppercase()) {
                                "BANK" -> "🏦 Institutional Bank"
                                "CREDIT_CARD" -> "💳 Premium Credit"
                                "CASH" -> "💵 Physical Cash"
                                "WALLET" -> "📱 Digital Holdings"
                                else -> account.type
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Small card badge/icon
                    val typeIcon = when (account.type.uppercase()) {
                        "BANK" -> Icons.Default.AccountBalance
                        "CREDIT_CARD" -> Icons.Default.CreditCard
                        "CASH" -> Icons.Default.Payments
                        "WALLET" -> Icons.Default.Wallet
                        else -> Icons.Default.AccountBalanceWallet
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp),
                        contentColor = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "AVAILABLE BALANCE",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.2f", account.balance)}",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = Color.White
                    )
                }
            }

            // High-fidelity card micro-chip design
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.18f)
                        .height(3.dp)
                        .background(Color.White, RoundedCornerShape(1.5.dp))
                        .align(Alignment.BottomStart)
                )
            }
        }
    }
}

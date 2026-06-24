package com.mine.expenseiq.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mine.expenseiq.data.model.Category
import com.mine.expenseiq.viewmodel.ExpenseViewModel
import com.mine.expenseiq.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ExpenseViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Collect database counts to show actual live metrics
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    
    // UI states
    var showExportDialog by remember { mutableStateOf(false) }
    var showConfirmImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var exportedJsonText by remember { mutableStateOf("") }
    
    // Category management states
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    
    // Toast controller helper
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            toastMessage = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("profile_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Screen Title with Back navigation support
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .size(40.dp)
                        .testTag("profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "User Hub",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Manage your preferences & registry backups",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // 2. User Premium Profile Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_user_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar bubble
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "N",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Nandha Kishore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "nandhakishore.2000@gmail.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // User status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Verified badge",
                                tint = BentoAccentGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "POWER USER • ACTIVE REBUILD ENGINE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 3. Current Live Registry Metrics Breakdown
        item {
            Text(
                text = "LIVE REGISTRY METRICS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricWidgetCard(
                    modifier = Modifier.weight(1f),
                    title = "Accounts",
                    count = accounts.size.toString(),
                    icon = Icons.Default.CreditCard,
                    accentColor = BentoAccentBlue
                )
                MetricWidgetCard(
                    modifier = Modifier.weight(1f),
                    title = "Budgets",
                    count = budgets.size.toString(),
                    icon = Icons.Default.Timer,
                    accentColor = BentoAccentOrange
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricWidgetCard(
                    modifier = Modifier.weight(1f),
                    title = "Categories",
                    count = categories.size.toString(),
                    icon = Icons.Default.Category,
                    accentColor = BentoAccentPurple
                )
                MetricWidgetCard(
                    modifier = Modifier.weight(1f),
                    title = "Transactions",
                    count = transactions.size.toString(),
                    icon = Icons.Default.ReceiptLong,
                    accentColor = BentoAccentGreen
                )
            }
        }

        // 4. Category Management
        item {
            Text(
                text = "CATEGORY MANAGEMENT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_management_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Button(
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_category_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Custom Category", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "EXPENSE CATEGORIES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    categories.filter { it.type == "EXPENSE" }.forEach { cat ->
                        CategoryRow(
                            category = cat,
                            onDelete = { categoryToDelete = cat }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "INCOME CATEGORIES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    categories.filter { it.type == "INCOME" }.forEach { cat ->
                        CategoryRow(
                            category = cat,
                            onDelete = { categoryToDelete = cat }
                        )
                    }
                }
            }
        }

        // 5. Data Backup & Rebuild Engine Controller
        item {
            Text(
                text = "DATA BACKUP & RESTORE CONSOLE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backup_restore_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Export Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Export data",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Safety Ledger Backup",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Save local database states into readable ledger lines",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Button(
                        onClick = {
                            val json = viewModel.exportDataAsJson()
                            exportedJsonText = json
                            showExportDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_backup_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Current Registry Backup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Import Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Import data",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Fresh Registry Import",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Rebuild data from previously saved ledger JSONs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("import_backup_input"),
                        label = { Text("Paste Ledger Backup JSON") },
                        placeholder = { Text("{\n  \"accounts\": [...],\n  \"transactions\": [...]\n}") },
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Danger Warning Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 1.dp)
                            )
                            Text(
                                text = "Clears all local history before performing a fresh override import to prevent duplication.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (importJsonText.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { importJsonText = "" },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text("Clear Input", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        
                        Button(
                            onClick = {
                                if (importJsonText.trim().isEmpty()) {
                                    toastMessage = "Please paste a ledger JSON string before validation triggers."
                                } else {
                                    showConfirmImportDialog = true
                                }
                            },
                            modifier = Modifier.weight(2f).height(48.dp).testTag("import_validate_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validate & Load Fresh", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // --- OVERLAY DIALOGS ---

    // 1. EXPORTED BACKUP CLIPBOARD DIALOG
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Ready-to-Copy Backup Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This code holds accounts, transactions, and categories in sync. Copy and store it safely in any text editor of your choice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Vault Ledger Backup", exportedJsonText)
                            clipboard.setPrimaryClip(clip)
                            toastMessage = "Backup JSON successfully copied to Clipboard!"
                            showExportDialog = false
                        } catch (e: Exception) {
                            toastMessage = "Failed to copy: ${e.localizedMessage}"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Copy Code block", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExportDialog = false }
                ) {
                    Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // 3. DELETE CATEGORY CONFIRMATION DIALOG
    categoryToDelete?.let { cat ->
        val txCount = transactions.count { it.category == cat.name }
        val budgetCount = budgets.count { it.categoryName == cat.name }
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Delete Category?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Are you sure you want to delete \"${cat.name}\"?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (txCount > 0 || budgetCount > 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (txCount > 0) {
                                    Text(
                                        "$txCount transaction(s) use this category.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                if (budgetCount > 0) {
                                    Text(
                                        "$budgetCount budget(s) reference this category.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Text(
                                    "Deleting will leave those entries without a valid category reference.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // 4. ADD CATEGORY DIALOG
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name, color, iconName, type ->
                viewModel.addCategory(name, color, iconName, type)
                showAddCategoryDialog = false
            }
        )
    }

    // 5. IMPORT OVERWRITE CONFIRMATION WARNING DIALOG
    if (showConfirmImportDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmImportDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Overwrite Active Ledger?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            },
            text = {
                Text(
                    "You are about to execute a deep rebuild. This cleans all active accounts, food limits, and existing ledger logs to load the newly imported dataset fresh without record duplication.\n\nAre you sure you want to proceed with database replacement?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmImportDialog = false
                        viewModel.importDataFromJson(
                            jsonString = importJsonText,
                            onSuccess = {
                                toastMessage = "Import completed! Fresh ledger registers successfully setup."
                                importJsonText = ""
                            },
                            onError = { err ->
                                toastMessage = "Rebuild failure: $err"
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Overwrite Fresh", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmImportDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun CategoryRow(
    category: Category,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        Color(android.graphics.Color.parseColor(category.color)),
                        CircleShape
                    )
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete ${category.name}",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun MetricWidgetCard(
    modifier: Modifier = Modifier,
    title: String,
    count: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = count,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

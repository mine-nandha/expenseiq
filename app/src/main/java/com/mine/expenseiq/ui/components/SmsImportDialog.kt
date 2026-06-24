package com.mine.expenseiq.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mine.expenseiq.data.model.Account
import com.mine.expenseiq.data.model.Category
import com.mine.expenseiq.utils.ParsedSmsWithRaw
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsImportDialog(
    scannedSms: List<ParsedSmsWithRaw>,
    categories: List<Category>,
    accounts: List<Account>,
    onToggleSelectAll: (Boolean) -> Unit,
    onToggleSelection: (String) -> Unit,
    onToggleExpansion: (String) -> Unit,
    onUpdateDetails: (String, Double, String, String, Long, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirmImport: () -> Unit
) {
    val selectedCount = scannedSms.count { it.isSelected }
    val allSelected = scannedSms.isNotEmpty() && scannedSms.all { it.isSelected }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = "SMS Sync Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Sync Past SMS",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Select banking transactions to log",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("sms_import_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select All Control Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { onToggleSelectAll(it) },
                                modifier = Modifier.testTag("sms_select_all_checkbox")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Select All Transactions",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "$selectedCount selected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable List
                if (scannedSms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = "No SMS found",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No transaction SMS found.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Make sure you have financial SMS messages inside your inbox.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(scannedSms, key = { it.id }) { item ->
                            SmsScannedItemRow(
                                item = item,
                                categories = categories,
                                accounts = accounts,
                                onToggleSelect = { onToggleSelection(item.id) },
                                onToggleExpand = { onToggleExpansion(item.id) },
                                onSaveDetails = { amt, merchant, cat, accId, isExp ->
                                    onUpdateDetails(item.id, amt, merchant, cat, accId, isExp)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .testTag("sms_import_cancel"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onConfirmImport,
                        enabled = selectedCount > 0,
                        modifier = Modifier
                            .weight(1.5f)
                            .padding(start = 8.dp)
                            .testTag("sms_import_confirm"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Selected ($selectedCount)")
                    }
                }
            }
        }
    }
}

@Composable
fun SmsScannedItemRow(
    item: ParsedSmsWithRaw,
    categories: List<Category>,
    accounts: List<Account>,
    onToggleSelect: () -> Unit,
    onToggleExpand: () -> Unit,
    onSaveDetails: (Double, String, String, Long, Boolean) -> Unit
) {
    val df = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = df.format(Date(item.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelect() }
            .testTag("sms_scanned_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (item.isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Main Top Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.testTag("sms_checkbox_${item.id}")
                )
                
                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.merchant,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (item.isExpense) "-₹${item.amount}" else "+₹${item.amount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isExpense) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$dateStr • ${item.sender}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(item.selectedCategory) },
                                modifier = Modifier.scale(0.85f).height(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val activeAccountName = accounts.firstOrNull { it.id == item.selectedAccountId }?.name ?: item.paymentMode
                            SuggestionChip(
                                onClick = {},
                                label = { Text(activeAccountName) },
                                modifier = Modifier.scale(0.85f).height(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Raw SMS text excerpt
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.rawBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Expand/Collapse Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.testTag("sms_expand_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.isExpanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                        contentDescription = "Expand Edit Section"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (item.isExpanded) "Collapse" else "Edit Details")
                }
            }

            // Editable expanded block
            if (item.isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                var editAmount by remember { mutableStateOf(item.amount.toString()) }
                var editMerchant by remember { mutableStateOf(item.merchant) }
                var editIsExpense by remember { mutableStateOf(item.isExpense) }
                var editCategory by remember { mutableStateOf(item.selectedCategory) }
                var editAccountId by remember { mutableStateOf(item.selectedAccountId) }
                var isCatMenuExpanded by remember { mutableStateOf(false) }
                var isAccMenuExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        "Edit Details before logging",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Row showing Expense/Income toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Type: ", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = editIsExpense,
                            onClick = { editIsExpense = true },
                            label = { Text("Expense") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = !editIsExpense,
                            onClick = { editIsExpense = false },
                            label = { Text("Income") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Grid text fields (Amount and Merchant)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = { editAmount = it },
                            label = { Text("Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sms_edit_amt_${item.id}"),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = editMerchant,
                            onValueChange = { editMerchant = it },
                            label = { Text("Merchant") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("sms_edit_merchant_${item.id}"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dropdowns for Category and Account
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Category dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = editCategory,
                                onValueChange = {},
                                label = { Text("Category") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isCatMenuExpanded = true }
                                    .testTag("sms_edit_cat_tf_${item.id}"),
                                enabled = false,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            DropdownMenu(
                                expanded = isCatMenuExpanded,
                                onDismissRequest = { isCatMenuExpanded = false }
                            ) {
                                val relevantCats = categories.filter { 
                                    it.type == (if (editIsExpense) "EXPENSE" else "INCOME")
                                }
                                relevantCats.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            editCategory = cat.name
                                            isCatMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Account dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            val activeAccountName = accounts.firstOrNull { it.id == editAccountId }?.name ?: "Cash"
                            OutlinedTextField(
                                value = activeAccountName,
                                onValueChange = {},
                                label = { Text("Account") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isAccMenuExpanded = true }
                                    .testTag("sms_edit_acc_tf_${item.id}"),
                                enabled = false,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            DropdownMenu(
                                expanded = isAccMenuExpanded,
                                onDismissRequest = { isAccMenuExpanded = false }
                            ) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.name) },
                                        onClick = {
                                            editAccountId = acc.id
                                            isAccMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Apply Changes Inline Button
                    Button(
                        onClick = {
                            val parsedAmt = editAmount.toDoubleOrNull() ?: 0.0
                            onSaveDetails(parsedAmt, editMerchant, editCategory, editAccountId, editIsExpense)
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("sms_save_inline_${item.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Apply Changes")
                    }
                }
            }
        }
    }
}

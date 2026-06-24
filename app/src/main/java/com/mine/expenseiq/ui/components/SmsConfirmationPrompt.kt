package com.mine.expenseiq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mine.expenseiq.data.model.*
import com.mine.expenseiq.utils.ParsedSms

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsConfirmationPrompt(
    parsedSms: ParsedSms,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        type: String,
        category: String,
        note: String,
        accountName: String,
        accountId: Long
    ) -> Unit
) {
    var amountStr by remember { mutableStateOf(parsedSms.amount.toString()) }
    var type by remember { mutableStateOf(if (parsedSms.isExpense) "EXPENSE" else "INCOME") }
    var note by remember { mutableStateOf(parsedSms.merchant) }
    
    // Auto-locate matched account matching payment Mode string
    val matchedAccount = accounts.firstOrNull { it.name == parsedSms.paymentMode } ?: accounts.firstOrNull()
    var selectedAccountId by remember { mutableStateOf(matchedAccount?.id ?: 1L) }
    var selectedAccountName by remember { mutableStateOf(matchedAccount?.name ?: "Cash") }

    // Auto-locate logical category (e.g. food ifSwiggy/StarCafe)
    val preSelectedCat = remember(note, type) {
        val noteLower = note.lowercase()
        val expenseCats = categories.filter { it.type == "EXPENSE" }.map { it.name }
        when {
            type == "INCOME" -> categories.firstOrNull { it.type == "INCOME" }?.name ?: "Salary"
            noteLower.contains("cafe") || noteLower.contains("pizza") || noteLower.contains("swiggy") || noteLower.contains("restaurant") || noteLower.contains("food") -> "Food"
            noteLower.contains("cab") || noteLower.contains("uber") || noteLower.contains("ola") || noteLower.contains("metro") || noteLower.contains("fuel") -> "Transport"
            noteLower.contains("amazon") || noteLower.contains("flipkart") || noteLower.contains("shopping") || noteLower.contains("mall") -> "Shopping"
            noteLower.contains("hospital") || noteLower.contains("medical") || noteLower.contains("pharmacy") || noteLower.contains("doctor") -> "Health"
            noteLower.contains("netflix") || noteLower.contains("movie") || noteLower.contains("spotify") || noteLower.contains("theater") -> "Entertainment"
            noteLower.contains("house") || noteLower.contains("room") || noteLower.contains("rent") -> "Rent"
            noteLower.contains("power") || noteLower.contains("electricity") || noteLower.contains("water") || noteLower.contains("recharge") -> "Utilities"
            noteLower.contains("emi") || noteLower.contains("loan") -> "EMI"
            else -> expenseCats.firstOrNull() ?: "Savings"
        }
    }
    var selectedCategory by remember { mutableStateOf(preSelectedCat) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("sms_confirmation_prompt")
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header Icon Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Sms, "UPI Reader Icon", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "SMS Transaction Auto-Read",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Review scanned information",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alert details banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Matches Indian Banking pattern:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Verified extracted details match your records. Edit any fields below if required.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Editable Fields:
                
                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Extracted Amount (INR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sms_amount_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Merchant Note name
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Extracted Merchant") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sms_merchant_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Matched account dropdown
                var accountExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedAccountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Account") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { accountExpanded = true },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    DropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (Balance: ₹${acc.balance})") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    selectedAccountName = acc.name
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category dropdown matcher
                var categoryExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Auto-Matched Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryExpanded = true },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categories.filter { it.type == type }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat.name
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Confirm Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("sms_dismiss_button")) {
                        Text("Dismiss")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalAmt = amountStr.toDoubleOrNull() ?: 0.0
                            if (finalAmt > 0) {
                                onConfirm(
                                    finalAmt,
                                    type,
                                    selectedCategory,
                                    note,
                                    selectedAccountName,
                                    selectedAccountId
                                )
                            }
                        },
                        modifier = Modifier.testTag("sms_accept_button")
                    ) {
                        Text("Accept & Log")
                    }
                }
            }
        }
    }
}

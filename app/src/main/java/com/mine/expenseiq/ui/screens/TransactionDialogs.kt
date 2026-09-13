package com.mine.expenseiq.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mine.expenseiq.data.model.*
import com.mine.expenseiq.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    accounts: List<Account>,
    accountUsage: Map<Long, Int> = emptyMap(),
    categoryUsage: Map<String, Map<String, Int>> = emptyMap(),
    prefill: QuickLogSuggestion? = null,
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        type: String,
        category: String,
        date: Long,
        note: String,
        paymentMode: String,
        accountId: Long,
        photoUri: String?,
        isRecurring: Boolean,
        recurrencePeriod: String?,
        tags: String
    ) -> Unit,
    onSaveAndAddAnother: (
        amount: Double,
        type: String,
        category: String,
        date: Long,
        note: String,
        paymentMode: String,
        accountId: Long,
        photoUri: String?,
        isRecurring: Boolean,
        recurrencePeriod: String?,
        tags: String
    ) -> Unit
) {
    fun mostUsedAccountId(): Long? =
        accountUsage.entries
            .filter { (id, _) -> accounts.any { it.id == id } }
            .sortedByDescending { it.value }
            .firstOrNull()?.key

    fun mostUsedCategoryOf(type: String): String? =
        categoryUsage[type]
            ?.entries
            ?.filter { (name, _) -> categories.any { it.type == type && it.name == name } }
            ?.sortedByDescending { it.value }
            ?.firstOrNull()?.key

    val defaultCategory = prefill?.category
        ?: mostUsedCategoryOf("EXPENSE")
        ?: categories.firstOrNull { it.type == "EXPENSE" }?.name
        ?: "Food"
    val defaultAccount = accounts.firstOrNull { it.id == (prefill?.accountId ?: mostUsedAccountId() ?: -1L) }
        ?: accounts.firstOrNull()

    var amountStr by remember { mutableStateOf(prefill?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(prefill?.type ?: "EXPENSE") }
    var selectedCategory by remember { mutableStateOf(defaultCategory) }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf(prefill?.note ?: "") }

    var selectedAccountId by remember { mutableStateOf(defaultAccount?.id ?: -1L) }
    var selectedAccountName by remember { mutableStateOf(defaultAccount?.name ?: "Cash") }

    var isRecurring by remember { mutableStateOf(false) }
    var recurrencePeriod by remember { mutableStateOf("MONTHLY") }
    var tagsStr by remember { mutableStateOf("") }
    var receiptAttached by remember { mutableStateOf(false) }

    var showAdvanced by remember { mutableStateOf(false) }
    var showAllAccounts by remember { mutableStateOf(false) }
    var showAllCategories by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val amountFocusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        amountFocusRequester.requestFocus()
    }

    val filteredCategories = categories.filter { it.type == type }

    // Top-3 most-used account ids (usage order) plus the currently selected one.
    val accountOrder = accountUsage.entries
        .filter { (id, _) -> accounts.any { it.id == id } }
        .sortedByDescending { it.value }
        .map { it.key }
        .let { ranked ->
            if (ranked.isEmpty()) accounts.map { it.id } else {
                val list = ranked.toMutableList()
                accounts.filter { it.id !in list }.forEach { list.add(it.id) }
                list
            }
        }
    val topAccountIds = (accountOrder.take(3)).toSet()
    val visibleAccounts = if (showAllAccounts) accounts else accounts.filter { it.id in topAccountIds || it.id == selectedAccountId }

    // Top-3 most-used category names for the current type, plus the selected one.
    val categoryOrder = categoryUsage[type]?.entries
        ?.filter { (name, _) -> categories.any { it.type == type && it.name == name } }
        ?.sortedByDescending { it.value }
        ?.map { it.key }
        ?: emptyList()
    val categoryOrderWithRest = if (categoryOrder.isEmpty()) {
        filteredCategories.map { it.name }
    } else {
        val list = categoryOrder.toMutableList()
        filteredCategories.filter { it.name !in list }.forEach { list.add(it.name) }
        list
    }
    val topCategoryNames = categoryOrderWithRest.take(3).toSet()
    val visibleCategories = if (showAllCategories) {
        filteredCategories
    } else {
        filteredCategories.filter { it.name in topCategoryNames || it.name == selectedCategory }
    }

    fun updateType(newType: String) {
        if (type != newType) {
            type = newType
            selectedCategory = mostUsedCategoryOf(newType)
                ?: categories.firstOrNull { it.type == newType }?.name
                ?: selectedCategory
        }
    }

    fun persist(keepOpen: Boolean) {
        val finalAmount = amountStr.toDoubleOrNull()
        if (finalAmount == null || finalAmount <= 0) {
            amountError = "Please enter a valid amount greater than zero."
            return
        }
        amountError = null
        val photoPath = if (receiptAttached) "content://com.mine.expenseiq/receipt_mock" else null
        val recurrence = if (isRecurring) recurrencePeriod else null
        val saveFn = if (keepOpen) onSaveAndAddAnother else onSave
        saveFn(
            finalAmount,
            type,
            selectedCategory,
            date,
            note.ifEmpty { "Transaction" },
            selectedAccountName,
            selectedAccountId,
            photoPath,
            isRecurring,
            recurrence,
            tagsStr
        )
        if (keepOpen) {
            amountStr = ""
            note = ""
            tagsStr = ""
            receiptAttached = false
            isRecurring = false
            amountFocusRequester.requestFocus()
        }
    }

    val isIncome = type == "INCOME"
    val isDark = isSystemInDarkTheme()
    val slipAccent by animateColorAsState(
        targetValue = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.primary,
        animationSpec = tween(220),
        label = "slipAccent"
    )
    val accentContainer =
        if (isIncome) (if (isDark) IncomeContainerDark else IncomeContainerLight)
        else MaterialTheme.colorScheme.primaryContainer

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("add_transaction_dialog"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (prefill != null) "Repeat transaction" else "Log transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Hero amount — the stamped total
            OutlinedTextField(
                value = amountStr,
                onValueChange = {
                    amountStr = it
                    if (amountError != null) amountError = null
                },
                textStyle = AmountDisplay.copy(color = MaterialTheme.colorScheme.onSurface),
                prefix = {
                    Text("\u20B9", style = AmountDisplay.copy(color = slipAccent))
                },
                placeholder = {
                    Text(
                        "0.00",
                        style = AmountDisplay.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { noteFocusRequester.requestFocus() }),
                isError = amountError != null,
                supportingText = if (amountError != null) {
                    { Text(amountError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = slipAccent,
                    focusedPrefixColor = slipAccent,
                    unfocusedPrefixColor = slipAccent,
                    errorTextColor = MaterialTheme.colorScheme.error,
                    errorSupportingTextColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input")
                    .focusRequester(amountFocusRequester)
            )

            // The stamp rule — the line the total is struck on
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(3.dp)
                    .background(
                        if (amountError != null) MaterialTheme.colorScheme.error else slipAccent,
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stamp toggle: Expense or Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SlipToggle(
                    label = "Expense",
                    selected = type == "EXPENSE",
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onSelect = { updateType("EXPENSE") }
                )
                SlipToggle(
                    label = "Income",
                    selected = type == "INCOME",
                    selectedColor = IncomeGreen,
                    onSelect = { updateType("INCOME") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Note / merchant
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / merchant") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = hairlineFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input")
                    .focusRequester(noteFocusRequester)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Paid from
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Paid from")
                Spacer(modifier = Modifier.weight(1f))
                if (accounts.size > visibleAccounts.size) {
                    if (showAllAccounts) {
                        TextButton(onClick = { showAllAccounts = false }) { Text("Less") }
                    } else {
                        TextButton(onClick = { showAllAccounts = true }) { Text("All ${accounts.size} accounts") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                visibleAccounts.forEach { acc ->
                    SlipChip(
                        selected = acc.id == selectedAccountId,
                        dotColor = runCatching { Color(android.graphics.Color.parseColor(acc.color)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary),
                        label = acc.name,
                        modifier = Modifier.testTag("account_chip_${acc.name}"),
                        onClick = {
                            selectedAccountId = acc.id
                            selectedAccountName = acc.name
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Category")
                Spacer(modifier = Modifier.weight(1f))
                if (filteredCategories.size > visibleCategories.size) {
                    if (showAllCategories) {
                        TextButton(onClick = { showAllCategories = false }) { Text("Less") }
                    } else {
                        TextButton(onClick = { showAllCategories = true }) { Text("All ${filteredCategories.size} categories") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                visibleCategories.forEach { cat ->
                    SlipChip(
                        selected = cat.name == selectedCategory,
                        dotColor = runCatching { Color(android.graphics.Color.parseColor(cat.color)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary),
                        label = cat.name,
                        modifier = Modifier.testTag("category_chip_${cat.name}"),
                        onClick = { selectedCategory = cat.name }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Actions
            Button(
                onClick = { persist(keepOpen = false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = slipAccent,
                    contentColor = Color.White
                )
            ) {
                Text("Save transaction", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { persist(keepOpen = true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save and add another", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Advanced section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "More options",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showAdvanced) {
                // Date Picker Trigger
                OutlinedTextField(
                    value = sdf.format(Date(date)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Change date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = date
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(y, m, d)
                                    date = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tags
                OutlinedTextField(
                    value = tagsStr,
                    onValueChange = { tagsStr = it },
                    label = { Text("Tags (comma separated, e.g. office, trip)") },
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = "Tags") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Receipt/Bill attachment toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (receiptAttached) accentContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { receiptAttached = !receiptAttached }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (receiptAttached) Icons.Default.Receipt else Icons.Default.CameraAlt,
                            contentDescription = "Receipt attachment",
                            tint = if (receiptAttached) slipAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Attach bill / receipt",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (receiptAttached) "\u2713 Receipt attached: receipt_scan_mock.webp"
                                else "Select a receipt image from your gallery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recurring
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Recurring",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isRecurring) "Repeats every ${recurrencePeriod.lowercase(Locale.getDefault())}"
                            else "Repeats on a schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                }

                if (isRecurring) {
                    var recurrenceExpanded by remember { mutableStateOf(false) }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = recurrencePeriod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Recurrence frequency") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { recurrenceExpanded = true },
                            enabled = false,
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = recurrenceExpanded,
                            onDismissRequest = { recurrenceExpanded = false }
                        ) {
                            listOf("DAILY", "WEEKLY", "MONTHLY").forEach { periodName ->
                                DropdownMenuItem(
                                    text = { Text(periodName) },
                                    onClick = {
                                        recurrencePeriod = periodName
                                        recurrenceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RowScope.SlipToggle(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onSelect: () -> Unit
) {
    val fill by animateColorAsState(
        targetValue = if (selected) selectedColor else Color.Transparent,
        animationSpec = tween(220),
        label = "slipToggleFill"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .clickable(onClick = onSelect)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SlipChip(
    selected: Boolean,
    dotColor: Color,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) dotColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) dotColor else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun hairlineFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedBorderColor = MaterialTheme.colorScheme.outline
    )

private fun formatAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

@Composable
fun EditTransactionDialog(
    transaction: ExpenseTransaction,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (ExpenseTransaction) -> Unit
) {
    var amountStr by remember { mutableStateOf(transaction.amount.toString()) }
    var type by remember { mutableStateOf(transaction.type) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var date by remember { mutableStateOf(transaction.date) }
    var note by remember { mutableStateOf(transaction.note) }
    
    val currentAccount = accounts.firstOrNull { it.id == transaction.accountId } ?: accounts.firstOrNull()
    var selectedAccountId by remember { mutableStateOf(currentAccount?.id ?: 1L) }
    var selectedAccountName by remember { mutableStateOf(currentAccount?.name ?: "Cash") }

    var isRecurring by remember { mutableStateOf(transaction.isRecurring) }
    var recurrencePeriod by remember { mutableStateOf(transaction.recurrencePeriod ?: "MONTHLY") }
    var tagsStr by remember { mutableStateOf(transaction.tags) }
    var receiptAttached by remember { mutableStateOf(transaction.photoUri != null) }

    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("edit_transaction_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Edit Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Type Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val activeColor = MaterialTheme.colorScheme.primaryContainer
                    val inactiveColor = Color.Transparent
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (type == "EXPENSE") activeColor else inactiveColor, RoundedCornerShape(10.dp))
                            .clickable { 
                                type = "EXPENSE"
                                selectedCategory = categories.firstOrNull { it.type == "EXPENSE" }?.name ?: "Food"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Expense", 
                            fontWeight = FontWeight.SemiBold,
                            color = if (type == "EXPENSE") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (type == "INCOME") activeColor else inactiveColor, RoundedCornerShape(10.dp))
                            .clickable { 
                                type = "INCOME"
                                selectedCategory = categories.firstOrNull { it.type == "INCOME" }?.name ?: "Salary"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Income", 
                            fontWeight = FontWeight.SemiBold,
                            color = if (type == "INCOME") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (INR)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = "Amount Icon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Merchant") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Note Icon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker
                OutlinedTextField(
                    value = sdf.format(Date(date)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Date Icon") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = date
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(y, m, d)
                                    date = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Accounts Selection
                var accountExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedAccountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Account") },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Account Icon") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Open accounts list") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { accountExpanded = true },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    DropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (Bal: ₹${acc.balance})") },
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

                // Categories dropdown
                var categoryExpanded by remember { mutableStateOf(false) }
                val filteredCategories = categories.filter { it.type == type }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = "Category Icon") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Open categories list") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryExpanded = true },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        filteredCategories.forEach { cat ->
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

                Spacer(modifier = Modifier.height(12.dp))

                // Tags
                OutlinedTextField(
                    value = tagsStr,
                    onValueChange = { tagsStr = it },
                    label = { Text("Tags (comma separated)") },
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = "Tags Icon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Image Bill Scanner Switch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (receiptAttached) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { receiptAttached = !receiptAttached }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (receiptAttached) Icons.Default.Receipt else Icons.Default.CameraAlt,
                            contentDescription = "Receipt Attachment Icon",
                            tint = if (receiptAttached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Attach Bill/Receipt Scan",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (receiptAttached) "✓ Receipt attached: receipt_scan_mock.webp" else "Select receipt image from local gallery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recurring Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Text("Is Recurring Expense", style = MaterialTheme.typography.bodyMedium)
                }

                if (isRecurring) {
                    var recurrenceExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = recurrencePeriod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Recurrence Frequency") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { recurrenceExpanded = true },
                            enabled = false,
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = recurrenceExpanded,
                            onDismissRequest = { recurrenceExpanded = false }
                        ) {
                            listOf("DAILY", "WEEKLY", "MONTHLY").forEach { periodName ->
                                DropdownMenuItem(
                                    text = { Text(periodName) },
                                    onClick = {
                                        recurrencePeriod = periodName
                                        recurrenceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalAmount = amountStr.toDoubleOrNull() ?: 0.0
                            if (finalAmount > 0) {
                                val photoPath = if (receiptAttached) "content://com.mine.expenseiq/receipt_mock" else null
                                onSave(
                                    transaction.copy(
                                        amount = finalAmount,
                                        type = type,
                                        category = selectedCategory,
                                        date = date,
                                        note = note.ifEmpty { "Transaction" },
                                        paymentMode = selectedAccountName,
                                        accountId = selectedAccountId,
                                        photoUri = photoPath,
                                        isRecurring = isRecurring,
                                        recurrencePeriod = if (isRecurring) recurrencePeriod else null,
                                        tags = tagsStr
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("submit_button")
                    ) {
                        Text("Save File")
                    }
                }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, color: String, iconName: String, type: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var selectedColor by remember { mutableStateOf("#FF9800") } // Defaults to orange

    val colors = listOf(
        "#FF9800", "#2196F3", "#4CAF50", "#E91E63", 
        "#9C27B0", "#FFC107", "#00BCD4", "#795548", 
        "#009688", "#E65100", "#1B5E20", "#311B92"
    )

    var selectedIconName by remember { mutableStateOf("📁") }

    val emojiGroups = listOf(
        "Food & Drink" to listOf("☕", "🍕", "🍔", "🍟", "🌮", "🍜", "🍣", "🍩", "🍪", "🍺", "🍷", "🍰", "🥗", "🍳", "🥤", "🍱", "🍝", "🥩", "🍦", "🍿"),
        "Transport" to listOf("🚗", "🚕", "🚌", "🚲", "✈️", "🚢", "🚂", "🛵", "🚁", "🚀", "🚇", "🚊", "🛴", "🏎️"),
        "Shopping" to listOf("🛍️", "👗", "👟", "👠", "🎁", "🛒", "👕", "👖", "👔", "👛", "💄"),
        "Home & Bills" to listOf("🏠", "🏢", "🏡", "🏦", "🏪", "💒", "🏬", "🏨", "🏧"),
        "Health" to listOf("💊", "💉", "💪", "🧘", "🧠", "🦷", "🫀", "🏥", "🩺", "❤️"),
        "Entertainment" to listOf("🎬", "🎮", "🎵", "🎧", "⚽", "🎯", "🎪", "🎭", "🎶", "🎸", "🎨", "🎲", "🏀", "🎉"),
        "Finance" to listOf("💰", "💳", "💵", "💷", "💶", "💎", "📊", "📈", "📉", "🧾"),
        "Nature" to listOf("🌿", "🌻", "🌊", "🌈", "🌸", "🌺", "🌳", "🌞", "🌙", "⭐", "🔥", "🌴", "🍃"),
        "Travel" to listOf("🗺️", "🧳", "🌍", "🏖️", "🏔️", "🏝️", "🌋", "🏕️", "🗽"),
        "Tech & Work" to listOf("💻", "📱", "🖥️", "💼", "🔧", "💡", "🔌", "⚙️", "🖨️", "⌨️", "📷", "👑"),
        "Misc" to listOf("🔑", "🎓", "📚", "✉️", "📝", "🏆", "💎", "🎯", "💌", "🔔", "🎀", "🧸")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = "Add Custom Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("category_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Custom category type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("EXPENSE", "INCOME").forEach { catType ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { type = catType }
                        ) {
                            RadioButton(selected = type == catType, onClick = { type = catType })
                            Text(catType.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pick a Theme Color:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Colors Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.take(6).forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(col, RoundedCornerShape(18.dp))
                                .clickable { selectedColor = hex }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(20.dp), tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.drop(6).forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(col, RoundedCornerShape(18.dp))
                                .clickable { selectedColor = hex }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(20.dp), tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pick an Emoji:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedIconName,
                        fontSize = 32.sp
                    )
                    OutlinedTextField(
                        value = selectedIconName,
                        onValueChange = { selectedIconName = if (it.isEmpty()) "\uD83D\uDCC1" else it },
                        label = { Text("Type or paste any emoji") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Pick a common one:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                emojiGroups.forEach { (groupName, emojis) ->
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    emojis.chunked(7).forEach { rowEmojis ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowEmojis.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (selectedIconName == emoji) Color(android.graphics.Color.parseColor(selectedColor)).copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedIconName = emoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 16.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty()) {
                                onSave(name.trim(), selectedColor, selectedIconName, type)
                            }
                        },
                        modifier = Modifier.testTag("submit_button")
                    ) { Text("Create") }
                }
            }
        }
    }
}

@Composable
fun AddBudgetDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (categoryName: String, limitAmount: Double, period: String, isRollover: Boolean) -> Unit
) {
    var limitStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Overall") }
    var period by remember { mutableStateOf("MONTHLY") }
    var isRollover by remember { mutableStateOf(false) }

    var categoryExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Configure Budget Limit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // CategoryDropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Category") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
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
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Overall Budget (Total Wallet)") },
                            onClick = {
                                selectedCategory = "Overall"
                                categoryExpanded = false
                            }
                        )
                        categories.filter { it.type == "EXPENSE" }.forEach { cat ->
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

                Spacer(modifier = Modifier.height(12.dp))

                // Limit Amount
                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Maximum Budget (INR)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("budget_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Period Choice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("MONTHLY", "WEEKLY").forEach { budgetPeriod ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { period = budgetPeriod }
                        ) {
                            RadioButton(selected = period == budgetPeriod, onClick = { period = budgetPeriod })
                            Text(budgetPeriod.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rollover Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isRollover, onCheckedChange = { isRollover = it })
                    Text("Rollover unused balance to next cycle", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val limit = limitStr.toDoubleOrNull() ?: 0.0
                            if (limit > 0) {
                                onSave(selectedCategory, limit, period, isRollover)
                            }
                        },
                        modifier = Modifier.testTag("submit_button")
                    ) { Text("Apply Budget") }
                }
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, balance: Double, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balanceStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("BANK") }
    var selectedColor by remember { mutableStateOf("#2196F3") }

    val accountTypes = listOf("CASH", "BANK", "CREDIT_CARD", "WALLET")
    val colors = listOf("#4CAF50", "#2196F3", "#F44336", "#00BCD4", "#9C27B0", "#E91E63", "#795548")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Add Account / Card",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account / Card Name (e.g. Axis Bank)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Starting Balance (INR)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_balance_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Account type Dropdown
                var typeExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { typeExpanded = true },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        accountTypes.forEach { accType ->
                            DropdownMenuItem(
                                text = { Text(accType) },
                                onClick = {
                                    type = accType
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pick Card Theme Color:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(col, RoundedCornerShape(18.dp))
                                .clickable { selectedColor = hex }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(20.dp), tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val initialBal = balanceStr.toDoubleOrNull() ?: 0.0
                            if (name.trim().isNotEmpty()) {
                                onSave(name.trim(), type, initialBal, selectedColor)
                            }
                        },
                        modifier = Modifier.testTag("submit_button")
                    ) { Text("Create Account") }
                }
            }
        }
    }
}

@Composable
fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var balanceStr by remember { mutableStateOf(account.balance.toBigDecimal().stripTrailingZeros().toPlainString()) }
    var type by remember { mutableStateOf(account.type) }
    var selectedColor by remember { mutableStateOf(account.color) }

    val accountTypes = listOf("CASH", "BANK", "CREDIT_CARD", "WALLET")
    val colors = listOf("#4CAF50", "#2196F3", "#F44336", "#00BCD4", "#9C27B0", "#E91E63", "#795548")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Edit Account / Card",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account / Card Name (e.g. Axis Bank)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_account_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Balance (INR)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_account_balance_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                var typeExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { typeExpanded = true },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        accountTypes.forEach { accType ->
                            DropdownMenuItem(
                                text = { Text(accType) },
                                onClick = {
                                    type = accType
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pick Card Theme Color:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(col, RoundedCornerShape(18.dp))
                                .clickable { selectedColor = hex }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(20.dp), tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val bal = balanceStr.toDoubleOrNull() ?: account.balance
                            if (name.trim().isNotEmpty()) {
                                onSave(account.copy(name = name.trim(), type = type, balance = bal, color = selectedColor))
                            }
                        },
                        modifier = Modifier.testTag("submit_button")
                    ) { Text("Update Account") }
                }
            }
        }
    }
}

@Composable
fun TransferFundsDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onTransfer: (fromAccountId: Long, toAccountId: Long, amount: Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    
    val defaultFrom = accounts.firstOrNull()
    var fromAccountId by remember { mutableStateOf(defaultFrom?.id ?: 1L) }
    var fromAccountName by remember { mutableStateOf(defaultFrom?.name ?: "Cash") }

    val defaultTo = accounts.getOrNull(1) ?: accounts.firstOrNull()
    var toAccountId by remember { mutableStateOf(defaultTo?.id ?: 1L) }
    var toAccountName by remember { mutableStateOf(defaultTo?.name ?: "SBI Savings") }

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Transfer Funds",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Source Account
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = fromAccountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transfer From") },
                        leadingIcon = { Icon(Icons.Default.Output, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fromExpanded = true },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (Balance: ₹${acc.balance})") },
                                onClick = {
                                    fromAccountId = acc.id
                                    fromAccountName = acc.name
                                    fromExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Target Account
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = toAccountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transfer To") },
                        leadingIcon = { Icon(Icons.Default.Input, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toExpanded = true },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    DropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (Balance: ₹${acc.balance})") },
                                onClick = {
                                    toAccountId = acc.id
                                    toAccountName = acc.name
                                    toExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Transfer Amount (INR)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && fromAccountId != toAccountId) {
                                onTransfer(fromAccountId, toAccountId, amt)
                            }
                        },
                        modifier = Modifier.testTag("submit_button")
                    ) { Text("Transfer") }
                }
            }
        }
    }
}

package com.mine.expenseiq.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mine.expenseiq.data.local.AppDatabase
import com.mine.expenseiq.data.model.*
import com.mine.expenseiq.data.repository.ExpenseRepository
import com.mine.expenseiq.utils.ParsedSms
import com.mine.expenseiq.utils.SmsTrigger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository
) : AndroidViewModel(application) {

    // --- DATA FLOWS ---
    val accounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<ExpenseTransaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SMS REALTIME FLOWS ---
    private val _pendingSms = MutableStateFlow<ParsedSms?>(null)
    val pendingSms: StateFlow<ParsedSms?> = _pendingSms.asStateFlow()

    private val _scannedSmsList = MutableStateFlow<List<com.mine.expenseiq.utils.ParsedSmsWithRaw>>(emptyList())
    val scannedSmsList: StateFlow<List<com.mine.expenseiq.utils.ParsedSmsWithRaw>> = _scannedSmsList.asStateFlow()

    init {
        // Collect background-received real SMS transactions in real-time
        viewModelScope.launch {
            SmsTrigger.smsFlow.collect { parsedSms ->
                _pendingSms.value = parsedSms
            }
        }
    }

    fun setPendingSms(parsed: ParsedSms?) {
        _pendingSms.value = parsed
    }

    fun triggerSimulatedSms(body: String) {
        viewModelScope.launch {
            val parsed = com.mine.expenseiq.utils.SmsParser.parse(body)
            if (parsed != null) {
                _pendingSms.value = parsed
            }
        }
    }

    fun clearPendingSms() {
        _pendingSms.value = null
    }

    fun scanInboxForTransactions() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val uri = android.net.Uri.parse("content://sms/inbox")
                val projection = arrayOf("_id", "address", "body", "date")
                val cursor = context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "date DESC"
                )
                
                val results = mutableListOf<com.mine.expenseiq.utils.ParsedSmsWithRaw>()
                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow("_id")
                    val addrCol = it.getColumnIndexOrThrow("address")
                    val bodyCol = it.getColumnIndexOrThrow("body")
                    val dateCol = it.getColumnIndexOrThrow("date")
                    
                    var count = 0
                    while (it.moveToNext() && count < 500) {
                        count++
                        val id = it.getString(idCol)
                        val address = it.getString(addrCol) ?: "Unknown"
                        val body = it.getString(bodyCol) ?: ""
                        val date = it.getLong(dateCol)
                        
                        val parsed = com.mine.expenseiq.utils.SmsParser.parse(body)
                        if (parsed != null) {
                            val accts = accounts.value
                            val cats = categories.value
                            
                            val matchedAcc = accts.firstOrNull { acc -> acc.name == parsed.paymentMode } ?: accts.firstOrNull()
                            val initialCategory = if (parsed.isExpense) {
                                cats.firstOrNull { c -> c.type == "EXPENSE" }?.name ?: "Utilities"
                            } else {
                                cats.firstOrNull { c -> c.type == "INCOME" }?.name ?: "Salary"
                            }
                            
                            results.add(
                                com.mine.expenseiq.utils.ParsedSmsWithRaw(
                                    id = id,
                                    sender = address,
                                    date = date,
                                    rawBody = body,
                                    amount = parsed.amount,
                                    merchant = parsed.merchant,
                                    paymentMode = parsed.paymentMode,
                                    isExpense = parsed.isExpense,
                                    selectedCategory = initialCategory,
                                    selectedAccountId = matchedAcc?.id ?: 1L
                                )
                            )
                        }
                    }
                }
                _scannedSmsList.value = results
            } catch (e: Exception) {
                android.util.Log.e("ExpenseViewModel", "Error scanning past inbox SMS: ${e.message}", e)
            }
        }
    }

    fun toggleSelectAllSms(isSelected: Boolean) {
        _scannedSmsList.value = _scannedSmsList.value.map {
            it.copy(isSelected = isSelected)
        }
    }

    fun toggleSmsSelection(id: String) {
        _scannedSmsList.value = _scannedSmsList.value.map {
            if (it.id == id) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }
    }

    fun toggleSmsExpansion(id: String) {
        _scannedSmsList.value = _scannedSmsList.value.map {
            if (it.id == id) {
                it.copy(isExpanded = !it.isExpanded)
            } else {
                it
            }
        }
    }

    fun updateSmsDetails(
        id: String,
        amount: Double,
        merchant: String,
        category: String,
        accountId: Long,
        isExpense: Boolean
    ) {
        _scannedSmsList.value = _scannedSmsList.value.map {
            if (it.id == id) {
                it.copy(
                    amount = amount,
                    merchant = merchant,
                    selectedCategory = category,
                    selectedAccountId = accountId,
                    isExpense = isExpense
                )
            } else {
                it
            }
        }
    }

    fun logSelectedSmsTransactions() {
        viewModelScope.launch {
            val selectedItems = _scannedSmsList.value.filter { it.isSelected }
            selectedItems.forEach { item ->
                val activeAccount = accounts.value.firstOrNull { it.id == item.selectedAccountId }
                val tx = ExpenseTransaction(
                    amount = item.amount,
                    type = if (item.isExpense) "EXPENSE" else "INCOME",
                    category = item.selectedCategory,
                    date = item.date,
                    note = item.merchant,
                    paymentMode = activeAccount?.name ?: item.paymentMode,
                    accountId = item.selectedAccountId,
                    tags = "SMS Sync"
                )
                repository.addTransaction(tx)
            }
            _scannedSmsList.value = emptyList()
        }
    }

    fun clearScannedSmsList() {
        _scannedSmsList.value = emptyList()
    }

    // --- TRANSACTION ACTIONS ---
    fun addTransaction(
        amount: Double,
        type: String,
        category: String,
        date: Long,
        note: String,
        paymentMode: String,
        accountId: Long,
        photoUri: String? = null,
        isRecurring: Boolean = false,
        recurrencePeriod: String? = null,
        tags: String = ""
    ) {
        viewModelScope.launch {
            val tx = ExpenseTransaction(
                amount = amount,
                type = type,
                category = category,
                date = date,
                note = note,
                paymentMode = paymentMode,
                accountId = accountId,
                photoUri = photoUri,
                isRecurring = isRecurring,
                recurrencePeriod = recurrencePeriod,
                tags = tags
            )
            repository.addTransaction(tx)
        }
    }

    fun updateTransaction(transaction: ExpenseTransaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: ExpenseTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // --- ACCOUNT ACTIONS ---
    fun addAccount(name: String, type: String, balance: Double, color: String) {
        viewModelScope.launch {
            repository.insertAccount(Account(name = name, type = type, balance = balance, color = color))
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    // --- TRANSFER BETWEEN ACCOUNTS ---
    fun executeTransfer(fromAccountId: Long, toAccountId: Long, amount: Double) {
        viewModelScope.launch {
            val fromAcc = repository.getAccountById(fromAccountId) ?: return@launch
            val toAcc = repository.getAccountById(toAccountId) ?: return@launch
            
            val fromTx = ExpenseTransaction(
                amount = amount,
                type = "EXPENSE",
                category = "Savings",
                date = System.currentTimeMillis(),
                note = "Transfer -> ${toAcc.name}",
                paymentMode = fromAcc.name,
                accountId = fromAccountId
            )
            val toTx = ExpenseTransaction(
                amount = amount,
                type = "INCOME",
                category = "Savings",
                date = System.currentTimeMillis(),
                note = "Transfer <- ${fromAcc.name}",
                paymentMode = toAcc.name,
                accountId = toAccountId
            )
            repository.addTransaction(fromTx)
            repository.addTransaction(toTx)
        }
    }

    // --- CATEGORY ACTIONS ---
    fun addCategory(name: String, color: String, iconName: String, type: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, color = color, iconName = iconName, type = type))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // --- BUDGET ACTIONS ---
    fun addBudget(categoryName: String, limitAmount: Double, period: String, isRollover: Boolean) {
        viewModelScope.launch {
            repository.insertBudget(
                Budget(
                    categoryName = categoryName,
                    limitAmount = limitAmount,
                    period = period,
                    isRollover = isRollover
                )
            )
        }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            repository.updateBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // --- DATA BACKUP AND RESTORE ---
    fun exportDataAsJson(): String {
        try {
            val root = org.json.JSONObject()

            // Accounts
            val accountsArray = org.json.JSONArray()
            accounts.value.forEach { acc ->
                val accObj = org.json.JSONObject().apply {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("type", acc.type)
                    put("balance", acc.balance)
                    put("color", acc.color)
                }
                accountsArray.put(accObj)
            }
            root.put("accounts", accountsArray)

            // Categories
            val categoriesArray = org.json.JSONArray()
            categories.value.forEach { cat ->
                val catObj = org.json.JSONObject().apply {
                    put("name", cat.name)
                    put("color", cat.color)
                    put("iconName", cat.iconName)
                    put("type", cat.type)
                }
                categoriesArray.put(catObj)
            }
            root.put("categories", categoriesArray)

            // Budgets
            val budgetsArray = org.json.JSONArray()
            budgets.value.forEach { b ->
                val bObj = org.json.JSONObject().apply {
                    put("id", b.id)
                    put("categoryName", b.categoryName)
                    put("limitAmount", b.limitAmount)
                    put("period", b.period)
                    put("isRollover", b.isRollover)
                }
                budgetsArray.put(bObj)
            }
            root.put("budgets", budgetsArray)

            // Transactions
            val transactionsArray = org.json.JSONArray()
            transactions.value.forEach { tx ->
                val txObj = org.json.JSONObject().apply {
                    put("id", tx.id)
                    put("amount", tx.amount)
                    put("type", tx.type)
                    put("category", tx.category)
                    put("date", tx.date)
                    put("note", tx.note)
                    put("paymentMode", tx.paymentMode)
                    put("accountId", tx.accountId)
                    tx.photoUri?.let { put("photoUri", it) }
                    put("isRecurring", tx.isRecurring)
                    tx.recurrencePeriod?.let { put("recurrencePeriod", it) }
                    put("tags", tx.tags)
                }
                transactionsArray.put(txObj)
            }
            root.put("transactions", transactionsArray)

            return root.toString(2)
        } catch (e: Exception) {
            android.util.Log.e("ExpenseViewModel", "Failed to export backup data", e)
            return ""
        }
    }

    fun importDataFromJson(jsonString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(jsonString)

                // 1. Parse Accounts
                val accountsList = mutableListOf<Account>()
                val accountsArray = root.optJSONArray("accounts")
                if (accountsArray != null) {
                    for (i in 0 until accountsArray.length()) {
                        val obj = accountsArray.getJSONObject(i)
                        accountsList.add(
                            Account(
                                id = obj.optLong("id", 0L),
                                name = obj.getString("name"),
                                type = obj.getString("type"),
                                balance = obj.getDouble("balance"),
                                color = obj.getString("color")
                            )
                        )
                    }
                }

                // 2. Parse Categories
                val categoriesList = mutableListOf<Category>()
                val categoriesArray = root.optJSONArray("categories")
                if (categoriesArray != null) {
                    for (i in 0 until categoriesArray.length()) {
                        val obj = categoriesArray.getJSONObject(i)
                        categoriesList.add(
                            Category(
                                name = obj.getString("name"),
                                color = obj.getString("color"),
                                iconName = obj.getString("iconName"),
                                type = obj.getString("type")
                            )
                        )
                    }
                }

                // 3. Parse Budgets
                val budgetsList = mutableListOf<Budget>()
                val budgetsArray = root.optJSONArray("budgets")
                if (budgetsArray != null) {
                    for (i in 0 until budgetsArray.length()) {
                        val obj = budgetsArray.getJSONObject(i)
                        budgetsList.add(
                            Budget(
                                id = obj.optLong("id", 0L),
                                categoryName = obj.getString("categoryName"),
                                limitAmount = obj.getDouble("limitAmount"),
                                period = obj.getString("period"),
                                isRollover = obj.optBoolean("isRollover", false)
                            )
                        )
                    }
                }

                // 4. Parse Transactions
                val transactionsList = mutableListOf<ExpenseTransaction>()
                val transactionsArray = root.optJSONArray("transactions")
                if (transactionsArray != null) {
                    for (i in 0 until transactionsArray.length()) {
                        val obj = transactionsArray.getJSONObject(i)
                        transactionsList.add(
                            ExpenseTransaction(
                                id = obj.optLong("id", 0L),
                                amount = obj.getDouble("amount"),
                                type = obj.getString("type"),
                                category = obj.getString("category"),
                                date = obj.getLong("date"),
                                note = obj.getString("note"),
                                paymentMode = obj.getString("paymentMode"),
                                accountId = obj.getLong("accountId"),
                                photoUri = if (obj.has("photoUri")) obj.getString("photoUri") else null,
                                isRecurring = obj.optBoolean("isRecurring", false),
                                recurrencePeriod = if (obj.has("recurrencePeriod")) obj.getString("recurrencePeriod") else null,
                                tags = obj.optString("tags", "")
                            )
                        )
                    }
                }

                repository.importBackup(
                    accounts = accountsList,
                    categories = categoriesList,
                    budgets = budgetsList,
                    transactions = transactionsList
                )
                
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("ExpenseViewModel", "Failed to deserialize import data", e)
                onError(e.localizedMessage ?: "Invalid JSON backup data format.")
            }
        }
    }

    // --- FACTORY FOR VIEWMODEL ---
    class Factory(
        private val application: Application,
        private val repository: ExpenseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                return ExpenseViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class representation")
        }
    }
}

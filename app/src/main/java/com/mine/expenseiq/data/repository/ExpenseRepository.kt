package com.mine.expenseiq.data.repository

import com.mine.expenseiq.data.local.ExpenseDao
import com.mine.expenseiq.data.model.*
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allAccounts: Flow<List<Account>> = expenseDao.getAllAccounts()
    val allCategories: Flow<List<Category>> = expenseDao.getAllCategories()
    val allBudgets: Flow<List<Budget>> = expenseDao.getAllBudgets()
    val allTransactions: Flow<List<ExpenseTransaction>> = expenseDao.getAllTransactions()

    suspend fun getAccountById(id: Long): Account? = expenseDao.getAccountById(id)
    suspend fun insertAccount(account: Account): Long = expenseDao.insertAccount(account)
    suspend fun updateAccount(account: Account) = expenseDao.updateAccount(account)
    suspend fun deleteAccount(account: Account) = expenseDao.deleteAccount(account)

    suspend fun insertCategory(category: Category) = expenseDao.insertCategory(category)
    suspend fun deleteCategory(category: Category) = expenseDao.deleteCategory(category)

    suspend fun insertBudget(budget: Budget): Long = expenseDao.insertBudget(budget)
    suspend fun updateBudget(budget: Budget) = expenseDao.updateBudget(budget)
    suspend fun deleteBudget(budget: Budget) = expenseDao.deleteBudget(budget)

    fun getTransactionsByAccount(accountId: Long): Flow<List<ExpenseTransaction>> = expenseDao.getTransactionsByAccount(accountId)
    suspend fun getTransactionById(id: Long): ExpenseTransaction? = expenseDao.getTransactionById(id)

    suspend fun addTransaction(transaction: ExpenseTransaction) {
        val account = expenseDao.getAccountById(transaction.accountId)
        if (account != null) {
            val newBalance = if (transaction.type == "INCOME") {
                account.balance + transaction.amount
            } else {
                account.balance - transaction.amount
            }
            expenseDao.updateAccount(account.copy(balance = newBalance))
        }
        expenseDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: ExpenseTransaction) {
        val account = expenseDao.getAccountById(transaction.accountId)
        if (account != null) {
            val newBalance = if (transaction.type == "INCOME") {
                account.balance - transaction.amount
            } else {
                account.balance + transaction.amount
            }
            expenseDao.updateAccount(account.copy(balance = newBalance))
        }
        expenseDao.deleteTransaction(transaction)
    }

    suspend fun updateTransaction(newTx: ExpenseTransaction) {
        val oldTx = expenseDao.getTransactionById(newTx.id) ?: return
        
        // 1. Reverse the effect of the old transaction on the old account
        val oldAccount = expenseDao.getAccountById(oldTx.accountId)
        if (oldAccount != null) {
            val restoredBalance = if (oldTx.type == "INCOME") {
                oldAccount.balance - oldTx.amount
            } else {
                oldAccount.balance + oldTx.amount
            }
            expenseDao.updateAccount(oldAccount.copy(balance = restoredBalance))
        }

        // 2. Apply the effect of the new transaction on the new account
        val newAccount = expenseDao.getAccountById(newTx.accountId)
        if (newAccount != null) {
            val baseBalance = if (newTx.accountId == oldTx.accountId && oldAccount != null) {
                // If it's the exact same account, read from oldAccount's intermediate restored balance
                val restoredBalance = if (oldTx.type == "INCOME") {
                    oldAccount.balance - oldTx.amount
                } else {
                    oldAccount.balance + oldTx.amount
                }
                restoredBalance
            } else {
                newAccount.balance
            }

            val appliedBalance = if (newTx.type == "INCOME") {
                baseBalance + newTx.amount
            } else {
                baseBalance - newTx.amount
            }
            expenseDao.updateAccount(newAccount.copy(balance = appliedBalance))
        }

        // 3. Persist the updated transaction fields
        expenseDao.updateTransaction(newTx)
    }

    suspend fun clearAllData() {
        expenseDao.deleteAllTransactions()
        expenseDao.deleteAllBudgets()
        expenseDao.deleteAllCategories()
        expenseDao.deleteAllAccounts()
    }

    suspend fun importBackup(
        accounts: List<Account>,
        categories: List<Category>,
        budgets: List<Budget>,
        transactions: List<ExpenseTransaction>
    ) {
        clearAllData()
        accounts.forEach { expenseDao.insertAccount(it) }
        categories.forEach { expenseDao.insertCategory(it) }
        budgets.forEach { expenseDao.insertBudget(it) }
        transactions.forEach { expenseDao.insertTransaction(it) }
    }
}


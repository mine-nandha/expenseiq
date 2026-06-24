package com.mine.expenseiq.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // CASH, BANK, CREDIT_CARD, WALLET
    val balance: Double,
    val color: String // Hex representation (e.g. #4CAF50, #2196F3)
) : Serializable

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val name: String,
    val color: String, // Hex representation (e.g. #FF5722)
    val iconName: String, // e.g. "Restaurant", "DirectionsCar", "Home", "ShoppingBag"
    val type: String // INCOME, EXPENSE
) : Serializable

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryName: String, // e.g. "Food", "Transport" or "Overall"
    val limitAmount: Double,
    val period: String, // MONTHLY, WEEKLY
    val isRollover: Boolean = false
) : Serializable

@Entity(tableName = "transactions")
data class ExpenseTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // INCOME, EXPENSE
    val category: String,
    val date: Long, // timestamp
    val note: String,
    val paymentMode: String, // e.g. "SBI Bank", "Cash", "Paytm Wallet"
    val accountId: Long, // references Account.id
    val photoUri: String? = null, // receipt photo
    val isRecurring: Boolean = false,
    val recurrencePeriod: String? = null, // DAILY, WEEKLY, MONTHLY
    val tags: String = "" // Comma-separated list of tags
) : Serializable

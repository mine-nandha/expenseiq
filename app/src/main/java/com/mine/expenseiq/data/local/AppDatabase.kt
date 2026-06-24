package com.mine.expenseiq.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mine.expenseiq.data.model.*

@Database(
    entities = [Account::class, Category::class, Budget::class, ExpenseTransaction::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_iq_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default Accounts
                        db.execSQL("INSERT INTO accounts (name, type, balance, color) VALUES ('Cash', 'CASH', 2500.0, '#4CAF50')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, color) VALUES ('SBI Savings', 'BANK', 25000.0, '#2196F3')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, color) VALUES ('HDFC Credit Card', 'CREDIT_CARD', -1500.0, '#F44336')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, color) VALUES ('Paytm Wallet', 'WALLET', 1200.0, '#00BCD4')")

                        // Prepopulate default Categories
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('Food', '#FF9800', 'Restaurant', 'EXPENSE')")
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('Transport', '#2196F3', 'DirectionsCar', 'EXPENSE')")
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('Rent', '#9C27B0', 'Home', 'EXPENSE')")
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('Shopping', '#E91E63', 'ShoppingCart', 'EXPENSE')")
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('Health', '#4CAF50', 'LocalHospital', 'EXPENSE')")
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('Entertainment', '#FFC107', 'Movie', 'EXPENSE')")
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('Utilities', '#00BCD4', 'Power', 'EXPENSE')")
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('EMI', '#795548', 'CreditCard', 'EXPENSE')")
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('Savings', '#009688', 'Savings', 'EXPENSE')")
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('Salary', '#4CAF50', 'AttachMoney', 'INCOME')")
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('Refund', '#03A9F4', 'Undo', 'INCOME')")
                        db.execSQL("INSERT INTO categories (name, color, iconName, type) VALUES ('Other Income', '#8BC34A', 'AddCircle', 'INCOME')")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

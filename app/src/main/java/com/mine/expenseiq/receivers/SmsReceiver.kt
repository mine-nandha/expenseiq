package com.mine.expenseiq.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import com.mine.expenseiq.data.local.AppDatabase
import com.mine.expenseiq.data.model.ExpenseTransaction
import com.mine.expenseiq.data.repository.ExpenseRepository
import com.mine.expenseiq.utils.SmsParser
import com.mine.expenseiq.utils.SmsTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ExpenseIQSmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            try {
                val bundle = intent.extras ?: return
                val pdus = bundle.get("pdus") as? Array<*> ?: return
                val format = bundle.getString("format")
                
                var fullBody = ""
                var senderAddress = "Unknown"
                
                for (pdu in pdus) {
                    val msgBytes = pdu as? ByteArray ?: continue
                    val message = SmsMessage.createFromPdu(msgBytes, format)
                    fullBody += message.messageBody ?: ""
                    senderAddress = message.displayOriginatingAddress ?: message.originatingAddress ?: "Unknown"
                }
                
                if (fullBody.isNotEmpty()) {
                    Log.d(TAG, "SMS Received from $senderAddress: $fullBody")
                    
                    val parsed = SmsParser.parse(fullBody)
                    if (parsed != null) {
                        Log.d(TAG, "Parsed transacted SMS successfully: $parsed")
                        
                        // Use goAsync to complete database operation off the main thread safely
                        val pendingResult = goAsync()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val database = AppDatabase.getDatabase(context)
                                val dao = database.expenseDao()
                                val repository = ExpenseRepository(dao)

                                // Retrieve accounts and categories to match details
                                val accountsList = dao.getAllAccounts().first()
                                val categoriesList = dao.getAllCategories().first()

                                val matchedAcc = accountsList.firstOrNull { acc -> 
                                    acc.name.equals(parsed.paymentMode, ignoreCase = true) 
                                } ?: accountsList.firstOrNull()
                                
                                val initialCategory = if (parsed.isExpense) {
                                    categoriesList.firstOrNull { c -> 
                                        c.type == "EXPENSE" && c.name.equals("Utilities", ignoreCase = true) 
                                    }?.name 
                                        ?: categoriesList.firstOrNull { c -> c.type == "EXPENSE" }?.name 
                                        ?: "Utilities"
                                } else {
                                    categoriesList.firstOrNull { c -> 
                                        c.type == "INCOME" && c.name.equals("Salary", ignoreCase = true) 
                                    }?.name 
                                        ?: categoriesList.firstOrNull { c -> c.type == "INCOME" }?.name 
                                        ?: "Salary"
                                }

                                val transaction = ExpenseTransaction(
                                    amount = parsed.amount,
                                    type = if (parsed.isExpense) "EXPENSE" else "INCOME",
                                    category = initialCategory,
                                    date = System.currentTimeMillis(),
                                    note = parsed.merchant,
                                    paymentMode = matchedAcc?.name ?: parsed.paymentMode,
                                    accountId = matchedAcc?.id ?: 1L,
                                    tags = "SMS Auto"
                                )
                                
                                // Only trigger inside the app for UI response if active (where user can approve and log it)
                                SmsTrigger.triggerSms(parsed)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error writing transaction to database on SMS receive: ${e.message}", e)
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS intent context: ${e.message}", e)
            }
        }
    }
}

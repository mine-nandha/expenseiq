package com.mine.expenseiq.utils

import android.util.Log

data class ParsedSms(
    val amount: Double,
    val merchant: String,
    val paymentMode: String, // e.g. "SBI Savings", "HDFC Credit Card", "Paytm Wallet"
    val isExpense: Boolean = true
)

data class ParsedSmsWithRaw(
    val id: String,
    val sender: String,
    val date: Long,
    val rawBody: String,
    val amount: Double,
    val merchant: String,
    val paymentMode: String,
    val isExpense: Boolean,
    val selectedCategory: String,
    val selectedAccountId: Long,
    val isSelected: Boolean = true,
    val isExpanded: Boolean = false
)

object SmsParser {
    private const val TAG = "SmsParser"

    fun parse(body: String): ParsedSms? {
        try {
            val lowercaseBody = body.lowercase()
            // Quick check for transactional keywords
            if (!lowercaseBody.contains("debited") && !lowercaseBody.contains("charged") && 
                !lowercaseBody.contains("sent") && !lowercaseBody.contains("spent") &&
                !lowercaseBody.contains("credited") && !lowercaseBody.contains("received") &&
                !lowercaseBody.contains("txn of")) {
                return null
            }

            // Amount parsing pattern: e.g. 'Rs. 150.00', 'Rs 250', 'INR 1,200.00', 'Txn of Rs.540'
            val amountRegex = Regex("(?i)(?:rs\\.?|inr)\\s*([0-9,]+\\.[0-9]{2}|[0-9,]+)")
            val amountMatch = amountRegex.find(body) ?: return null
            val amountStr = amountMatch.groupValues[1].replace(",", "")
            val amount = amountStr.toDoubleOrNull() ?: return null

            // Merchant parsing pattern
            // Matches 'at <Merchant>' or 'to <Merchant>' or 'vpa <Merchant>' or 'ref: <Merchant>'
            var merchant = "Unknown Merchant"
            val atMatch = Regex("(?i)\\bat\\s+([A-Za-z0-9\\s&-]+)(?:\\bon|\\busing|\\bref|\\.|$)").find(body)
            val toMatch = Regex("(?i)\\bto\\s+([A-Za-z0-9\\s&-]+)(?:\\bon|\\busing|\\bref|\\.|$)").find(body)
            val vpaMatch = Regex("(?i)\\bvpa\\s+([A-Za-z0-9\\s&@.-]+)").find(body)
            val infoMatch = Regex("(?i)\\binfo:\\s*([A-Za-z0-9\\s&-]+)").find(body)

            if (atMatch != null) {
                merchant = atMatch.groupValues[1].trim()
            } else if (toMatch != null) {
                merchant = toMatch.groupValues[1].trim()
            } else if (vpaMatch != null) {
                merchant = vpaMatch.groupValues[1].trim()
            } else if (infoMatch != null) {
                merchant = infoMatch.groupValues[1].trim()
            }

            // Clean up merchant name
            merchant = merchant.replace(Regex("(?i)on\\s*$|using\\s*$|ref\\s*$"), "").trim()
            if (merchant.length > 25) {
                merchant = merchant.substring(0, 25).trim() + "..."
            }
            if (merchant.isEmpty()) {
                merchant = "Merchant Transaction"
            }

            // Detect suitable account from SMS body
            var paymentMode = "Cash" // Default fallback
            if (lowercaseBody.contains("credit card") || lowercaseBody.contains("hdfc card") || lowercaseBody.contains("cc ending")) {
                paymentMode = "HDFC Credit Card"
            } else if (lowercaseBody.contains("paytm") || lowercaseBody.contains("wallet") || lowercaseBody.contains("phonepe")) {
                paymentMode = "Paytm Wallet"
            } else if (lowercaseBody.contains("sbi") || lowercaseBody.contains("state bank")) {
                paymentMode = "SBI Savings"
            } else if (lowercaseBody.contains("hdfc") || lowercaseBody.contains("bank a/c")) {
                paymentMode = "SBI Savings" // Match to default saving account
            } else if (lowercaseBody.contains("upi")) {
                paymentMode = "SBI Savings" // Standard UPI source
            }

            val isExpense = !lowercaseBody.contains("credited") && !lowercaseBody.contains("received")

            return ParsedSms(
                amount = amount,
                merchant = merchant,
                paymentMode = paymentMode,
                isExpense = isExpense
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS: ${e.message}", e)
            return null
        }
    }
}

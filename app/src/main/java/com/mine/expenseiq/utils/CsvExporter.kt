package com.mine.expenseiq.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.mine.expenseiq.data.model.ExpenseTransaction
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private fun escapeCsvField(field: String): String {
        val escaped = field.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    /**
     * Generate complete CSV text content from a list of transactions
     */
    fun generateCsvContent(transactions: List<ExpenseTransaction>): String {
        val header = "ID,Amount,Type,Category,Date,Note,Payment Mode,Account ID,Is Recurring,Recurrence Period,Tags"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        return buildString {
            append(header)
            append("\n")
            transactions.forEach { tx ->
                val id = tx.id
                val amount = tx.amount
                val type = tx.type
                val category = escapeCsvField(tx.category)
                val dateStr = dateFormat.format(Date(tx.date))
                val note = escapeCsvField(tx.note)
                val paymentMode = escapeCsvField(tx.paymentMode)
                val accountId = tx.accountId
                val isRecurring = tx.isRecurring
                val recurrencePeriod = escapeCsvField(tx.recurrencePeriod ?: "")
                val tags = escapeCsvField(tx.tags)

                append("$id,$amount,$type,$category,$dateStr,$note,$paymentMode,$accountId,$isRecurring,$recurrencePeriod,$tags")
                append("\n")
            }
        }
    }

    /**
     * Direct saving of CSV text to the Device's Public "Downloads" directory using MediaStore for optimal compatibility.
     * No SAF selector dialog is popped.
     */
    fun saveCsvToDownloads(context: Context, csvContent: String): Uri? {
        val fileName = "expense_iq_transactions_${System.currentTimeMillis()}.csv"
        try {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(csvContent)
                    }
                }
                Toast.makeText(context, "Saved to Downloads as: $fileName", Toast.LENGTH_LONG).show()
                return uri
            } else {
                // Secondary legacy fallback
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.writeText(csvContent)
                Toast.makeText(context, "Saved to Downloads folder: $fileName", Toast.LENGTH_LONG).show()
                return Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Try saving in local Cache and telling the user
            try {
                val file = File(context.cacheDir, fileName)
                file.writeText(csvContent)
                Toast.makeText(context, "Saved to app cache: $fileName", Toast.LENGTH_SHORT).show()
                return Uri.fromFile(file)
            } catch (ex: Exception) {
                Toast.makeText(context, "Failed to write file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            return null
        }
    }

    /**
     * Shares the CSV content using the native Android share intent sheet.
     */
    fun shareCsvFile(context: Context, csvContent: String): Boolean {
        val fileName = "expense_iq_transactions_${System.currentTimeMillis()}.csv"
        try {
            val file = File(context.cacheDir, fileName)
            file.writeText(csvContent)

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ExpenseIQ CSV Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Share Transactions CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            return true
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    /**
     * Copy raw CSV text directly to Clipboard so the user can easily paste it.
     */
    fun copyCsvToClipboard(context: Context, csvContent: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("ExpenseIQ Transactions CSV", csvContent)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Copy failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Safely write transactions to a user selected SAF Uri
     */
    fun writeTransactionsToUri(context: Context, uri: Uri, transactions: List<ExpenseTransaction>): Boolean {
        try {
            val csvContent = generateCsvContent(transactions)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write(csvContent)
                }
            }
            Toast.makeText(context, "Transactions exported successfully!", Toast.LENGTH_SHORT).show()
            return true
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to write CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return false
        }
    }
}

package com.mine.expenseiq.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupFileUtil {

    const val BACKUP_MIME_TYPE = "application/json"

    fun suggestBackupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "expense_iq_backup_$timestamp.json"
    }

    /**
     * Direct saving of backup JSON to the Device's Public "Downloads" directory using MediaStore for optimal compatibility.
     * No SAF selector dialog is popped.
     */
    fun saveJsonToDownloads(context: Context, jsonContent: String): Uri? {
        val fileName = suggestBackupFileName()
        try {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, BACKUP_MIME_TYPE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Backup saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
                return uri
            } else {
                // Secondary legacy fallback
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                file.writeText(jsonContent)
                Toast.makeText(context, "Backup saved to Downloads folder: $fileName", Toast.LENGTH_LONG).show()
                return Uri.fromFile(file)
            }
        } catch (e: Exception) {
            // Try saving in local Cache and telling the user
            try {
                val file = java.io.File(context.cacheDir, fileName)
                file.writeText(jsonContent)
                Toast.makeText(context, "Backup saved to app cache: $fileName", Toast.LENGTH_SHORT).show()
                return Uri.fromFile(file)
            } catch (ex: Exception) {
                Toast.makeText(context, "Failed to write backup file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            return null
        }
    }

    /**
     * Safely write backup JSON to a user selected SAF Uri.
     */
    fun writeJsonToUri(context: Context, uri: Uri, jsonContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to write backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Read backup JSON content from a user selected SAF Uri. Returns null on failure.
     */
    fun readJsonFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to read backup file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }
}
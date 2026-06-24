package com.mine.expenseiq.workers

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mine.expenseiq.data.local.AppDatabase
import com.mine.expenseiq.data.model.ExpenseTransaction
import com.mine.expenseiq.data.repository.ExpenseRepository
import com.mine.expenseiq.utils.SmsParser
import kotlinx.coroutines.flow.first

class SmsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SmsSyncWorker"
        private const val PREFS_NAME = "sms_sync_prefs"
        private const val KEY_LAST_SCANNED_TIME = "last_scanned_time"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SmsSyncWorker is disabled. Background auto-updates are paused to ensure no transaction gets added unless explicitly approved by the user.")
        return Result.success()
    }
}

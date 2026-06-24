package com.mine.expenseiq.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SmsTrigger {
    // SharedFlow to deliver real-time bank SMS alerts to the active screen view models
    private val _smsFlow = MutableSharedFlow<ParsedSms>(extraBufferCapacity = 64)
    val smsFlow = _smsFlow.asSharedFlow()

    fun triggerSms(parsedSms: ParsedSms) {
        _smsFlow.tryEmit(parsedSms)
    }
}

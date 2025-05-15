package com.pplm.projectinventarisuas.utils.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TimerState {
    private val _timeInSeconds = MutableStateFlow(0)
    val timeInSeconds: StateFlow<Int> = _timeInSeconds

    fun updateTime(seconds: Int) {
            _timeInSeconds.value = seconds
        }

        fun reset() {
            _timeInSeconds.value = 0
        }
}

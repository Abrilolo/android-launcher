package com.adrian.minimallauncher.ui.components
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.LocalDateTime
import kotlinx.coroutines.delay
// Emits the current time, updated at the start of every minute.
@Composable
fun rememberCurrentMinute(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = LocalDateTime.now()
            now = current
            delay(60_000L - current.second * 1_000L - current.nano / 1_000_000L)
        }
    }
    return now
}

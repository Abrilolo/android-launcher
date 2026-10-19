package com.adrian.minimallauncher.ui.components
import android.app.AlarmManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
private val spanish = Locale.forLanguageTag("es")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", spanish)
private val alarmDayFormat = DateTimeFormatter.ofPattern("EEE HH:mm", spanish)
@Composable
fun ClockHeader(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val now = rememberCurrentMinute()
    val nextAlarm = remember(now) { nextAlarmText(context, now) }
    Column(modifier, Arrangement.spacedBy(4.dp)) {
        Text(now.format(timeFormat), style = MaterialTheme.typography.displayLarge)
        Text(now.format(dateFormat).uppercase(spanish), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (nextAlarm != null) {
            Text(nextAlarm, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
private fun nextAlarmText(context: Context, now: LocalDateTime): String? {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return null
    val trigger = alarmManager.nextAlarmClock?.triggerTime ?: return null
    val alarm = LocalDateTime.ofInstant(Instant.ofEpochMilli(trigger), ZoneId.systemDefault())
    val text = if (alarm.toLocalDate() == now.toLocalDate()) alarm.format(timeFormat) else alarm.format(alarmDayFormat)
    return "ALARMA  ${text.uppercase(spanish)}"
}

package com.adrian.minimallauncher.ui.widgets

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MealRecord
import com.adrian.minimallauncher.data.ReminderRecord
import com.adrian.minimallauncher.ui.components.DotMatrixText
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayInitials = listOf("L", "M", "X", "J", "V", "S", "D")

@Composable
private fun WidgetLayout(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize(), Arrangement.SpaceBetween) {
        Label(title)
        content()
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Detail(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun DayProgressWidget(now: LocalDateTime, size: WidgetSize) {
    val minutes = now.hour * 60 + now.minute
    val percent = minutes * 100 / (24 * 60)
    val left = 24 * 60 - minutes
    val filled = MaterialTheme.colorScheme.onSurface
    val empty = MaterialTheme.colorScheme.outline
    WidgetLayout("DÍA") {
        if (size != WidgetSize.SMALL) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(6f / 4f)) {
                val step = this.size.width / 6
                val radius = step * 0.28f
                repeat(24) { hour ->
                    val center = Offset((hour % 6 + 0.5f) * step, (hour / 6 + 0.5f) * step)
                    if (hour < now.hour) drawCircle(filled, radius, center)
                    else drawCircle(empty, radius, center, style = Stroke(1.dp.toPx()))
                }
            }
        }
        Column {
            DotMatrixText("$percent%", cell = if (size == WidgetSize.LARGE) 4.5.dp else 3.5.dp)
            Spacer(Modifier.height(4.dp))
            Label("${now.hour} DE 24 H")
            if (size == WidgetSize.LARGE) Label("QUEDAN %d:%02d".format(left / 60, left % 60))
        }
    }
}

@Composable
fun ExpensesWidget(size: WidgetSize, finances: List<FinanceRecord>) {
    val today = LocalDate.now()
    val todayRecords = finances.filter { it.createdAt.toLocalDate() == today }
    val totalCents = todayRecords.sumOf { it.amountCents }
    val dollars = totalCents / 100

    // Last 7 days expenditure for the bar chart
    val weekAmounts = (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        finances.filter { it.createdAt.toLocalDate() == date }.sumOf { it.amountCents } / 100f
    }
    val maxExpense = weekAmounts.maxOrNull()?.takeIf { it > 0f } ?: 1f

    WidgetLayout("GASTOS HOY") {
        DotMatrixText("$$dollars", cell = 4.dp)
        if (size == WidgetSize.LARGE) {
            Row(Modifier.fillMaxWidth().height(50.dp), Arrangement.SpaceBetween, Alignment.Bottom) {
                weekAmounts.forEachIndexed { index, amount ->
                    val color = if (index == weekAmounts.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    val fraction = (amount / maxExpense).coerceIn(0.08f, 1f)
                    Box(Modifier.width(8.dp).fillMaxHeight(fraction).background(color))
                }
            }
        }
        if (size != WidgetSize.SMALL) {
            if (todayRecords.isNotEmpty()) {
                Column {
                    todayRecords.take(2).forEach { r ->
                        Detail("${r.description} $${r.amountCents / 100}")
                    }
                }
            } else {
                Detail("Sin gastos hoy")
            }
        }
    }
}

@Composable
fun DietMealsWidget(now: LocalDateTime, size: WidgetSize, meals: List<MealRecord>) {
    val today = now.toLocalDate()
    val todayMeals = meals.filter { it.createdAt.toLocalDate() == today }
    val count = todayMeals.size
    // Target 4 meals: 13:00, 16:30, 18:30, 21:30
    val targetSlots = listOf("13:00", "16:30", "18:30", "21:30")

    WidgetLayout("DIETA") {
        if (size != WidgetSize.SMALL) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                targetSlots.forEachIndexed { index, timeLabel ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val dot = Modifier.size(12.dp)
                        if (index < count) {
                            Box(dot.background(MaterialTheme.colorScheme.onSurface, CircleShape))
                        } else {
                            Box(dot.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                        }
                        if (size == WidgetSize.LARGE) {
                            Spacer(Modifier.height(4.dp))
                            Label(timeLabel)
                        }
                    }
                }
            }
        }
        Column {
            DotMatrixText("$count DE 4", cell = if (size == WidgetSize.LARGE) 4.5.dp else 3.5.dp)
            Spacer(Modifier.height(4.dp))
            if (todayMeals.isNotEmpty()) {
                Detail("ÚLTIMA: ${todayMeals.first().description}")
            } else {
                Label("AYUNO MATINAL")
            }
        }
    }
}

@Composable
fun NextReminderWidget(now: LocalDateTime, size: WidgetSize, reminders: List<ReminderRecord>) {
    val currentTime = now.toLocalTime()
    val upcoming = reminders.filter { LocalTime.of(it.hour, it.minute) >= currentTime }
    val next = upcoming.firstOrNull() ?: reminders.firstOrNull()

    WidgetLayout("RECORDATORIO") {
        if (next != null) {
            val nextTime = LocalTime.of(next.hour, next.minute)
            Column {
                DotMatrixText("%02d:%02d".format(next.hour, next.minute), cell = 4.dp)
                if (size != WidgetSize.SMALL) {
                    Spacer(Modifier.height(8.dp))
                    Text(next.title, style = MaterialTheme.typography.bodySmall)
                }
            }
            Label(countdown(now, nextTime))
        } else {
            Column {
                DotMatrixText("0", cell = 4.dp)
                Spacer(Modifier.height(6.dp))
                Detail("TODO AL DÍA")
            }
            Label("SIN PENDIENTES")
        }
    }
}

private data class WorldCity(val code: String, val zoneId: String)
private val worldCities = listOf(
    WorldCity("CDMX", "America/Mexico_City"),
    WorldCity("TYO", "Asia/Tokyo"),
    WorldCity("MAD", "Europe/Madrid"),
    WorldCity("NYC", "America/New_York"),
)

@Composable
fun WorldClockWidget(size: WidgetSize) {
    var cityIndex by remember { mutableIntStateOf(1) } // Default to TYO
    val city = worldCities[cityIndex % worldCities.size]
    val zone = runCatching { ZoneId.of(city.zoneId) }.getOrElse { ZoneId.systemDefault() }
    val cityTime = ZonedDateTime.now(zone)
    val localTime = ZonedDateTime.now()
    val diffHours = (cityTime.offset.totalSeconds - localTime.offset.totalSeconds) / 3600

    WidgetLayout("RELOJ MUNDIAL", Modifier.clickable { cityIndex++ }) {
        Column {
            val offsetStr = if (diffHours >= 0) "+$diffHours H" else "$diffHours H"
            Label("${city.code}  ($offsetStr)")
            Spacer(Modifier.height(4.dp))
            DotMatrixText(cityTime.format(DateTimeFormatter.ofPattern("HH:mm")), cell = 4.dp)
        }
        if (size == WidgetSize.LARGE) {
            val nextCity = worldCities[(cityIndex + 1) % worldCities.size]
            val nextTime = ZonedDateTime.now(ZoneId.of(nextCity.zoneId))
            Detail("${nextCity.code}  ${nextTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
        }
        Label("TOCA PARA CAMBIAR")
    }
}

@Composable
fun AlarmWidget(size: WidgetSize) {
    val context = LocalContext.current
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val nextTrigger = alarmManager?.nextAlarmClock?.triggerTime
    val nextAlarm = nextTrigger?.let {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
    }

    WidgetLayout("ALARMA", Modifier.clickable {
        runCatching {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        }
    }) {
        if (nextAlarm != null) {
            val now = LocalDateTime.now()
            val rem = Duration.between(now, nextAlarm)
            val remText = if (!rem.isNegative) "EN ${rem.toHours()}H ${rem.toMinutesPart()}M" else "HOY"
            Column {
                DotMatrixText(nextAlarm.format(DateTimeFormatter.ofPattern("HH:mm")), cell = 4.dp)
                Spacer(Modifier.height(6.dp))
                Detail(remText)
            }
            Label("TOCA PARA ABRIR")
        } else {
            Column {
                DotMatrixText("--:--", cell = 4.dp)
                Spacer(Modifier.height(6.dp))
                Detail("Sin alarma")
            }
            Label("TOCA PARA CREAR")
        }
    }
}

private fun countdown(now: LocalDateTime, time: LocalTime): String {
    val remaining = Duration.between(now.toLocalTime(), time)
    return when {
        remaining.isNegative -> "PASADO"
        remaining.toHours() > 0 -> "EN ${remaining.toHours()} H ${remaining.toMinutesPart()} MIN"
        else -> "EN ${remaining.toMinutes()} MIN"
    }
}

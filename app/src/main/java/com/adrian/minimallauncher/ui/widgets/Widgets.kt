package com.adrian.minimallauncher.ui.widgets
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.ui.components.DotMatrixText
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
// Sample data until calendar access and notes exist (Phase 6).
private data class SampleEvent(val time: LocalTime, val title: String)
private val sampleEvents = listOf(
    SampleEvent(LocalTime.of(15, 0), "Reunión equipo"),
    SampleEvent(LocalTime.of(18, 30), "Gimnasio"),
    SampleEvent(LocalTime.of(21, 0), "Leer"),
)
private val sampleBreakfastWeek = listOf(true, true, false, true, true, true) // Days before today.
private val dayInitials = listOf("L", "M", "X", "J", "V", "S", "D") // Monday first, as DayOfWeek.
private val sampleExpenses = listOf("Comida" to 180, "Uber" to 140)
private val sampleWeekExpenses = listOf(250, 90, 410, 120, 300, 60, 320)
@Composable
private fun WidgetLayout(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize(), Arrangement.SpaceBetween) {
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
private fun countdown(now: LocalDateTime, time: LocalTime): String {
    val remaining = Duration.between(now.toLocalTime(), time)
    return when {
        remaining.isNegative -> "TERMINADO"
        remaining.toHours() > 0 -> "EN ${remaining.toHours()} H ${remaining.toMinutesPart()} MIN"
        else -> "EN ${remaining.toMinutes()} MIN"
    }
}
private fun LocalTime.hhmm() = "%02d:%02d".format(hour, minute)
@Composable
fun NextEventWidget(now: LocalDateTime, size: WidgetSize) {
    val upcoming = sampleEvents.filter { it.time > now.toLocalTime() }
    val next = upcoming.firstOrNull() ?: sampleEvents.last()
    WidgetLayout("PRÓXIMO") {
        Column {
            DotMatrixText(next.time.hhmm(), cell = 4.dp)
            if (size != WidgetSize.SMALL) {
                Spacer(Modifier.height(8.dp))
                Text(next.title, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (size == WidgetSize.LARGE) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                upcoming.drop(1).forEach { event -> Detail("${event.time.hhmm()}  ${event.title}") }
            }
        }
        Label(countdown(now, next.time))
    }
}
@Composable
fun BreakfastStreakWidget(now: LocalDateTime, size: WidgetSize) {
    val streak = sampleBreakfastWeek.reversed().takeWhile { it }.size
    val weekDays = (6 downTo 0).map { dayInitials[now.minusDays(it.toLong()).dayOfWeek.value - 1] }
    WidgetLayout("DESAYUNO") {
        if (size != WidgetSize.SMALL) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                (sampleBreakfastWeek + null).forEachIndexed { index, done ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val dot = Modifier.size(12.dp)
                        when (done) {
                            true -> Box(dot.background(MaterialTheme.colorScheme.onSurface, CircleShape))
                            false -> Box(dot.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                            null -> Box(dot.border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)) // Today, pending.
                        }
                        if (size == WidgetSize.LARGE) {
                            Spacer(Modifier.height(6.dp))
                            Label(weekDays[index])
                        }
                    }
                }
            }
        }
        Column {
            DotMatrixText("$streak", cell = if (size == WidgetSize.LARGE) 6.dp else 4.dp)
            Spacer(Modifier.height(4.dp))
            Label("DÍAS SEGUIDOS")
        }
    }
}
@Composable
fun ExpensesWidget(size: WidgetSize) {
    WidgetLayout("GASTOS HOY") {
        DotMatrixText("$${sampleExpenses.sumOf { it.second }}", cell = 4.dp)
        if (size == WidgetSize.LARGE) {
            val max = sampleWeekExpenses.max().toFloat()
            Row(Modifier.fillMaxWidth().height(60.dp), Arrangement.SpaceBetween, Alignment.Bottom) {
                sampleWeekExpenses.forEachIndexed { index, amount ->
                    val color = if (index == sampleWeekExpenses.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    Box(Modifier.width(8.dp).fillMaxHeight(amount / max).background(color))
                }
            }
        }
        if (size != WidgetSize.SMALL) {
            Column { sampleExpenses.forEach { (name, amount) -> Detail("$name $$amount") } }
        }
    }
}

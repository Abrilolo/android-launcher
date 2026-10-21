package com.adrian.minimallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleCard
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StatusDot
import com.adrian.minimallauncher.feature.exercise.ExerciseDay
import com.adrian.minimallauncher.feature.exercise.SessionKind
import com.adrian.minimallauncher.feature.exercise.TokyoGuide
import com.adrian.minimallauncher.feature.finance.formatPesos
import com.adrian.minimallauncher.ui.components.DotMatrixText
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")

/**
 * The page left of home: how the week (Monday to Sunday) is going in each module,
 * read-only. Tapping a card opens that module.
 */
@Composable
fun WeekSummaryScreen(
    now: LocalDateTime,
    records: PersonalRecords,
    exercise: ExerciseDay,
    settings: ModuleSettings,
    onOpenModule: (String) -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = now.toLocalDate()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val week = (0L..6L).map { monday.plusDays(it) }
    val elapsed = week.filter { !it.isAfter(today) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallLabel("SEMANA")
                Text(rangeLabel(monday, week.last()), style = MaterialTheme.typography.headlineSmall)
            }
            SmallLabel("INICIO ›", Modifier.clickable(role = Role.Button, onClick = onNavigateHome).padding(12.dp), color = MaterialTheme.colorScheme.onSurface)
        }

        ExerciseCard(week, today, exercise) { onOpenModule("exercise") }
        FinanceCard(week, elapsed, today, records, settings.dailyBudgetCents) { onOpenModule("finance") }
        DietCard(week, today, elapsed, records, settings.waterGoal) { onOpenModule("diet") }
        BodyCard(monday, records) { onOpenModule("exercise") }
        NotesCard(monday, records) { onOpenModule("notes") }
    }
}

private fun rangeLabel(from: LocalDate, to: LocalDate): String {
    val month = DateTimeFormatter.ofPattern("MMM", spanish)
    val end = "${to.dayOfMonth} ${to.format(month)}".uppercase(spanish).replace(".", "")
    return if (from.month == to.month) "${from.dayOfMonth} – $end" else "${from.dayOfMonth} ${from.format(month).uppercase(spanish).replace(".", "")} – $end"
}

/** Weekday initials over a row of per-day content, so every card lines up the same way. */
@Composable
private fun DayColumns(week: List<LocalDate>, today: LocalDate, cell: @Composable (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        week.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cell(day)
                SmallLabel(
                    day.dayOfWeek.getDisplayName(TextStyle.NARROW, spanish).uppercase(spanish),
                    color = if (day == today) LauncherRed else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    ModuleCard(Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth()) {
            SmallLabel(title, Modifier.weight(1f))
            SmallLabel("›")
        }
        content()
    }
}

@Composable
private fun ExerciseCard(week: List<LocalDate>, today: LocalDate, exercise: ExerciseDay, onClick: () -> Unit) {
    val training = week.filter { TokyoGuide.sessionFor(it).kind != SessionKind.REST }
    val done = training.count { it in exercise.weekDone }
    SummaryCard("EJERCICIO · SESIONES", onClick) {
        DotMatrixText("$done/${training.size}", cell = 5.dp)
        DayColumns(week, today) { day ->
            if (TokyoGuide.sessionFor(day).kind == SessionKind.REST) {
                // Same height as the dots so the weekday letters stay on one line.
                Box(Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 10.dp, height = 2.dp).background(Color(0xFF333333)))
                }
            } else {
                StatusDot(filled = day in exercise.weekDone, next = day == today && day !in exercise.weekDone, size = 12.dp)
            }
        }
    }
}

@Composable
private fun FinanceCard(week: List<LocalDate>, elapsed: List<LocalDate>, today: LocalDate, records: PersonalRecords, budgetCents: Long, onClick: () -> Unit) {
    val byDay = week.associateWith { day -> records.finances.filter { it.createdAt.toLocalDate() == day }.sumOf { it.amountCents } }
    val total = byDay.values.sum()
    val max = byDay.values.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val overDays = if (budgetCents > 0) elapsed.count { (byDay[it] ?: 0) > budgetCents } else 0
    SummaryCard("FINANZAS · GASTO", onClick) {
        DotMatrixText(formatPesos(total), cell = 5.dp)
        SmallLabel(
            buildString {
                append("PROMEDIO ${formatPesos(total / elapsed.size.coerceAtLeast(1))} AL DÍA")
                if (budgetCents > 0) append(" · $overDays ${if (overDays == 1) "DÍA" else "DÍAS"} SOBRE PRESUPUESTO")
            },
            color = if (overDays > 0) LauncherRed else MaterialTheme.colorScheme.onSurface,
        )
        DayColumns(week, today) { day ->
            val cents = byDay[day] ?: 0
            Box(Modifier.height(28.dp), contentAlignment = Alignment.BottomCenter) {
                val color = when {
                    budgetCents > 0 && cents > budgetCents -> LauncherRed
                    day == today -> MaterialTheme.colorScheme.onSurface
                    else -> Color(0xFF5A5A5A)
                }
                val share = if (cents == 0L) 0.12f else (cents.toFloat() / max).coerceIn(0.2f, 1f)
                Box(Modifier.width(8.dp).height(28.dp * share).background(if (cents == 0L) Color(0xFF222222) else color, RoundedCornerShape(4.dp)))
            }
        }
    }
}

@Composable
private fun DietCard(week: List<LocalDate>, today: LocalDate, elapsed: List<LocalDate>, records: PersonalRecords, waterGoal: Int, onClick: () -> Unit) {
    val habits = records.completedHabits
    val meals = elapsed.sumOf { day -> MEAL_NAMES.indices.count { mealKey(day, it) in habits } }
    val water = elapsed.sumOf { day -> (1..waterGoal).count { waterKey(day, it) in habits } }
    val dailyHabits = elapsed.sumOf { day -> DIET_HABITS.count { (id, _) -> habitKey(day, id) in habits } }
    SummaryCard("DIETA · LO QUE VA DE LA SEMANA", onClick) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Stat("COMIDAS", "$meals/${elapsed.size * MEAL_NAMES.size}")
            Stat("AGUA", "$water/${elapsed.size * waterGoal}")
            Stat("HÁBITOS", "$dailyHabits/${elapsed.size * DIET_HABITS.size}")
        }
        // One column of four dots per day: a filled dot per meal checked off.
        DayColumns(week, today) { day ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MEAL_NAMES.indices.forEach { index ->
                    val done = mealKey(day, index) in habits
                    Box(Modifier.size(8.dp).background(if (done) MaterialTheme.colorScheme.onSurface else Color(0xFF222222), CircleShape))
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SmallLabel(label)
        DotMatrixText(value, cell = 2.5.dp)
    }
}

@Composable
private fun BodyCard(monday: LocalDate, records: PersonalRecords, onClick: () -> Unit) {
    val weights = records.bodyMetrics.filter { it.type == "WEIGHT" }
    val thisWeek = weights.filter { !it.createdAt.toLocalDate().isBefore(monday) }.map { it.value }
    val lastWeek = weights.filter { it.createdAt.toLocalDate().let { d -> !d.isBefore(monday.minusWeeks(1)) && d.isBefore(monday) } }.map { it.value }
    val waist = records.bodyMetrics.firstOrNull { it.type == "WAIST" }
    SummaryCard("CUERPO", onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallLabel("PESO PROMEDIO · ${thisWeek.size}/3 MAÑANAS")
                if (thisWeek.isEmpty()) NoData() else DotMatrixText("%.1f".format(thisWeek.average()), cell = 4.dp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallLabel("CINTURA")
                if (waist == null) NoData() else DotMatrixText("%.0f".format(waist.value), cell = 4.dp)
            }
        }
        // Trends over two weeks, not single days, as the guide says.
        val trend = if (thisWeek.isNotEmpty() && lastWeek.isNotEmpty()) {
            val diff = thisWeek.average() - lastWeek.average()
            "%s%.1f KG VS SEMANA ANTERIOR".format(if (diff > 0) "+" else "", diff)
        } else {
            "REGISTRA EL PESO EN EJERCICIO · SEMANA"
        }
        SmallLabel(trend, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Dashes in dot-matrix read as stray dots, so an empty value is plain text. */
@Composable
private fun NoData() {
    Text("Sin datos", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun NotesCard(monday: LocalDate, records: PersonalRecords, onClick: () -> Unit) {
    val thisWeek = records.notes.filter { !it.createdAt.toLocalDate().isBefore(monday) }
    SummaryCard("NOTAS · ${thisWeek.size} ESTA SEMANA", onClick) {
        Text(
            records.notes.firstOrNull()?.text ?: "Sin notas",
            style = MaterialTheme.typography.bodyLarge,
            color = if (records.notes.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

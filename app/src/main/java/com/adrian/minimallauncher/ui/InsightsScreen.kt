package com.adrian.minimallauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.feature.exercise.ExerciseDay
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.ui.components.DotMatrixText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val spanish = Locale.forLanguageTag("es")

@Composable
fun InsightsScreen(
    records: PersonalRecords,
    actions: PersonalRecordActions,
    now: LocalDateTime,
    onNavigateHome: () -> Unit,
    exerciseDay: ExerciseDay,
    onOpenExercise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val today = now.toLocalDate()
    val todayStr = today.toString()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("PULSO & INSIGHTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", spanish)).uppercase(spanish), style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onNavigateHome) {
                Text("HOME →", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Section 1: EJERCICIO
        InsightCard(title = "EJERCICIO") {
            ExerciseSummary(exerciseDay, onOpenExercise)
        }

        Spacer(Modifier.height(20.dp))

        // Section 2: DIETA
        InsightCard(title = "DIETA") {
            DietSection(records, actions, todayStr)
        }

        Spacer(Modifier.height(20.dp))

        // Section 3: FINANZAS
        InsightCard(title = "FINANZAS") {
            FinanceSection(records, actions, today)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun InsightCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** The full plan, set logging and body tracking live in the exercise module; this is its entry point. */
@Composable
private fun ExerciseSummary(day: ExerciseDay, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(day.session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val progress = if (day.session.totalSets > 0) "${day.doneSets} / ${day.session.totalSets} SERIES" else day.session.duration
            Text(if (day.completed) "HECHA · $progress" else progress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("ABRIR →", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DietSection(
    records: PersonalRecords,
    actions: PersonalRecordActions,
    todayStr: String,
) {
    val meals = listOf(
        "13:00" to "Comida completa: Pollo, carne o pescado + arroz + verduras",
        "16:30" to "Proteína + energía: Yogur alto en proteína + granola + fruta",
        "18:30" to "Colación preentreno: Onigiri + leche, o plátano + yogur",
        "21:30" to "Cena post-entreno: Arroz + pollo, carne o pescado + verduras",
    )

    meals.forEachIndexed { index, (time, desc) ->
        val key = "$todayStr|meal_$index"
        val done = records.completedHabits.contains(key)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { actions.toggleHabit(key) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .background(if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Text("HÁBITOS DEL DÍA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    val habits = listOf(
        "walk" to "Caminar 10-15 min después de comer",
        "no_extras" to "Sin refrescos, azúcar ni frituras frecuentes",
        "protein" to "Meta de proteína (~1.6 g/kg)",
        "sleep" to "Sueño 7-9 h (acostarse 23:15-23:30)",
    )

    habits.forEach { (id, label) ->
        val key = "$todayStr|habit_$id"
        val done = records.completedHabits.contains(key)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { actions.toggleHabit(key) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (done) "[✓] " else "[ ] ", style = MaterialTheme.typography.bodyMedium, color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FinanceSection(
    records: PersonalRecords,
    actions: PersonalRecordActions,
    today: LocalDate,
) {
    val todayRecords = records.finances.filter { it.createdAt.toLocalDate() == today }
    val totalCents = todayRecords.sumOf { it.amountCents }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("TOTAL HOY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DotMatrixText("$${totalCents / 100}", cell = 4.dp)
        }
        Text("${todayRecords.size} REGISTROS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    // Weekly bar chart
    Spacer(Modifier.height(12.dp))
    val weekDays = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val weekAmounts = weekDays.map { d -> records.finances.filter { it.createdAt.toLocalDate() == d }.sumOf { it.amountCents } / 100f }
    val maxAmt = weekAmounts.maxOrNull()?.takeIf { it > 0f } ?: 1f

    Row(Modifier.fillMaxWidth().height(45.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        weekAmounts.forEachIndexed { i, amt ->
            val color = if (i == weekAmounts.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            val frac = (amt / maxAmt).coerceIn(0.1f, 1f)
            Box(Modifier.width(16.dp).fillMaxHeight(frac).background(color))
        }
    }

    Spacer(Modifier.height(12.dp))
    var amountInput by rememberSaveable { mutableStateOf("") }
    var descInput by rememberSaveable { mutableStateOf("") }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(amountInput, { amountInput = it }, Modifier.weight(1f), label = { Text("Monto $") }, singleLine = true)
        OutlinedTextField(descInput, { descInput = it }, Modifier.weight(1.5f), label = { Text("Concepto") }, singleLine = true)
        TextButton(onClick = {
            val cents = amountInput.replace(',', '.').toBigDecimalOrNull()?.movePointRight(2)?.longValueExactOrNull()
            if (cents != null && cents > 0 && descInput.isNotBlank()) {
                actions.addFinance(cents, descInput.trim())
                amountInput = ""
                descInput = ""
            }
        }, Modifier.align(Alignment.CenterVertically)) { Text("+") }
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
private fun java.math.BigDecimal.longValueExactOrNull(): Long? = runCatching { longValueExact() }.getOrNull()

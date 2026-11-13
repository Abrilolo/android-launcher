package com.adrian.minimallauncher.feature.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.feature.shortDay
import com.adrian.minimallauncher.feature.finance.ActionPill
import com.adrian.minimallauncher.feature.finance.FinanceMuted
import com.adrian.minimallauncher.feature.finance.FinanceWhite
import com.adrian.minimallauncher.feature.finance.InfoIcon
import com.adrian.minimallauncher.feature.finance.LabelCream
import com.adrian.minimallauncher.feature.finance.MoneyLow
import com.adrian.minimallauncher.feature.finance.MoneyPlenty
import com.adrian.minimallauncher.feature.finance.PlanState
import com.adrian.minimallauncher.feature.finance.SectionLabel
import com.adrian.minimallauncher.feature.finance.TextBlue
import com.adrian.minimallauncher.feature.finance.tinted
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal val hhmm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val DayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val DayShort = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
private val DayInitials = listOf("L", "M", "X", "J", "V", "S", "D")

/** Gray for rest days and neutral frames. */
internal val RestGray = Color.White.copy(alpha = 0.22f)

/** "en 2 h 10 min", "en 25 min", "ahora" or "hace 1 h": from [now] to [at]. */
internal fun untilText(now: LocalTime, at: LocalTime): String {
    val minutes = (at.toSecondOfDay() - now.toSecondOfDay()) / 60
    val abs = kotlin.math.abs(minutes)
    val span = if (abs >= 60) "${abs / 60} h" + (if (abs % 60 > 0) " ${abs % 60} min" else "") else "$abs min"
    return when {
        abs < 2 -> "ahora"
        minutes > 0 -> "en $span"
        else -> "hace $span"
    }
}

/** "3 × 10 · 8 kg", "2 × 30 s" or "25 min": what one movement asks. */
internal fun PlanExercise.ask(kg: Float?): String {
    val cardio = group() == "cardio"
    val target = if (cardio) "${(minTarget / 60).coerceAtLeast(1)} min" else if (timed) "$sets × $minTarget s" else "$sets × $minTarget"
    return target + if (!timed && kg != null && kg > 0f) " · ${kgText(kg)} kg" else ""
}

internal fun kgText(kg: Float) = if (kg % 1f == 0f) kg.toInt().toString() else "%.1f".format(kg)

internal fun Session.isRest() = kind == SessionKind.REST

/** "7 ejercicios · 45 min". */
internal fun Session.meta(): String = listOfNotNull(
    exercises.size.takeIf { it > 0 }?.let { if (it == 1) "1 ejercicio" else "$it ejercicios" },
    minutes()?.let { "$it min" },
).joinToString(" · ").ifEmpty { note.take(40) }

/** How a day stands: done, today, missed (a past training day) or still to come. */
internal fun dayState(done: Boolean, date: LocalDate, today: LocalDate, rest: Boolean): PlanState = when {
    done -> PlanState.DONE
    date == today -> PlanState.TODAY
    date.isBefore(today) && !rest -> PlanState.LATE
    else -> PlanState.PLANNED
}

internal fun stateTint(state: PlanState, rest: Boolean = false): Color = when {
    state == PlanState.DONE -> MoneyPlenty
    state == PlanState.TODAY -> LauncherAccent
    state == PlanState.LATE -> LauncherDanger
    rest -> RestGray
    else -> MoneyLow
}

internal fun stateName(state: PlanState, rest: Boolean): String = when {
    state == PlanState.DONE -> "Hecho"
    state == PlanState.LATE -> "No se hizo"
    state == PlanState.TODAY -> if (rest) "Hoy descansas" else "Hoy toca"
    rest -> "Descanso"
    else -> "Planeado"
}

/** The mark of a day: filled green done, blue ring today, pink ring missed, amber dashed to come, a gray dash resting. */
@Composable
internal fun DayMark(state: PlanState, rest: Boolean, size: Dp = 12.dp) {
    Canvas(Modifier.size(size)) {
        val s = 1.8.dp.toPx()
        val r = this.size.minDimension / 2
        when {
            state == PlanState.DONE -> drawCircle(MoneyPlenty, r)
            state == PlanState.TODAY -> drawCircle(LauncherAccent, r - s / 2, style = Stroke(s * 1.2f))
            state == PlanState.LATE -> drawCircle(LauncherDanger, r - s / 2, style = Stroke(s))
            rest -> drawLine(RestGray.copy(alpha = 0.6f), Offset(0f, center.y), Offset(this.size.width, center.y), s, androidx.compose.ui.graphics.StrokeCap.Round)
            else -> drawCircle(MoneyLow, r - s / 2, style = Stroke(s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5.dp.toPx(), 2.dp.toPx()))))
        }
    }
}

/** A short text action in the action blue, like a link. */
@Composable
internal fun TextLink(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text, modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp, horizontal = 6.dp),
        style = MaterialTheme.typography.labelLarge, color = TextBlue,
    )
}

/** The consistency of the last weeks: how many trainings each had against how many its routine asks. */
internal class Consistency(val streak: Int, val bars: List<Pair<Int, Int>>, val target: Int)

internal fun consistencyOf(day: ExerciseDay): Consistency {
    val monday = day.date.with(DayOfWeek.MONDAY)
    fun week(start: LocalDate): Pair<Int, Int> {
        val days = (0L..6L).map { start.plusDays(it) }
        val target = days.count { !RoutinePlans.sessionFor(it, day.plans).isRest() }
        return days.count { it in day.doneDates } to target
    }
    val bars = (9 downTo 0).map { week(monday.minusWeeks(it.toLong())) }
    var streak = 0
    for (w in 1..12) {
        val (done, target) = week(monday.minusWeeks(w.toLong()))
        if (target > 0 && done >= target) streak++ else break
    }
    val (doneNow, targetNow) = bars.last()
    if (targetNow > 0 && doneNow >= targetNow) streak++
    return Consistency(streak, bars, targetNow)
}

/**
 * Hoy, each thing once, in reading order:
 * 1. The day's card: the routine, its hour and what it works, Iniciar, and the way to see and edit it. On a rest
 *    day: "Hoy descansas" and the next training.
 * 2. Esta semana: seven days to tap; the tapped one says what it was and opens it.
 * 3. Constancia: weeks in a row meeting the routine, the last ten weeks and this week's trainings.
 */
@Composable
internal fun ExerciseHoy(
    day: ExerciseDay,
    settings: ModuleSettings,
    onStart: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val session = day.session
    val now by produceState(LocalTime.now()) { while (true) { delay(30_000); value = LocalTime.now() } }
    val started = day.sets.isNotEmpty()
    val late = !session.isRest() && !started && !day.completed && now.isAfter(settings.exerciseStart.plusMinutes(30))

    // 1. Hoy
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Hoy · ${DayNames[day.date.dayOfWeek.value - 1].lowercase()} ${shortDay(day.date)}")
        if (session.isRest()) RestCard(day, settings) { onOpenDay(it) }
        else Column(
            Modifier.fillMaxWidth().tinted(if (day.completed) MoneyPlenty else LauncherAccent, radius = 20.dp, width = 1.5.dp).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(session.title, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 28.sp), color = FinanceWhite, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(settings.exerciseStart.format(hhmm), style = MaterialTheme.typography.titleLarge, color = if (late) LauncherDanger else TextBlue)
                        Text(
                            when {
                                day.completed -> "Hecho"
                                started -> "En curso"
                                else -> untilText(now, settings.exerciseStart)
                            },
                            Modifier.padding(bottom = 2.dp), style = MaterialTheme.typography.bodyMedium, color = if (day.completed) MoneyPlenty else FinanceWhite,
                        )
                    }
                    Text(session.meta(), style = MaterialTheme.typography.bodySmall, color = LabelCream)
                }
                MuscleMap(heatOf(session.exercises.map { it.muscles() }), height = 92.dp, labels = false)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPill(
                    when {
                        day.completed -> "Ver sesión"
                        started -> "Seguir"
                        else -> "Iniciar"
                    },
                    if (day.completed) "listo" else "gimnasio",
                    Modifier.weight(1f),
                    onClick = onStart,
                )
                ActionPill("Rutina", "lapiz", primary = false) { onOpenDay(day.date) }
            }
        }
    }

    // 2. Esta semana
    WeekStrip(day, onOpenDay)

    // 3. Constancia
    ConsistencyCard(day)
}

/** A rest day: said once, with the next training under it. */
@Composable
private fun RestCard(day: ExerciseDay, settings: ModuleSettings, onOpenDay: (LocalDate) -> Unit) {
    val next = (1L..14L).map { day.date.plusDays(it) }.firstOrNull { !RoutinePlans.sessionFor(it, day.plans).isRest() }
    Column(Modifier.fillMaxWidth().tinted(RestGray, radius = 20.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StrokeIcon("dormir", StrokeState.DONE, 28.dp, color = LabelCream)
            Text("Hoy descansas", style = MaterialTheme.typography.headlineSmall, color = FinanceWhite)
        }
        if (day.session.note.isNotBlank()) Text(day.session.note, style = MaterialTheme.typography.bodyMedium, color = FinanceMuted)
        next?.let { date ->
            Row(
                Modifier.fillMaxWidth().tinted(LauncherAccent, radius = 16.dp, width = 1.5.dp).clickable(role = Role.Button) { onOpenDay(date) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Siguiente · " + if (date == day.date.plusDays(1)) "mañana" else DayNames[date.dayOfWeek.value - 1].lowercase(), style = MaterialTheme.typography.bodySmall, color = LabelCream)
                    Text(RoutinePlans.sessionFor(date, day.plans).title, style = MaterialTheme.typography.bodyLarge, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(settings.exerciseStart.format(hhmm), style = MaterialTheme.typography.titleLarge, color = TextBlue)
            }
        }
    }
}

/** Seven days to tap; the tapped one shows what it was and opens its routine. */
@Composable
private fun WeekStrip(day: ExerciseDay, onOpenDay: (LocalDate) -> Unit) {
    var picked by rememberSaveable { mutableIntStateOf(day.date.dayOfWeek.value - 1) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Esta semana")
            InfoIcon("Toca un día para ver qué tocaba. Verde: hecho. Azul: hoy. Rosa: no se hizo. Ámbar punteado: viene. Raya gris: descanso. Las semanas siguientes están en Plan.")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            day.week.forEachIndexed { i, (date, session) ->
                val rest = session.isRest()
                val state = dayState(date in day.weekDone, date, day.date, rest)
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .then(if (i == picked) Modifier.border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(14.dp)) else Modifier)
                        .clickable(role = Role.Tab) { picked = i }
                        .semantics(mergeDescendants = true) { contentDescription = "${DayNames[i]} ${date.dayOfMonth}: ${stateName(state, rest)}" }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(DayInitials[i], style = MaterialTheme.typography.labelMedium, color = LabelCream)
                    Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleSmall, color = FinanceWhite)
                    DayMark(state, rest, 10.dp)
                }
            }
        }
        day.week.getOrNull(picked)?.let { (date, session) ->
            val rest = session.isRest()
            val state = dayState(date in day.weekDone, date, day.date, rest)
            Row(
                Modifier.fillMaxWidth().tinted(stateTint(state, rest), radius = 16.dp, dashed = state == PlanState.PLANNED && !rest)
                    .clickable(role = Role.Button) { onOpenDay(date) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${DayNames[date.dayOfWeek.value - 1]} ${date.dayOfMonth} · ${stateName(state, rest)}", style = MaterialTheme.typography.bodySmall, color = LabelCream)
                    Text(session.title, style = MaterialTheme.typography.bodyLarge, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                com.adrian.minimallauncher.feature.finance.Chevron('>', TextBlue)
            }
        }
    }
}

/** Constancia: the goal is training as the routine asks, week after week; weight is only a record. */
@Composable
private fun ConsistencyCard(day: ExerciseDay) {
    val c = consistencyOf(day)
    val training = day.week.filter { !it.second.isRest() }
    val pending = training.filter { (d, _) -> d !in day.weekDone && !d.isBefore(day.date) }
    val doneNow = c.bars.last().first
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Constancia")
            InfoIcon("Tu meta es entrenar los días que pide tu rutina, cada semana. Las columnas son tus últimas 10 semanas: verde cumplida, azul a medias, rosa sin entrenar. Tu peso y medidas quedan en Cuerpo solo como registro.")
        }
        Column(Modifier.fillMaxWidth().tinted(MoneyPlenty, radius = 18.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${c.streak}", style = MaterialTheme.typography.headlineMedium, color = if (c.streak > 0) MoneyPlenty else FinanceWhite)
                        Text(if (c.streak == 1) "semana seguida" else "semanas seguidas", Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.bodyLarge, color = FinanceWhite)
                    }
                    Text(if (c.target == 1) "cumpliendo 1 entreno a la semana" else "cumpliendo ${c.target} entrenos a la semana", style = MaterialTheme.typography.bodySmall, color = LabelCream)
                }
                Row(Modifier.height(40.dp).semantics { contentDescription = "Últimas 10 semanas" }, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                    c.bars.forEachIndexed { i, (done, target) ->
                        val current = i == c.bars.lastIndex
                        val fraction = if (target <= 0) 1f else (done.toFloat() / target).coerceIn(0.15f, 1f)
                        val color = when {
                            target > 0 && done >= target -> MoneyPlenty
                            current -> MoneyLow
                            done == 0 -> LauncherDanger
                            else -> TextBlue.copy(alpha = 0.7f)
                        }
                        Box(
                            Modifier.width(6.dp).height(40.dp * fraction)
                                .then(if (current && done < target) Modifier.border(1.dp, color, RoundedCornerShape(3.dp)) else Modifier.background(color, RoundedCornerShape(3.dp))),
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                training.forEach { (date, session) ->
                    DayMark(dayState(date in day.weekDone, date, day.date, session.isRest()), false, 16.dp)
                }
                Text(
                    when {
                        doneNow >= c.target && c.target > 0 -> "Semana cumplida"
                        pending.isEmpty() -> "Ya no quedan días esta semana"
                        else -> {
                            val names = pending.map { (d, _) -> if (d == day.date) "hoy" else DayNames[d.dayOfWeek.value - 1].lowercase() }
                            "Faltan: " + names.joinToString(", ")
                        }
                    },
                    Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodyMedium, color = FinanceWhite, maxLines = 2,
                )
            }
        }
    }
}

/**
 * Plan: this week and the three after it, one at a time. Each day with its routine, framed by its state; a day changed
 * only once says so. A tap opens the day's routine, where it is changed for that day or for every week.
 */
@Composable
internal fun ExercisePlan(day: ExerciseDay, onOpenDay: (LocalDate) -> Unit, onCopyPrevious: (LocalDate) -> Unit) {
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val monday = day.date.with(DayOfWeek.MONDAY).plusWeeks(offset.toLong())
    val days = (0L..6L).map { monday.plusDays(it) }.map { it to RoutinePlans.sessionFor(it, day.plans) }
    val trainings = days.count { !it.second.isRest() }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WeekArrow("‹", "Semana anterior", offset > 0) { offset-- }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                when (offset) {
                    0 -> "Esta semana"
                    1 -> "La próxima"
                    else -> "En $offset semanas"
                },
                style = MaterialTheme.typography.titleMedium, color = FinanceWhite,
            )
            Text("${shortDay(monday)} – ${shortDay(monday.plusDays(6))}", style = MaterialTheme.typography.bodySmall, color = LabelCream)
        }
        WeekArrow("›", "Semana siguiente", offset < 8) { offset++ }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(if (trainings == 1) "1 entreno" else "$trainings entrenos")
            InfoIcon("Toca un día para ver su rutina y cambiarla solo ese día o todas las semanas. \"Solo esta vez\" marca un día cambiado una vez.")
            Box(Modifier.weight(1f))
            if (offset > 0) ActionPill("Copiar semana anterior", null, primary = false) { onCopyPrevious(monday) }
        }
        days.forEach { (date, session) ->
            val rest = session.isRest()
            val state = dayState(date in day.doneDates, date, day.date, rest)
            val once = RoutinePlans.todayId(date) in day.plans
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .tinted(stateTint(state, rest), radius = 16.dp, dashed = state == PlanState.PLANNED && !rest, width = if (state == PlanState.TODAY) 1.5.dp else 1.dp)
                    .clickable(role = Role.Button) { onOpenDay(date) }
                    .semantics(mergeDescendants = true) { contentDescription = "${DayNames[date.dayOfWeek.value - 1]} ${date.dayOfMonth}: ${session.title}" }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(52.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(DayShort[date.dayOfWeek.value - 1], style = MaterialTheme.typography.bodySmall, color = LabelCream)
                    Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = FinanceWhite)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(session.title, style = MaterialTheme.typography.bodyLarge, color = if (rest) FinanceMuted else FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(session.meta(), style = MaterialTheme.typography.bodySmall, color = FinanceMuted, maxLines = 1)
                }
                if (once) Text("Solo esta vez", Modifier.border(1.dp, MoneyLow.copy(alpha = 0.6f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = MoneyLow)
                if (state != PlanState.PLANNED) Text(stateName(state, rest).removePrefix("Hoy toca").ifEmpty { "Hoy" }, style = MaterialTheme.typography.labelMedium, color = if (state == PlanState.TODAY) TextBlue else stateTint(state, rest))
            }
        }
    }
}

@Composable
private fun WeekArrow(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).border(1.dp, Color.White.copy(alpha = if (enabled) 0.2f else 0.08f), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { com.adrian.minimallauncher.feature.finance.Chevron(if (symbol == "‹") '<' else '>', if (enabled) FinanceWhite else FinanceMuted.copy(alpha = 0.4f), 20.dp) }
}

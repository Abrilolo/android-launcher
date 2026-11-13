package com.adrian.minimallauncher.feature.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.feature.finance.FinanceWhite
import com.adrian.minimallauncher.feature.finance.LabelCream
import com.adrian.minimallauncher.feature.finance.MoneyLow
import com.adrian.minimallauncher.feature.finance.MoneyPlenty
import com.adrian.minimallauncher.feature.finance.PlanState
import com.adrian.minimallauncher.feature.finance.TextBlue
import com.adrian.minimallauncher.ui.Tile
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.LocalDate
import java.time.LocalTime

/**
 * Ejercicio widgets (concept: the "Widgets de Ejercicio" canvas, 2026-11-13). Hoy, Constancia and Semana, each in
 * 4×2, 2×2 and 4×1, with a one-word tag. The frame says the state (blue to do, green done, gray resting); the
 * background stays transparent. ▶ starts today's session, a day opens its routine, the rest opens Ejercicio.
 */
object ExerciseWidgets {
    /** Kept as "exercise" so a Hoy widget already on a page picks up the new look. */
    const val TODAY = "exercise"
    const val STREAK = "exercise_streak"
    const val WEEK = "exercise_week"
    val ids = listOf(TODAY, STREAK, WEEK)
    val labels = mapOf(TODAY to "Ejercicio · Hoy", STREAK to "Ejercicio · Constancia", WEEK to "Ejercicio · Semana")
    private val tags = mapOf(TODAY to "HOY", STREAK to "CONSTANCIA", WEEK to "SEMANA")
    fun tag(id: String) = tags[id] ?: "HOY"

    /** Opens Ejercicio straight into today's session. */
    const val START_ROUTE = "exercise:start"
    /** Opens Ejercicio on [date]'s routine. */
    fun dayRoute(date: LocalDate) = "exercise:day:$date"
}

private enum class Shape { SQUARE, WIDE, STRIP }

private val RestFrame = Color.White.copy(alpha = 0.22f)
private val NeutralFrame = Color.White.copy(alpha = 0.16f)
private val WidgetShape = RoundedCornerShape(26.dp) // the stack card's corners, so the colored frame lies on its edge

@Composable
fun ExerciseWidget(id: String, day: ExerciseDay, start: LocalTime, modifier: Modifier, onOpen: (String) -> Unit, onLongClick: () -> Unit) {
    val frame = when (id) {
        ExerciseWidgets.STREAK -> MoneyPlenty.copy(alpha = 0.6f)
        ExerciseWidgets.WEEK -> NeutralFrame
        else -> when {
            day.completed -> MoneyPlenty.copy(alpha = 0.65f)
            day.session.isRest() -> RestFrame
            else -> LauncherAccent
        }
    }
    val strong = id == ExerciseWidgets.TODAY && !day.completed && !day.session.isRest()
    Tile(modifier.border(if (strong) 1.5.dp else 1.dp, frame, WidgetShape), { onOpen("exercise") }, onLongClick) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val shape = when {
                maxHeight < 96.dp -> Shape.STRIP
                maxWidth > maxHeight * 1.6f -> Shape.WIDE
                else -> Shape.SQUARE
            }
            val content: @Composable () -> Unit = {
                when (id) {
                    ExerciseWidgets.STREAK -> StreakWidget(day, shape)
                    ExerciseWidgets.WEEK -> WeekWidget(day, start, shape, onOpen)
                    else -> TodayWidget(day, start, shape, maxHeight, onOpen)
                }
            }
            if (shape == Shape.STRIP) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tag(ExerciseWidgets.tag(id))
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) { content() }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(ExerciseWidgets.tag(id))
                    Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp), color = LabelCream, maxLines = 1,
    )
}

// ---------- Hoy ----------

@Composable
private fun TodayWidget(day: ExerciseDay, start: LocalTime, shape: Shape, height: Dp, onOpen: (String) -> Unit) {
    val session = day.session
    val live = day.sets.isNotEmpty() && !day.completed
    val current = session.exercises.firstOrNull { (day.sets[it.id]?.size ?: 0) < it.sets }
    val now = LocalTime.now()
    when {
        // Rest day: said once, with the next training.
        session.isRest() -> {
            val next = (1L..14L).map { day.date.plusDays(it) }.firstOrNull { !RoutinePlans.sessionFor(it, day.plans).isRest() }
            val when_ = next?.let { if (it == day.date.plusDays(1)) "Mañana" else DayNames[it.dayOfWeek.value - 1] }
            when (shape) {
                Shape.STRIP -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Descansas", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = FinanceWhite)
                    when_?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = LabelCream) }
                    Text(start.format(hhmm), style = MaterialTheme.typography.titleMedium, color = TextBlue)
                }
                else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StrokeIcon("dormir", StrokeState.DONE, 26.dp, color = LabelCream)
                        if (shape == Shape.WIDE) Text("Descansas", style = MaterialTheme.typography.titleLarge, color = FinanceWhite)
                    }
                    next?.let { date ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("$when_ · ${start.format(hhmm)}", style = MaterialTheme.typography.bodySmall, color = LabelCream)
                            Text(
                                RoutinePlans.sessionFor(date, day.plans).title, style = MaterialTheme.typography.bodyMedium, color = FinanceWhite,
                                maxLines = if (shape == Shape.WIDE) 1 else 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        // Done: green, with what it was.
        day.completed -> when (shape) {
            Shape.STRIP -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(session.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Hecho", style = MaterialTheme.typography.titleSmall, color = MoneyPlenty)
            }
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                if (shape == Shape.WIDE) Text(session.title, style = MaterialTheme.typography.titleMedium, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Check(if (shape == Shape.WIDE) 22.dp else 32.dp)
                    if (shape == Shape.WIDE) Text("Hecho", style = MaterialTheme.typography.titleLarge, color = MoneyPlenty)
                }
                val sets = day.sets.values.sumOf { it.size }
                Text(listOfNotNull(sets.takeIf { it > 0 }?.let { "$it series" }, session.minutes()?.let { "$it min" }).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = LabelCream)
            }
        }
        // Under way: the movement now and its series.
        live && current != null -> {
            val done = day.sets[current.id]?.size ?: 0
            when (shape) {
                Shape.STRIP -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(current.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Series(current.sets, done, 10.dp)
                    Play(onOpen, 40.dp)
                }
                else -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        Text(current.name, style = MaterialTheme.typography.titleMedium, color = FinanceWhite, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Series(current.sets, done, 12.dp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Play(onOpen, 44.dp)
                            if (shape == Shape.WIDE) Text("Seguir", style = MaterialTheme.typography.bodySmall, color = TextBlue)
                        }
                    }
                    if (shape == Shape.WIDE) Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                        MuscleMap(current.muscles(), height = (height - 40.dp).coerceIn(48.dp, 96.dp), labels = false)
                    }
                }
            }
        }
        // To do: the routine, its hour and ▶.
        else -> {
            val late = now.isAfter(start.plusMinutes(30))
            val hourColor = if (late) LauncherDanger else TextBlue
            when (shape) {
                Shape.STRIP -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(session.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(start.format(hhmm), style = MaterialTheme.typography.titleMedium, color = hourColor)
                    Play(onOpen, 40.dp)
                }
                Shape.SQUARE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(session.title, style = MaterialTheme.typography.titleSmall, color = FinanceWhite, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(start.format(hhmm), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = hourColor)
                        Play(onOpen, 44.dp)
                    }
                }
                Shape.WIDE -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(session.title, style = MaterialTheme.typography.titleMedium, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(start.format(hhmm), style = MaterialTheme.typography.titleLarge, color = hourColor)
                                Text(untilText(now, start), Modifier.padding(bottom = 2.dp), style = MaterialTheme.typography.bodySmall, color = FinanceWhite)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Play(onOpen, 44.dp)
                            Text(session.meta(), style = MaterialTheme.typography.bodySmall, color = LabelCream, maxLines = 1)
                        }
                    }
                    Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                        MuscleMap(heatOf(session.exercises.map { it.muscles() }), height = (height - 40.dp).coerceIn(48.dp, 96.dp), labels = false)
                    }
                }
            }
        }
    }
}

/** ▶ in the action blue: opens today's session straight away. */
@Composable
private fun Play(onOpen: (String) -> Unit, size: Dp) {
    Box(
        Modifier.size(size).background(LauncherAccent, CircleShape)
            .clickable(role = Role.Button) { onOpen(ExerciseWidgets.START_ROUTE) }
            .semantics { contentDescription = "Iniciar rutina" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.36f)) {
            val w = this.size.width
            val h = this.size.height
            drawPath(Path().apply { moveTo(w * 0.15f, 0f); lineTo(w, h / 2); lineTo(w * 0.15f, h); close() }, Color.White)
        }
    }
}

@Composable
private fun Check(size: Dp) {
    Canvas(Modifier.size(size).semantics { contentDescription = "Hecho" }) {
        val w = this.size.width
        val h = this.size.height
        val s = 2.2.dp.toPx()
        drawLine(MoneyPlenty, Offset(w * 0.18f, h * 0.52f), Offset(w * 0.4f, h * 0.74f), s, androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(MoneyPlenty, Offset(w * 0.4f, h * 0.74f), Offset(w * 0.82f, h * 0.28f), s, androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

/** A movement's series: done green, the next one a blue ring, the rest amber dashed. */
@Composable
private fun Series(total: Int, done: Int, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i -> DayMark(if (i < done) PlanState.DONE else if (i == done) PlanState.TODAY else PlanState.PLANNED, false, size) }
    }
}

// ---------- Constancia ----------

@Composable
private fun StreakWidget(day: ExerciseDay, shape: Shape) {
    val c = consistencyOf(day)
    val training = day.week.filter { !it.second.isRest() }
    val pending = training.filter { (d, _) -> d !in day.weekDone && !d.isBefore(day.date) }
    val color = if (c.streak > 0) MoneyPlenty else FinanceWhite
    when (shape) {
        Shape.STRIP -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${c.streak}", style = MaterialTheme.typography.headlineSmall, color = color)
            Box(Modifier.weight(1f))
            Bars(c, 32.dp, 6.dp)
        }
        Shape.SQUARE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("${c.streak}", style = MaterialTheme.typography.displaySmall, color = color)
                Text(if (c.streak == 1) "semana" else "semanas", style = MaterialTheme.typography.bodySmall, color = FinanceWhite)
            }
            WeekDots(day, training, 14.dp)
        }
        Shape.WIDE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${c.streak}", style = MaterialTheme.typography.displaySmall, color = color)
                    Text(if (c.streak == 1) "semana seguida" else "semanas seguidas", Modifier.padding(bottom = 6.dp), style = MaterialTheme.typography.bodyMedium, color = FinanceWhite)
                }
                Bars(c, 44.dp, 8.dp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeekDots(day, training, 14.dp)
                val doneNow = c.bars.last().first
                Text(
                    when {
                        c.target > 0 && doneNow >= c.target -> "Semana cumplida"
                        pending.isEmpty() -> ""
                        else -> "Faltan " + pending.joinToString(" y ") { (d, _) -> if (d == day.date) "hoy" else DayNames[d.dayOfWeek.value - 1].lowercase() }
                    },
                    style = MaterialTheme.typography.bodySmall, color = LabelCream, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The last ten weeks: green met, blue halfway, pink empty, this week amber while it is open. */
@Composable
private fun Bars(c: Consistency, height: Dp, width: Dp) {
    Row(Modifier.height(height).semantics { contentDescription = "Últimas 10 semanas" }, horizontalArrangement = Arrangement.spacedBy(width * 0.6f), verticalAlignment = Alignment.Bottom) {
        c.bars.forEachIndexed { i, (done, target) ->
            val current = i == c.bars.lastIndex
            val met = target > 0 && done >= target
            val fraction = if (target <= 0) 1f else (done.toFloat() / target).coerceIn(0.2f, 1f)
            val color = when {
                met -> MoneyPlenty
                current -> MoneyLow
                done == 0 -> LauncherDanger
                else -> TextBlue.copy(alpha = 0.7f)
            }
            Box(
                Modifier.width(width).height(height * fraction).then(
                    if (current && !met) Modifier.border(1.dp, color, RoundedCornerShape(width / 2)) else Modifier.background(color, RoundedCornerShape(width / 2)),
                ),
            )
        }
    }
}

@Composable
private fun WeekDots(day: ExerciseDay, training: List<Pair<LocalDate, Session>>, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        training.forEach { (date, _) -> DayMark(dayState(date in day.weekDone, date, day.date, false), false, size) }
    }
}

// ---------- Semana ----------

private val Initials = listOf("L", "M", "X", "J", "V", "S", "D")

@Composable
private fun WeekWidget(day: ExerciseDay, start: LocalTime, shape: Shape, onOpen: (String) -> Unit) {
    fun state(date: LocalDate, session: Session) = dayState(date in day.weekDone, date, day.date, session.isRest())
    when (shape) {
        Shape.STRIP -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            day.week.forEachIndexed { i, (date, session) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(Initials[i], style = MaterialTheme.typography.labelSmall, color = LabelCream)
                    DayMark(state(date, session), session.isRest(), 10.dp)
                }
            }
        }
        Shape.SQUARE -> {
            val next = (0L..14L).map { day.date.plusDays(it) }
                .firstOrNull { d -> !RoutinePlans.sessionFor(d, day.plans).isRest() && !(d == day.date && day.completed) }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                next?.let { d ->
                    Column(Modifier.clickable(role = Role.Button) { onOpen(ExerciseWidgets.dayRoute(d)) }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Siguiente · " + if (d == day.date) "hoy" else DayNames[d.dayOfWeek.value - 1].lowercase(), style = MaterialTheme.typography.bodySmall, color = LabelCream)
                        Text(RoutinePlans.sessionFor(d, day.plans).title, style = MaterialTheme.typography.titleSmall, color = FinanceWhite, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                } ?: Box(Modifier)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    day.week.forEach { (date, session) -> DayMark(state(date, session), session.isRest(), 10.dp) }
                }
            }
        }
        Shape.WIDE -> {
            var picked by remember(day.date) { mutableIntStateOf(day.date.dayOfWeek.value - 1) }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth()) {
                    day.week.forEachIndexed { i, (date, session) ->
                        Column(
                            Modifier.weight(1f)
                                .then(if (i == picked) Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)) else Modifier)
                                .clickable(role = Role.Tab) { if (picked == i) onOpen(ExerciseWidgets.dayRoute(date)) else picked = i }
                                .semantics(mergeDescendants = true) { contentDescription = "${DayNames[i]} ${date.dayOfMonth}: ${session.title}" }
                                .padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(Initials[i], style = MaterialTheme.typography.labelSmall, color = LabelCream)
                            Text("${date.dayOfMonth}", style = MaterialTheme.typography.labelLarge, color = FinanceWhite)
                            DayMark(state(date, session), session.isRest(), 9.dp)
                        }
                    }
                }
                day.week.getOrNull(picked)?.let { (date, session) ->
                    val st = state(date, session)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(DayNames[date.dayOfWeek.value - 1], style = MaterialTheme.typography.bodySmall, color = LabelCream)
                        Text(session.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (st == PlanState.TODAY && !session.isRest()) start.format(hhmm) else stateName(st, session.isRest()),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (st == PlanState.TODAY) TextBlue else stateTint(st, session.isRest()).let { if (it == RestGray) LabelCream else it },
                        )
                    }
                }
            }
        }
    }
}

package com.adrian.minimallauncher.data.routine

import com.adrian.minimallauncher.data.MealSlot
import com.adrian.minimallauncher.data.NoteCategory
import com.adrian.minimallauncher.data.NoteRecord
import com.adrian.minimallauncher.feature.exercise.Session
import com.adrian.minimallauncher.feature.exercise.SessionKind
import com.adrian.minimallauncher.feature.exercise.TokyoGuide
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** What a widget can show, most urgent first in [WidgetSelector.pick]. Design comes later; this is the data. */
sealed interface WidgetPick {
    /** Payments waiting to be registered (the forced flow). */
    data class PendingPayment(val count: Int, val amountCents: Long) : WidgetPick
    /** A meal is due within [WidgetSelector.SOON] and not marked yet. */
    data class MealSoon(val meal: MealSlot, val minutesLeft: Long) : WidgetPick
    /** Training starts within [WidgetSelector.SOON], or is due and not done. */
    data class Training(val session: Session, val at: LocalTime, val minutesLeft: Long) : WidgetPick
    /** The first pending item by hour of the phase: a timed action or a linked reminder (see [PhaseReminders]). */
    data class PhaseStep(val phaseId: String, val next: PhaseReminder) : WidgetPick
    /** No timed item pending: the phase's next checklist step without an hour (casa: ordenar, agua…). */
    data class ChecklistStep(val phaseId: String, val step: ActionState) : WidgetPick
    /** Pending notes per aspect, the phase's own aspect first. The Rutinas 4×2 widget does not use it. */
    data class PendingNotes(val byAspect: Map<NoteCategory, Int>, val focus: NoteCategory?) : WidgetPick
}

/** The records a pick needs; the ViewModel fills it from its flows. */
data class WidgetInputs(
    val openPayments: Int = 0,
    val openPaymentsCents: Long = 0,
    val meals: List<MealSlot> = emptyList(),
    val habits: Set<String> = emptySet(),
    val exerciseStart: LocalTime = LocalTime.of(20, 35),
    val workouts: Set<String> = emptySet(),
    val phaseChecks: List<ActionState> = emptyList(),
    /** The phase's timed actions and linked reminders, sorted by hour. */
    val phaseReminders: List<PhaseReminder> = emptyList(),
    val pendingNotes: Map<NoteCategory, Int> = emptyMap(),
)

object WidgetSelector {
    val SOON: Duration = Duration.ofMinutes(30)

    /** Which notes aspect each phase cares about first. */
    fun focusOf(phaseId: String): NoteCategory? = when (phaseId) {
        RoutineDefaults.TRABAJO -> NoteCategory.WORK
        RoutineDefaults.CASA -> NoteCategory.EXERCISE
        RoutineDefaults.INICIO -> NoteCategory.DIET
        else -> null
    }

    /** Pending (not done) notes per aspect; aspects with none are left out. */
    fun pendingByAspect(notes: List<NoteRecord>): Map<NoteCategory, Int> =
        NoteCategory.ASPECTS.associateWith { a -> notes.count { it.category == a && !it.done } }.filterValues { it > 0 }

    /** Everything worth showing now for [phase], most urgent first; widgets take the first that fits them. */
    fun pick(phase: RoutinePhase, now: LocalDateTime, inputs: WidgetInputs): List<WidgetPick> {
        val picks = mutableListOf<WidgetPick>()
        val date = now.toLocalDate()
        val time = now.toLocalTime()
        if (inputs.openPayments > 0) picks += WidgetPick.PendingPayment(inputs.openPayments, inputs.openPaymentsCents)

        inputs.meals.filter { "$date|meal_${it.id}" !in inputs.habits }
            .map { it to Duration.between(time, it.time).toMinutes() }
            .filter { (_, left) -> left in 0..SOON.toMinutes() }
            .minByOrNull { it.second }
            ?.let { (meal, left) -> picks += WidgetPick.MealSoon(meal, left) }

        val session = TokyoGuide.sessionFor(date)
        if (session.kind != SessionKind.REST && date.toString() !in inputs.workouts) {
            val left = Duration.between(time, inputs.exerciseStart).toMinutes()
            // Shown from half an hour before; once late, only in a phase that has a gimnasio action.
            if (left <= SOON.toMinutes() && (left >= 0 || phase.actions.any { it.type == "gimnasio" })) {
                picks += WidgetPick.Training(session, inputs.exerciseStart, left)
            }
        }

        val next = PhaseReminders.next(inputs.phaseReminders)
        if (next != null) picks += WidgetPick.PhaseStep(phase.id, next)
        else inputs.phaseChecks.firstOrNull { it.on && it.markable && !it.done && it.action.at == null }?.let { picks += WidgetPick.ChecklistStep(phase.id, it) }

        if (inputs.pendingNotes.isNotEmpty()) {
            val focus = focusOf(phase.id)?.takeIf { it in inputs.pendingNotes }
            val ordered = (listOfNotNull(focus) + inputs.pendingNotes.keys.sortedByDescending { inputs.pendingNotes[it] }).distinct()
            picks += WidgetPick.PendingNotes(ordered.associateWith { inputs.pendingNotes.getValue(it) }, focus)
        }
        return picks
    }
}

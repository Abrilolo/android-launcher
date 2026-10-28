package com.adrian.minimallauncher.data.routine

import com.adrian.minimallauncher.data.ReminderRecord
import com.adrian.minimallauncher.data.Space
import java.time.LocalDate
import java.time.LocalTime

/**
 * One item with an hour in the current phase: a timed action of the phase or a reminder linked to it.
 * [icon] is the icon key the UI draws (the action's catalog icon, or [REMINDER_ICON]); [title] is the reminder's
 * own text (null for actions), for edit mode only. [key] is its done mark for the day.
 */
data class PhaseReminder(
    val id: String,
    val source: Source,
    val icon: String,
    val at: LocalTime,
    val done: Boolean,
    val key: String,
    val title: String? = null,
) {
    enum class Source { ACTION, REMINDER }

    companion object {
        const val REMINDER_ICON = "campana"
    }
}

object PhaseReminders {
    /** Day check key of a reminder done that day, "<date>|reminder_<id>". */
    fun doneKey(date: LocalDate, reminderId: String) = "$date|reminder_$reminderId"

    /** The phase's enabled actions that have an hour plus the reminders linked to it, sorted by time. */
    fun of(phase: RoutinePhase, reminders: List<ReminderRecord>, date: LocalDate, done: Set<String>): List<PhaseReminder> {
        val actions = phase.actions.filter { it.enabled && !it.kind.runsOnEnter }.mapNotNull { a ->
            val at = a.at ?: return@mapNotNull null
            val key = RoutineEngine.dotKey(date, phase.id, a.id)
            PhaseReminder(a.id, PhaseReminder.Source.ACTION, a.kind.icon, at, key in done, key)
        }
        val linked = reminders.filter { it.phaseId == phase.id }.map { r ->
            val key = doneKey(date, r.id)
            PhaseReminder(r.id, PhaseReminder.Source.REMINDER, PhaseReminder.REMINDER_ICON, LocalTime.of(r.hour, r.minute), key in done, key, r.title)
        }
        return (actions + linked).sortedBy { it.at }
    }

    /** The first pending item by hour, overdue ones first: the urgent one for the widget. */
    fun next(items: List<PhaseReminder>): PhaseReminder? = items.firstOrNull { !it.done }

    /**
     * The phase a reminder belongs to, from what it stored: a phase id; a phase name; or, for reminders saved
     * with the three old spaces, the phase that came from that space (Mañana → inicio, Trabajo → trabajo,
     * Noche → casa). Anything else (a deleted phase) goes to the phase whose window holds its hour.
     */
    fun phaseIdFor(stored: String, hour: Int, minute: Int, phases: List<RoutinePhase>, spaces: List<Space>): String {
        phases.firstOrNull { it.id == stored }?.let { return it.id }
        phases.firstOrNull { it.name.equals(stored, ignoreCase = true) }?.let { return it.id }
        val legacy = listOf(RoutineDefaults.INICIO, RoutineDefaults.TRABAJO, RoutineDefaults.CASA)
        val spaceIndex = spaces.indexOfFirst { it.name.equals(stored, ignoreCase = true) }
            .takeIf { it >= 0 } ?: Space.all.indexOfFirst { it.name.equals(stored, ignoreCase = true) }
        legacy.getOrNull(spaceIndex)?.let { id -> phases.firstOrNull { it.id == id } }?.let { return it.id }
        val time = LocalTime.of(hour, minute)
        return (phases.firstOrNull { it.window?.contains(time) == true } ?: phases.first()).id
    }
}

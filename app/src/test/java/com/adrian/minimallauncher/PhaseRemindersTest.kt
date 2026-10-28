package com.adrian.minimallauncher

import com.adrian.minimallauncher.data.ReminderRecord
import com.adrian.minimallauncher.data.Space
import com.adrian.minimallauncher.data.routine.PhaseReminder
import com.adrian.minimallauncher.data.routine.PhaseReminders
import com.adrian.minimallauncher.data.routine.RoutineDefaults
import com.adrian.minimallauncher.data.routine.RoutineEngine
import com.adrian.minimallauncher.data.routine.WidgetInputs
import com.adrian.minimallauncher.data.routine.WidgetPick
import com.adrian.minimallauncher.data.routine.WidgetSelector
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseRemindersTest {
    private val phases = RoutineDefaults.fromSpaces(Space.all)
    private val trabajo = phases.first { it.id == RoutineDefaults.TRABAJO }
    private val day = LocalDate.of(2026, 10, 28)

    @Test fun mergesTimedActionsAndLinkedRemindersByHour() {
        val reminders = listOf(
            ReminderRecord("r1", "junta", 12, 10, trabajo.id),
            ReminderRecord("r2", "otra fase", 12, 15, RoutineDefaults.CASA),
        )
        val items = PhaseReminders.of(trabajo, reminders, day, emptySet())
        assertEquals(items.map { it.at }, items.map { it.at }.sorted())
        val reminder = items.single { it.source == PhaseReminder.Source.REMINDER }
        assertEquals("r1", reminder.id)
        assertEquals(PhaseReminder.REMINDER_ICON, reminder.icon)
        assertTrue(items.filter { it.source == PhaseReminder.Source.ACTION }.all { it.icon.isNotEmpty() })
    }

    @Test fun nextIsTheFirstPendingByHour() {
        val reminders = listOf(ReminderRecord("r1", "junta", 12, 10, trabajo.id))
        val first = PhaseReminders.of(trabajo, reminders, day, emptySet()).first()
        val done = setOf(first.key)
        val items = PhaseReminders.of(trabajo, reminders, day, done)
        assertEquals(items.first { !it.done }, PhaseReminders.next(items))
        val picks = WidgetSelector.pick(trabajo, day.atTime(13, 0), WidgetInputs(phaseReminders = items))
        assertEquals(items.first { !it.done }, (picks.first { it is WidgetPick.PhaseStep } as WidgetPick.PhaseStep).next)
    }

    @Test fun oldSpaceNamesMapToTheirPhase() {
        assertEquals(RoutineDefaults.INICIO, PhaseReminders.phaseIdFor("Mañana", 7, 0, phases, Space.all))
        assertEquals(RoutineDefaults.TRABAJO, PhaseReminders.phaseIdFor("Trabajo", 13, 0, phases, Space.all))
        assertEquals(RoutineDefaults.CASA, PhaseReminders.phaseIdFor("Noche", 21, 0, phases, Space.all))
        assertEquals(RoutineDefaults.SALIDA, PhaseReminders.phaseIdFor("salida", 18, 0, phases, Space.all))
        // A deleted phase: the one whose window holds the hour.
        assertEquals(RoutineEngine.phaseAt(day.atTime(LocalTime.of(15, 0)), phases, null).id, PhaseReminders.phaseIdFor("gone", 15, 0, phases, Space.all))
    }
}

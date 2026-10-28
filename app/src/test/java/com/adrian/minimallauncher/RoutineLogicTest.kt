package com.adrian.minimallauncher

import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.ForcedMode
import com.adrian.minimallauncher.data.ForcedStep
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.data.PendingPayment
import com.adrian.minimallauncher.data.Space
import com.adrian.minimallauncher.data.routine.PlaceEvent
import com.adrian.minimallauncher.data.routine.PlaceIds
import com.adrian.minimallauncher.data.routine.RoutineDefaults
import com.adrian.minimallauncher.data.routine.RoutineEngine
import com.adrian.minimallauncher.data.routine.PhaseMode
import com.adrian.minimallauncher.data.routine.RoutinePhase
import com.adrian.minimallauncher.data.routine.Trigger
import com.adrian.minimallauncher.feature.exercise.ExerciseBurn
import com.adrian.minimallauncher.feature.finance.Budget
import com.adrian.minimallauncher.feature.finance.BudgetLimit
import com.adrian.minimallauncher.feature.finance.BudgetPeriod
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineLogicTest {
    private val phases = RoutineDefaults.fromSpaces(Space.all)
    private val day = LocalDate.of(2026, 10, 28) // a Wednesday

    @Test fun phasesKeepTheSpacesHoursAndCoverTheDay() {
        assertEquals(listOf(RoutineDefaults.INICIO, RoutineDefaults.TRASLADO, RoutineDefaults.TRABAJO, RoutineDefaults.SALIDA, RoutineDefaults.CASA), phases.map { it.id })
        for (minute in 0 until 24 * 60 step 15) {
            val time = LocalTime.MIDNIGHT.plusMinutes(minute.toLong())
            val phase = RoutineEngine.phaseAt(day.atTime(time), phases, null)
            assertEquals("same groups as the old space at $time", Space.at(day.atTime(time)).availableGroups, phase.groups)
        }
        assertEquals(RoutineDefaults.TRABAJO, RoutineEngine.phaseAt(day.atTime(15, 0), phases, null).id)
        assertEquals(RoutineDefaults.CASA, RoutineEngine.phaseAt(day.atTime(23, 0), phases, null).id)
        assertEquals(RoutineDefaults.CASA, RoutineEngine.phaseAt(day.plusDays(1).atTime(2, 0), phases, null).id)
    }

    @Test fun leavingTheOfficeStartsSalidaUntilTheNextWindow() {
        val salida = RoutineEngine.phaseFor(PlaceIds.OFICINA, entered = false, phases)!!
        val event = PlaceEvent(salida.id, day.atTime(17, 30))
        assertEquals(RoutineDefaults.SALIDA, RoutineEngine.phaseAt(day.atTime(17, 40), phases, event).id)
        // An event from before the current window started no longer counts.
        assertEquals(RoutineDefaults.TRABAJO, RoutineEngine.phaseAt(day.plusDays(1).atTime(13, 0), phases, event).id)
        val home = PlaceEvent(RoutineDefaults.CASA, day.atTime(18, 20))
        assertEquals(RoutineDefaults.CASA, RoutineEngine.phaseAt(day.atTime(18, 30), phases, home).id)
    }

    private fun expense(date: LocalDate, cents: Long, category: String = "", kind: MoneyKind = MoneyKind.EXTRA) =
        FinanceRecord(date.toString() + cents + category, cents, "x", date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), kind, category)

    @Test fun overspendingLowersTheRestOfTheWeek() {
        val monday = Budget.weekStart(day)
        val limits = listOf(BudgetLimit(BudgetLimit.idOf(BudgetLimit.TOTAL, BudgetPeriod.WEEK), BudgetLimit.TOTAL, BudgetPeriod.WEEK, 70_000))
        // Monday and Tuesday spent 30,000 of a 70,000 week: Wednesday gets (70,000 − 30,000) ÷ 5 days.
        val records = listOf(expense(monday, 20_000), expense(monday.plusDays(1), 10_000), expense(day, 1_000))
        val s = Budget.summary(records, limits, 10, 0, day)
        assertEquals(8_000, s.todayAllowanceCents)
        assertEquals(7_000, s.todayLeftCents)
        assertEquals(31_000, s.weekSpentCents)
    }

    @Test fun bagIsIncomeMinusFixedMinusSaving() {
        val records = listOf(
            expense(day.withDayOfMonth(1), 1_000_000, kind = MoneyKind.INCOME_FIXED),
            expense(day.withDayOfMonth(2), 400_000, kind = MoneyKind.FIXED),
            expense(day.withDayOfMonth(3), 50_000, kind = MoneyKind.GIFT),
        )
        val s = Budget.summary(records, emptyList(), 20, 0, day)
        assertEquals(120_000, s.savingCents)
        assertEquals(480_000, s.bagCents)
        assertEquals(50_000, s.otherIncomeCents)
        assertEquals(480_000L * 7 / 31, s.weekAllowanceCents)
    }

    @Test fun topicDayBudgetCarriesOver() {
        val limit = BudgetLimit(BudgetLimit.idOf("comida", BudgetPeriod.DAY), "comida", BudgetPeriod.DAY, 20_000)
        val records = listOf(expense(Budget.weekStart(day), 40_000, "comida"))
        val topic = Budget.summary(records, listOf(limit), 0, 0, day).topics.single()
        assertEquals((140_000L - 40_000) / 5, topic.todayAllowanceCents)
    }

    @Test fun settlingASharedBillLowersBoth() {
        val bill = FinanceRecord("s", 90_000, "cena", 0, MoneyKind.SHARED, pendingFromOthersCents = 60_000)
        val after = Budget.settled(bill, 20_000)
        assertEquals(40_000, after.pendingFromOthersCents)
        assertEquals(70_000, after.amountCents)
        assertEquals(0, Budget.settled(after, 999_999).pendingFromOthersCents)
    }

    @Test fun excessPicksAnExerciseOfTheSameGroup() {
        val stack = ExerciseBurn.PLACEHOLDER_STACK
        val small = ExerciseBurn.pick(stack, "cardio", 50f, 70f)!!
        assertEquals("cardio", small.exercise.muscleGroup)
        assertTrue(small.kcal >= 50f)
        val big = ExerciseBurn.pick(stack, "piernas", 400f, 70f)!!
        assertEquals("ph_piernas_zancadas", big.exercise.id)
        assertTrue(big.minutes > big.exercise.minutes && big.kcal >= 400f)
        assertNull(ExerciseBurn.pick(stack, "pecho", 0f, 70f))
    }

    @Test fun forcedFlowSteps() {
        val p = PendingPayment("p", 8500, PendingPayment.SOURCE_DEBUG, "OXXO")
        assertEquals(ForcedStep.ASK_FOOD, ForcedMode.state(listOf(p), emptyMap(), false)!!.step)
        assertEquals(ForcedStep.EXPENSE, ForcedMode.state(listOf(p), mapOf("p" to true), false)!!.step)
        assertEquals(ForcedStep.MEAL, ForcedMode.state(listOf(p.copy(financeId = "f")), emptyMap(), false)!!.step)
        assertEquals(ForcedStep.ROUTINE_MEALS, ForcedMode.state(emptyList(), emptyMap(), true)!!.step)
        assertNull(ForcedMode.state(emptyList(), emptyMap(), false))
    }


    @Test fun orderedPhaseMarksOneStepAtATime() {
        val casa = phases.first { it.id == RoutineDefaults.CASA }
        assertEquals(PhaseMode.ORDERED, casa.mode)
        val states = RoutineEngine.actionStates(casa, day, emptySet())
        val (first, second) = casa.actions
        assertTrue(RoutineEngine.canMark(casa, states, first.id))
        assertFalse(RoutineEngine.canMark(casa, states, second.id))
        val done = RoutineEngine.actionStates(casa, day, setOf(RoutineEngine.dotKey(day, casa.id, first.id)))
        assertEquals(second.id, RoutineEngine.nextStep(done)!!.action.id)
        // Turned off (a standing choice, not per day): skipped by the next step.
        val withOff = casa.copy(actions = casa.actions.map { if (it.id == first.id) it.copy(enabled = false) else it })
        val off = RoutineEngine.actionStates(withOff, day, emptySet())
        assertEquals(second.id, RoutineEngine.nextStep(off)!!.action.id)
    }

    @Test fun userPhasesWithAndWithoutWindow() {
        val gym = RoutinePhase.new("Gimnasio", Trigger.Between(LocalTime.of(20, 0), LocalTime.of(21, 0)))
        val noWindow = RoutinePhase.new("Visita", Trigger.Arrive("gimnasio_2"))
        assertNull(noWindow.window)
        // A phase put first wins its window over the seed; a place-only phase never wins by the clock.
        val list = listOf(gym) + phases + noWindow
        assertEquals(gym.id, RoutineEngine.phaseAt(day.atTime(20, 30), list, null).id)
        assertEquals(RoutineDefaults.CASA, RoutineEngine.phaseAt(day.atTime(21, 30), list, null).id)
        assertEquals(noWindow.id, RoutineEngine.phaseAt(day.atTime(21, 40), list, PlaceEvent(noWindow.id, day.atTime(21, 35))).id)
        // Gap in the day: the phase whose window began last holds.
        val gappy = listOf(gym, RoutinePhase.new("Mañana", Trigger.Between(LocalTime.of(7, 0), LocalTime.of(9, 0))))
        assertEquals(gym.id, RoutineEngine.phaseAt(day.atTime(23, 0), gappy, null).id)
    }
}

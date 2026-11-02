package com.adrian.minimallauncher.feature.exercise

import com.adrian.minimallauncher.data.routine.RoutineDefaults
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

/** What the scene needs besides the routine: [phase] picks the person's day, [boot] reopens it after Guardar. */
internal data class SceneInputs(val phase: String, val trainingNow: Boolean, val completed: Boolean, val excess: String, val boot: String?)

/**
 * The routine the user edits in Tu rutina, stored as JSON next to the session drafts (so it goes in the backup):
 * [WEEK] holds the seven days, [todayId] the one chosen with Guardar. Without either, the day follows [TokyoGuide].
 */
internal object RoutinePlans {
    const val WEEK = "plan:week"
    fun todayId(date: LocalDate) = "plan:today:$date"

    private val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    private val monthNames = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

    fun dateLabel(date: LocalDate) = "${dayNames[date.dayOfWeek.value - 1]}, ${date.dayOfMonth} ${monthNames[date.monthValue - 1]}"

    private fun shortDate(date: LocalDate) = "%02d %s".format(date.dayOfMonth, monthNames[date.monthValue - 1].take(3).uppercase())

    private fun weekPlan(plans: Map<String, String>, index: Int): JSONObject? =
        plans[WEEK]?.let { runCatching { JSONObject(it).getJSONArray("days").getJSONObject(index) }.getOrNull() }

    /** The chosen routine wins; then the edited week; then the guide. */
    fun sessionFor(date: LocalDate, plans: Map<String, String>): Session {
        val edited = plans[todayId(date)]?.let { runCatching { JSONObject(it) }.getOrNull() } ?: weekPlan(plans, date.dayOfWeek.value - 1)
        return edited?.let { runCatching { session(it) }.getOrNull() } ?: TokyoGuide.sessionFor(date)
    }

    private fun session(plan: JSONObject): Session {
        val items = plan.getJSONArray("items")
        val exercises = (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            val group = item.getString("group")
            val cardio = group == "cardio"
            val sets = item.getInt("sets")
            val reps = item.getInt("reps") * if (cardio) 60 else 1
            PlanExercise(
                id = item.getString("id"), name = item.getString("name"), sets = sets, scheme = "$sets × $reps-$reps",
                how = item.optString("how"), care = item.optString("care"), core = group == "core",
                timed = cardio || item.optBoolean("timed"), muscle = group, startKg = item.optDouble("kg", 0.0).toFloat(),
            )
        }
        val minutes = plan.optInt("minutes", 0)
        val kind = when {
            exercises.isEmpty() -> SessionKind.REST
            exercises.all { it.muscle == "cardio" } -> SessionKind.CARDIO
            else -> SessionKind.STRENGTH
        }
        val title = plan.optString("name", "Rutina")
        return Session(title.take(1).uppercase(), title, if (minutes > 0) "$minutes MIN" else "", kind, exercises)
    }

    /** A session as the plan the screens edit. Cardio and intervals, which have no list, become one timed item. */
    fun planOf(session: Session, date: LocalDate, lastKg: (String) -> Float?): JSONObject {
        val items = session.exercises.map { ex ->
            JSONObject().put("id", ex.id).put("name", ex.name).put("group", ex.group()).put("sets", ex.sets).put("reps", ex.minTarget)
                .put("kg", (lastKg(ex.id) ?: ex.startKg ?: 0f).toDouble()).put("timed", ex.timed).put("how", ex.how).put("care", ex.care)
        }.ifEmpty {
            if (session.kind == SessionKind.REST) emptyList()
            else listOf(JSONObject().put("id", "session_${session.kind.name.lowercase()}").put("name", session.title).put("group", "cardio")
                .put("sets", 1).put("reps", session.minutes() ?: 20).put("kg", 0).put("timed", true).put("how", session.note).put("care", session.note))
        }
        return JSONObject().put("day", dayNames[date.dayOfWeek.value - 1]).put("date", shortDate(date)).put("name", session.title)
            .put("minutes", session.minutes() ?: 0).put("groups", JSONArray(items.map { it.getString("group") }.distinct()))
            .put("items", JSONArray(items))
    }

    /** Monday to Sunday of [date]'s week, with today's labels on whatever is saved. */
    fun week(date: LocalDate, plans: Map<String, String>, lastKg: (String) -> Float?): JSONArray {
        val monday = date.minusDays(date.dayOfWeek.value - 1L)
        return JSONArray((0..6).map { i ->
            val day = monday.plusDays(i.toLong())
            val saved = weekPlan(plans, i)
            saved?.put("day", dayNames[i])?.put("date", shortDate(day)) ?: planOf(TokyoGuide.sessionFor(day), day, lastKg)
        })
    }

    /** The routine phase as one of the person's days; a phase the user created goes by the clock. */
    fun scenePhase(phaseId: String, now: LocalTime, training: Boolean): String = when {
        training -> "training"
        phaseId == RoutineDefaults.INICIO -> "morning"
        phaseId == RoutineDefaults.TRASLADO || phaseId == RoutineDefaults.SALIDA -> "commute"
        phaseId == RoutineDefaults.TRABAJO -> "work"
        now.hour >= 21 || now.hour < 5 -> "night"
        phaseId == RoutineDefaults.CASA -> "home"
        now.hour < 9 -> "morning"
        now.hour < 18 -> "work"
        else -> "home"
    }
}

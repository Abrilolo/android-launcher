package com.adrian.minimallauncher.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.adrian.minimallauncher.ReminderReceiver
import com.adrian.minimallauncher.data.routine.RoutineAction
import com.adrian.minimallauncher.data.routine.RoutinePhase
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {
    fun schedule(reminder: ReminderRecord) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_ID, reminder.id)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_PHASE, reminder.phaseId)
        }
        setAlarm(reminder.id.hashCode(), LocalTime.of(reminder.hour, reminder.minute), intent)
    }

    fun cancel(id: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_REMINDER }
        cancelAlarm(id.hashCode(), intent)
    }

    /**
     * Schedules every action with an hour, in every phase, for its next time; one whose phase is not the current
     * one when it fires is skipped by the receiver. Actions no longer in [phases] are cancelled via [previous].
     */
    fun schedulePhases(phases: List<RoutinePhase>, previous: List<RoutinePhase> = emptyList()) {
        val keep = phases.flatMap { p -> p.actions.filter { it.enabled && it.at != null && !it.kind.runsOnEnter }.map { p.id to it } }
        val keepCodes = keep.map { (phaseId, action) -> dotCode(phaseId, action.id) }.toSet()
        previous.flatMap { p -> p.actions.map { dotCode(p.id, it.id) } }.filter { it !in keepCodes }.forEach { cancelAlarm(it, dotIntent()) }
        keep.forEach { (phaseId, action) -> scheduleDot(phaseId, action) }
    }

    fun scheduleDot(phaseId: String, dot: RoutineAction) {
        val at = dot.at ?: return
        val intent = dotIntent().apply {
            putExtra(EXTRA_PHASE, phaseId)
            putExtra(EXTRA_DOT, dot.id)
        }
        setAlarm(dotCode(phaseId, dot.id), at, intent)
    }

    private fun dotIntent() = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_DOT }
    private fun dotCode(phaseId: String, dotId: String) = "dot|$phaseId|$dotId".hashCode()

    private fun setAlarm(code: Int, time: LocalTime, intent: Intent) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val pending = PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pending,
        )
    }

    private fun cancelAlarm(code: Int, intent: Intent) {
        PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.adrian.minimallauncher.ACTION_REMINDER"
        const val ACTION_DOT = "com.adrian.minimallauncher.ACTION_DOT"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_SPACE = "reminder_space"
        const val EXTRA_PHASE = "reminder_phase"
        const val EXTRA_DOT = "reminder_dot"
    }
}

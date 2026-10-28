package com.adrian.minimallauncher

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.adrian.minimallauncher.data.ReminderScheduler
import com.adrian.minimallauncher.data.LauncherDatabase
import com.adrian.minimallauncher.data.PersonalRepository
import com.adrian.minimallauncher.data.routine.PhaseReminders
import com.adrian.minimallauncher.data.routine.RoutineEngine
import com.adrian.minimallauncher.data.routine.RoutineRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == ReminderScheduler.ACTION_DOT) {
                    onDot(context, intent)
                    return@launch
                }
                val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return@launch
                // Read the reminder as it is now: it may have been edited or moved to another phase.
                val reminder = PersonalRepository(LauncherDatabase.get(context)).reminders.first().firstOrNull { it.id == id } ?: return@launch
                ReminderScheduler(context).schedule(reminder)
                // It rings only while its phase is the current one, and not once done today.
                val routine = RoutineRepository(context)
                val current = RoutineEngine.phaseAt(LocalDateTime.now(), routine.phases.first(), routine.lastEvent.first())
                val done = LauncherDatabase.get(context).personal().hasCheck(PhaseReminders.doneKey(LocalDate.now(), id)) > 0
                if (current.id != reminder.phaseId || done) return@launch
                createReminderChannel(context)
                if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(context).notify(id.hashCode(), NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(reminder.title)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build())
                }
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * A phase reminder: it only shows while its phase is the current one and it is not done yet, and the
     * notification carries just the dot's name (the launcher draws the dot). Always rescheduled for tomorrow.
     */
    private suspend fun onDot(context: Context, intent: Intent) {
        val phaseId = intent.getStringExtra(ReminderScheduler.EXTRA_PHASE) ?: return
        val dotId = intent.getStringExtra(ReminderScheduler.EXTRA_DOT) ?: return
        val routine = RoutineRepository(context)
        val phases = routine.phases.first()
        val phase = phases.firstOrNull { it.id == phaseId } ?: return
        val dot = phase.actions.firstOrNull { it.id == dotId } ?: return
        ReminderScheduler(context).scheduleDot(phaseId, dot)
        val current = RoutineEngine.phaseAt(LocalDateTime.now(), phases, routine.lastEvent.first())
        val key = RoutineEngine.dotKey(LocalDate.now(), phaseId, dotId)
        val dao = LauncherDatabase.get(context).personal()
        // Not shown when done or turned off.
        if (current.id != phaseId || !dot.enabled || dao.hasCheck(key) > 0) return
        createReminderChannel(context)
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                android.app.PendingIntent.getActivity(context, 0, it, android.app.PendingIntent.FLAG_IMMUTABLE)
            }
            NotificationManagerCompat.from(context).notify(key.hashCode(), NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.radiobutton_on_background)
                .setContentTitle(dot.kind.icon)
                .setContentIntent(open)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build())
        }
    }

    private fun createReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Launcher reminders", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object { private const val CHANNEL_ID = "personal_reminders" }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PersonalRepository(LauncherDatabase.get(context)).reminders.first().forEach { ReminderScheduler(context).schedule(it) }
                val routine = RoutineRepository(context)
                ReminderScheduler(context).schedulePhases(routine.phases.first())
                // Geofences do not survive a reboot.
                com.adrian.minimallauncher.data.routine.Places.register(context, routine.places.first())
            }
            finally { pending.finish() }
        }
    }
}

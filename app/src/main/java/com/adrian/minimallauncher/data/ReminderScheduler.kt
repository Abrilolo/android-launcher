package com.adrian.minimallauncher.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.adrian.minimallauncher.ReminderReceiver
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {
    fun schedule(reminder: ReminderRecord) {
        val now = LocalDateTime.now()
        var next = now.withHour(reminder.hour).withMinute(reminder.minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_ID, reminder.id)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_SPACE, reminder.spaceName)
        }
        val pending = PendingIntent.getBroadcast(context, reminder.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pending,
        )
    }

    fun cancel(id: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_REMINDER }
        PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.adrian.minimallauncher.ACTION_REMINDER"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_SPACE = "reminder_space"
    }
}

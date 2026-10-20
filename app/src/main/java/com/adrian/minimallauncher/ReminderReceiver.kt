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
import com.adrian.minimallauncher.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return@launch
                val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
                val space = intent.getStringExtra(ReminderScheduler.EXTRA_SPACE).orEmpty()
                createReminderChannel(context)
                if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(context).notify(id.hashCode(), NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(title)
                        .setContentText(space)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build())
                }
                SettingsRepository(context).reminders.first().firstOrNull { it.id == id }?.let { ReminderScheduler(context).schedule(it) }
            } finally {
                pending.finish()
            }
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
            try { SettingsRepository(context).reminders.first().forEach { ReminderScheduler(context).schedule(it) } }
            finally { pending.finish() }
        }
    }
}

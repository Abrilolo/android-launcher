package com.adrian.minimallauncher.feature.exercise

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.Vibrator

/** How the session feels: one pattern per kind of answer, shared by the scene and the alert. */
internal object SessionHaptics {
    private val shots = mapOf("tick" to (10L to 90), "press" to (14L to 140), "confirm" to (24L to 210))
    private val waves = mapOf(
        "success" to (longArrayOf(0, 20, 70, 36) to intArrayOf(0, 170, 0, 255)),
        "warn" to (longArrayOf(0, 34, 50, 34) to intArrayOf(0, 230, 0, 230)),
        "alert" to (longArrayOf(0, 90, 110, 90, 110, 180) to intArrayOf(0, 255, 0, 255, 0, 255)),
    )

    fun effect(kind: String, vibrator: Vibrator): VibrationEffect? {
        val fine = vibrator.hasAmplitudeControl()
        shots[kind]?.let { (ms, strength) -> return VibrationEffect.createOneShot(ms, if (fine) strength else VibrationEffect.DEFAULT_AMPLITUDE) }
        val (times, strengths) = waves[kind] ?: return null
        return if (fine) VibrationEffect.createWaveform(times, strengths, -1) else VibrationEffect.createWaveform(times, -1)
    }
}

/**
 * The end of a rest or a timed set, felt with the screen off or the scene closed. The deadline is armed from the
 * scene and fires only when the scene is not there to feel it. Inexact on purpose: exact alarms need a permission.
 */
internal object ExerciseTimerAlert {
    @Volatile var sceneVisible = false

    fun arm(context: Context, at: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(context, 0, Intent(context, ExerciseTimerReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (at > 0) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending) else alarms.cancel(pending)
    }
}

class ExerciseTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ExerciseTimerAlert.sceneVisible) return
        val vibrator = context.getSystemService(Vibrator::class.java)
        val effect = SessionHaptics.effect("alert", vibrator) ?: return
        // Android drops plain vibrations from the background; the alarm usage is the one that gets through.
        @Suppress("DEPRECATION")
        vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
    }
}

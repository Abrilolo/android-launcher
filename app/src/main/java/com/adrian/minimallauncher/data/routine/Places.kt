package com.adrian.minimallauncher.data.routine

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.adrian.minimallauncher.data.ForcedRepository
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.SettingsRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Geofences around casa and oficina. Leaving the office starts salida and arriving home starts casa
 * (see [RoutineEngine.phaseFor]). Needs fine location, and background location ("Permitir todo el tiempo")
 * so the geofences fire with the launcher closed.
 */
object Places {
    /** Fine first; background is asked apart (Android sends the user to settings for "todo el tiempo"). */
    val FOREGROUND_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    const val BACKGROUND_PERMISSION = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    fun hasForeground(context: Context) = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
    fun hasBackground(context: Context) = Build.VERSION.SDK_INT < 29 || granted(context, BACKGROUND_PERMISSION)
    private fun granted(context: Context, permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun geofenceIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, GeofenceReceiver::class.java),
        // Play Services fills the event into the intent, so it must be mutable.
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0),
    )

    /** Registers (or replaces) the geofences of every saved place; false when location is not granted. */
    @SuppressLint("MissingPermission")
    suspend fun register(context: Context, places: Collection<Place>): Boolean {
        if (!hasForeground(context) || !hasBackground(context)) return false
        val client = LocationServices.getGeofencingClient(context)
        runCatching { client.removeGeofences(geofenceIntent(context)).await() }
        val located = places.filter { it.located }
        if (located.isEmpty()) return true
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofences(located.map { place ->
                Geofence.Builder()
                    .setRequestId(place.id)
                    .setCircularRegion(place.lat!!, place.lng!!, place.radiusM)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
            })
            .build()
        return runCatching { client.addGeofences(request, geofenceIntent(context)).await() }.isSuccess
    }

    /** Saves place [id] at the phone's current location and re-registers the geofences. Null when no fix. */
    @SuppressLint("MissingPermission")
    suspend fun setHere(context: Context, id: String, radiusM: Float = 150f): Place? {
        if (!hasForeground(context)) return null
        val location = runCatching {
            LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        }.getOrNull() ?: return null
        val routine = RoutineRepository(context)
        val known = routine.places.first().firstOrNull { it.id == id }
        val place = Place(id, known?.name ?: id, location.latitude, location.longitude, radiusM)
        routine.setPlace(place)
        register(context, routine.places.first())
        return place
    }

    /** wa.me link with the message ready; WhatsApp opens the chat and the user taps send. */
    fun whatsAppIntent(phone: String, text: String): Intent {
        val uri = Uri.parse("https://wa.me/${phone.filter(Char::isDigit)}?text=${Uri.encode(text)}")
        return Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Opens the WhatsApp chat (or the link in whatever handles it if WhatsApp is missing). */
    fun openWhatsApp(context: Context, phone: String, text: String): Boolean {
        if (phone.none(Char::isDigit)) return false
        val intent = whatsAppIntent(phone, text)
        return runCatching { context.startActivity(Intent(intent).setPackage(WHATSAPP)) }
            .recoverCatching { context.startActivity(intent) }.isSuccess
    }

    const val WHATSAPP = "com.whatsapp"
}

/** What a phase does when it starts, shared by the ViewModel (launcher open) and the geofence receiver (closed). */
object RoutineActions {
    /** The chat and text of the phase's WhatsApp action; its own config, else the ones in settings. */
    fun whatsAppTarget(phase: RoutinePhase, s: ModuleSettings): Pair<String, String>? {
        val action = phase.actions.firstOrNull { it.kind.kind == ActionKind.WHATSAPP } ?: return null
        val phone = action.config[ActionCatalog.PHONE]?.takeIf { it.any(Char::isDigit) } ?: s.whatsappPhone
        val text = action.config[ActionCatalog.TEXT]?.takeIf { it.isNotBlank() } ?: s.whatsappText
        return if (phone.any(Char::isDigit)) phone to text else null
    }

    /**
     * Runs the enter actions that need no screen: a "pendientes" action marks the day's unmarked meals as
     * pending, which turns on the forced flow. Opening WhatsApp needs the screen, so it is left to the caller.
     */
    suspend fun onEnter(context: Context, phase: RoutinePhase) {
        if (phase.actions.any { it.kind.kind == ActionKind.PENDING }) {
            val settings = SettingsRepository(context).moduleSettings.first()
            val habits = com.adrian.minimallauncher.data.LauncherDatabase.get(context).personal().allChecks().map { it.key }.toSet()
            val now = LocalDateTime.now()
            val unmarked = settings.meals.any { !it.time.isAfter(now.toLocalTime()) && "${LocalDate.now()}|meal_${it.id}" !in habits }
            if (unmarked) ForcedRepository(context).setRoutinePending(true)
        }
    }
}

/** A geofence fired: record the place event (it starts salida or casa) and run that phase's enter actions. */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val entered = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> true
            Geofence.GEOFENCE_TRANSITION_EXIT -> false
            else -> return
        }
        val places = event.triggeringGeofences.orEmpty().map { it.requestId }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val routine = RoutineRepository(context)
                val phases = routine.phases.first()
                val phase = places.firstNotNullOfOrNull { RoutineEngine.phaseFor(it, entered, phases) } ?: return@launch
                routine.setLastEvent(PlaceEvent(phase.id, LocalDateTime.now()))
                if (routine.lastEntered.first() == phase.id) return@launch
                routine.setLastEntered(phase.id)
                RoutineActions.onEnter(context, phase)
                notifyWhatsApp(context, phase)
            } finally {
                pending.finish()
            }
        }
    }

    // The launcher is closed, so the chat cannot open by itself: a notification opens it with one tap.
    private suspend fun notifyWhatsApp(context: Context, phase: RoutinePhase) {
        val (phone, text) = RoutineActions.whatsAppTarget(phase, SettingsRepository(context).moduleSettings.first()) ?: return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID, "Rutina", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 1, Places.whatsAppIntent(phone, text), PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompat.from(context).notify(CHANNEL_ID.hashCode(), NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle(phase.name.lowercase())
            .setContentIntent(open)
            .setAutoCancel(true)
            .build())
    }

    private companion object { const val CHANNEL_ID = "routine" }
}

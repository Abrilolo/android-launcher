package com.adrian.minimallauncher.data.routine

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.adrian.minimallauncher.data.SettingsRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** An action of the current phase today: [done] is marked ([key] is that day check); [on] is the action's standing on/off. */
data class ActionState(val action: RoutineAction, val done: Boolean, val key: String) {
    val on: Boolean get() = action.enabled
    /** WhatsApp and pendientes run when the phase starts; the rest are dots to mark. */
    val markable: Boolean get() = !action.kind.runsOnEnter
}

/** Decides the phase for a moment; pure, so the ViewModel, the receivers and the widgets agree. */
object RoutineEngine {
    /**
     * The phase whose time window contains [now] (or, in a gap, the one whose window began last), unless a
     * place event started another phase after that: leaving the office at 17:30 is salida while the clock
     * still says trabajo. A phase without a window only comes from place events.
     */
    fun phaseAt(now: LocalDateTime, phases: List<RoutinePhase>, lastEvent: PlaceEvent?): RoutinePhase {
        val timed = phases.filter { it.window != null }
        val byTime = timed.firstOrNull { it.window!!.contains(now.toLocalTime()) }
            ?: timed.maxByOrNull { it.window!!.startedAt(now) }
            ?: phases.first()
        val event = lastEvent ?: return byTime
        val placed = phases.firstOrNull { it.id == event.phaseId } ?: return byTime
        val since = byTime.window?.startedAt(now) ?: now.minusHours(24)
        return if (!event.at.isAfter(now) && !event.at.isBefore(since)) placed else byTime
    }

    /** The phase a geofence transition starts, if any phase listens to it. */
    fun phaseFor(place: String, entered: Boolean, phases: List<RoutinePhase>): RoutinePhase? = phases.firstOrNull {
        it.trigger == if (entered) Trigger.Arrive(place) else Trigger.Leave(place)
    }

    /** Day check key of a done action, "<date>|routine_<phase>_<action>", stored like the habits. */
    fun dotKey(date: LocalDate, phaseId: String, actionId: String) = "$date|routine_${phaseId}_$actionId"
    fun actionStates(phase: RoutinePhase, date: LocalDate, done: Set<String>): List<ActionState> = phase.actions.map { a ->
        val key = dotKey(date, phase.id, a.id)
        ActionState(a, key in done, key)
    }

    /** The next action to do: in an ordered phase the steps go one after another (ordenar, agua, descanso…). */
    fun nextStep(states: List<ActionState>): ActionState? = states.firstOrNull { it.on && it.markable && !it.done }

    /** Whether [actionId] can be marked now: any one in a chooser phase, only the next one in an ordered phase. */
    fun canMark(phase: RoutinePhase, states: List<ActionState>, actionId: String): Boolean {
        val state = states.firstOrNull { it.action.id == actionId } ?: return false
        if (!state.markable || !state.on) return false
        return phase.mode == PhaseMode.CHOOSER || state.done || nextStep(states)?.action?.id == actionId
    }
}

/**
 * A place of the user's list (casa, oficina, gimnasio… they add, rename and remove them). Its geofence works
 * once [lat]/[lng] are set from the phone's location; [radiusM] is the geofence radius.
 */
data class Place(val id: String, val name: String, val lat: Double? = null, val lng: Double? = null, val radiusM: Float = 150f) {
    val located: Boolean get() = lat != null && lng != null
}

/** The first-load places, without coordinates yet. */
val DEFAULT_PLACES = listOf(Place(PlaceIds.CASA, "Casa"), Place(PlaceIds.OFICINA, "Oficina"), Place(PlaceIds.GIMNASIO, "Gimnasio"))

private val Context.routineStore by preferencesDataStore(name = "routine")

/** The routine's own preferences: phases, places and the last place event. Kept apart from the settings store. */
class RoutineRepository(context: Context, private val settings: SettingsRepository = SettingsRepository(context)) {
    private val context: Context = context.applicationContext ?: context
    private val phasesKey = stringPreferencesKey("phases")
    private val placesKey = stringPreferencesKey("places")
    private val lastEventKey = stringPreferencesKey("last_place_event")
    private val lastEnteredKey = stringPreferencesKey("last_entered_phase")

    /** Saved phases; until the first save, the five phases built from the saved spaces. */
    val phases: Flow<List<RoutinePhase>> = context.routineStore.data.map { prefs ->
        prefs[phasesKey]?.let { runCatching { RoutineJson.decode(JSONArray(it)) }.getOrNull() } ?: RoutineDefaults.fromSpaces(settings.spaces.first())
    }

    /** The user's places in order; the seed until the first edit. */
    val places: Flow<List<Place>> = context.routineStore.data.map { prefs -> prefs[placesKey]?.let(::decodePlaces) ?: DEFAULT_PLACES }

    val lastEvent: Flow<PlaceEvent?> = context.routineStore.data.map { prefs ->
        prefs[lastEventKey]?.let { runCatching { PlaceEvent(it.substringBefore('@'), LocalDateTime.parse(it.substringAfter('@'))) }.getOrNull() }
    }

    /** The phase whose enter actions already ran, so reopening the launcher does not run them twice. */
    val lastEntered: Flow<String?> = context.routineStore.data.map { it[lastEnteredKey] }

    /** First load: the saved spaces become phases once, and from then on the phases are what is edited. */
    suspend fun migrateSpaces() {
        if (context.routineStore.data.first()[phasesKey] == null) setPhases(RoutineDefaults.fromSpaces(settings.spaces.first()))
    }

    suspend fun setPhases(value: List<RoutinePhase>) { context.routineStore.edit { it[phasesKey] = RoutineJson.encode(value).toString() } }
    /** Adds [place], or replaces the one with its id in the same position. */
    suspend fun setPlace(place: Place) = editPlaces { all -> if (all.any { it.id == place.id }) all.map { if (it.id == place.id) place else it } else all + place }
    suspend fun removePlace(id: String) = editPlaces { all -> all.filterNot { it.id == id } }

    private suspend fun editPlaces(change: (List<Place>) -> List<Place>) {
        context.routineStore.edit { prefs -> prefs[placesKey] = encodePlaces(change(prefs[placesKey]?.let(::decodePlaces) ?: DEFAULT_PLACES)) }
    }
    suspend fun setLastEvent(event: PlaceEvent) { context.routineStore.edit { it[lastEventKey] = "${event.phaseId}@${event.at}" } }
    suspend fun setLastEntered(phaseId: String) { context.routineStore.edit { it[lastEnteredKey] = phaseId } }
    suspend fun clearLastEvent() { context.routineStore.edit { it.remove(lastEventKey) } }
    suspend fun clearLastEntered() { context.routineStore.edit { it.remove(lastEnteredKey) } }

    /** Phases and places for the backup file. */
    suspend fun toJson(): JSONObject = JSONObject()
        .put("phases", RoutineJson.encode(phases.first()))
        .put("places", JSONArray(encodePlaces(places.first())))

    suspend fun fromJson(o: JSONObject) {
        o.optJSONArray("phases")?.let(RoutineJson::decode)?.let { setPhases(it) }
        o.optJSONArray("places")?.let { context.routineStore.edit { prefs -> prefs[placesKey] = it.toString() } }
    }

    private fun encodePlaces(places: List<Place>) = JSONArray(places.map {
        JSONObject().put("id", it.id).put("name", it.name).put("lat", it.lat ?: JSONObject.NULL).put("lng", it.lng ?: JSONObject.NULL).put("radiusM", it.radiusM.toDouble())
    }).toString()

    private fun decodePlaces(text: String): List<Place>? = runCatching {
        val a = JSONArray(text)
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val id = o.getString("id")
            Place(id, o.optString("name", id), if (o.isNull("lat")) null else o.optDouble("lat"), if (o.isNull("lng")) null else o.optDouble("lng"), o.optDouble("radiusM", 150.0).toFloat())
        }
    }.getOrNull()
}

package com.adrian.minimallauncher.data.routine

import com.adrian.minimallauncher.data.AppGroup
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.data.Space
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Ids of the seeded places; the user can rename them and add others (see [Place]). */
object PlaceIds {
    const val CASA = "casa"
    const val OFICINA = "oficina"
    const val GIMNASIO = "gimnasio"
}

/** What starts a phase: a time window, or arriving at / leaving a place (geofence). */
sealed interface Trigger {
    data class Between(val start: LocalTime, val end: LocalTime) : Trigger {
        fun contains(time: LocalTime): Boolean = if (start < end) time >= start && time < end else time >= start || time < end

        /** When the occurrence of this window that contains (or last started before) [now] began. */
        fun startedAt(now: LocalDateTime): LocalDateTime {
            val today = now.toLocalDate().atTime(start)
            return if (today.isAfter(now)) today.minusDays(1) else today
        }
    }
    /** [placeId] is a [Place.id] from the user's list. */
    data class Arrive(val placeId: String) : Trigger
    data class Leave(val placeId: String) : Trigger
}

/** How a phase's actions are used: the user picks which ones today ("Iniciar el día"), or they go in order ("Llegar a casa"). */
enum class PhaseMode { CHOOSER, ORDERED }

/** What an action type does, which decides how the launcher runs it. */
enum class ActionKind {
    /** A dot to mark done (agua, caminar…); with an "at" config it also notifies at that hour. */
    DOT,
    /** A dot with a timer of "minutes" (descanso). */
    TIMER,
    /** Opens the "package" app (music, podcast). */
    AUDIO,
    /** Opens a WhatsApp chat ("phone") with "text" ready, when the phase starts. */
    WHATSAPP,
    /** Asks to register what is pending (payments, unmarked meals) when the phase starts. */
    PENDING,
}

/**
 * One kind of action in the catalog. [key] is what is stored, so a new action is one new entry here and no
 * schema change. [icon] is the DotIcons name the UI draws; [configKeys] are the settings an instance may carry.
 */
data class ActionType(val key: String, val label: String, val kind: ActionKind, val icon: String = key, val configKeys: List<String> = emptyList()) {
    /** Runs by itself when its phase starts (WhatsApp, pendientes) instead of waiting to be marked. */
    val runsOnEnter: Boolean get() = kind == ActionKind.WHATSAPP || kind == ActionKind.PENDING
}

/** The extensible list of action types; unknown keys (from a newer version) still load and show as plain dots. */
object ActionCatalog {
    const val AT = "at"
    const val MINUTES = "minutes"
    const val PACKAGE = "package"
    const val PHONE = "phone"
    const val TEXT = "text"

    private val types = linkedMapOf<String, ActionType>()

    fun register(type: ActionType) { types[type.key] = type }
    val all: List<ActionType> get() = types.values.toList()
    fun get(key: String): ActionType = types[key] ?: ActionType(key, key, ActionKind.DOT)

    init {
        register(ActionType("agua", "Agua", ActionKind.DOT, configKeys = listOf(AT)))
        register(ActionType("caminar", "Caminar", ActionKind.DOT, configKeys = listOf(AT)))
        register(ActionType("colacion", "Colación", ActionKind.DOT, configKeys = listOf(AT)))
        register(ActionType("musica", "Música", ActionKind.AUDIO, configKeys = listOf(PACKAGE)))
        register(ActionType("podcast", "Podcast", ActionKind.AUDIO, configKeys = listOf(PACKAGE)))
        register(ActionType("whatsapp", "WhatsApp", ActionKind.WHATSAPP, configKeys = listOf(PHONE, TEXT)))
        register(ActionType("pendientes", "Pendientes", ActionKind.PENDING))
        register(ActionType("ordenar", "Ordenar", ActionKind.DOT, configKeys = listOf(AT)))
        register(ActionType("descanso", "Descanso", ActionKind.TIMER, configKeys = listOf(MINUTES, AT)))
        register(ActionType("gimnasio", "Gimnasio", ActionKind.DOT, configKeys = listOf(AT)))
        register(ActionType("dormir", "Dormir", ActionKind.DOT, configKeys = listOf(AT)))
    }
}

/**
 * One action in a phase: a catalog [type] plus its own small [config] (hour, minutes, chat, app package).
 * [enabled] is the user's standing choice (set up once, e.g. in "Iniciar el día"); it does not reset each day.
 */
data class RoutineAction(val id: String, val type: String, val config: Map<String, String> = emptyMap(), val enabled: Boolean = true) {
    val kind: ActionType get() = ActionCatalog.get(type)
    /** Hour it notifies at, for dots and timers that have one. */
    val at: LocalTime? get() = config[ActionCatalog.AT]?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    val minutes: Int get() = config[ActionCatalog.MINUTES]?.toIntOrNull() ?: 0
    val packageName: String get() = config[ActionCatalog.PACKAGE].orEmpty()

    companion object {
        fun new(type: String, config: Map<String, String> = emptyMap()) = RoutineAction(newRoutineId(), type, config)
    }
}

fun newRoutineId(): String = UUID.randomUUID().toString().take(8)

/**
 * A part of the day the user defines: its name, what starts it, its actions in order and how they are used.
 * [groups] decide which apps open, the same rule [Space] has always applied. [window] is when it holds by the
 * clock; a phase started only by a place may have none (it then holds from the place event to the next window).
 */
data class RoutinePhase(
    val id: String,
    val name: String,
    val trigger: Trigger,
    val window: Trigger.Between?,
    val groups: Set<AppGroup>,
    val mode: PhaseMode = PhaseMode.CHOOSER,
    val actions: List<RoutineAction> = emptyList(),
) {
    /** This phase as the [Space] the app list, the pause and the widgets already understand. */
    fun toSpace(): Space = Space(name, window?.start ?: LocalTime.MIDNIGHT, window?.end ?: LocalTime.MIDNIGHT, groups)

    fun allows(app: LauncherApp): Boolean = toSpace().allows(app)

    companion object {
        fun new(name: String, trigger: Trigger, groups: Set<AppGroup> = setOf(AppGroup.ESSENTIAL)) =
            RoutinePhase(newRoutineId(), name, trigger, trigger as? Trigger.Between, groups)
    }
}

/** A geofence event that started a phase, kept so the phase holds until the next time window begins. */
data class PlaceEvent(val phaseId: String, val at: LocalDateTime)

/** The first-load seed: the plan's five phases, built from the three saved spaces. Everything in it is editable. */
object RoutineDefaults {
    // Seed ids; phases the user adds get generated ids.
    const val INICIO = "inicio"
    const val TRASLADO = "traslado"
    const val TRABAJO = "trabajo"
    const val SALIDA = "salida"
    const val CASA = "casa"

    private fun action(seedId: String, type: String, vararg config: Pair<String, String>) = RoutineAction(seedId, type, config.toMap())

    /**
     * Apps open at the same hours as with the spaces: Mañana splits into inicio and traslado, Trabajo is
     * trabajo, Noche splits into salida and casa.
     */
    fun fromSpaces(spaces: List<Space>): List<RoutinePhase> {
        val morning = spaces.getOrNull(0) ?: Space.all[0]
        val work = spaces.getOrNull(1) ?: Space.all[1]
        val night = spaces.getOrNull(2) ?: Space.all[2]
        val morningSplit = split(morning.startsAt, morning.endsAt)
        val nightSplit = split(night.startsAt, night.endsAt, maxFirst = Duration.ofHours(1))
        val inicio = Trigger.Between(morning.startsAt, morningSplit)
        val traslado = Trigger.Between(morningSplit, morning.endsAt)
        val trabajo = Trigger.Between(work.startsAt, work.endsAt)
        return listOf(
            RoutinePhase(
                INICIO, "Inicio", inicio, inicio, morning.availableGroups, PhaseMode.CHOOSER,
                listOf(
                    action("inicio_agua", "agua", ActionCatalog.AT to morning.startsAt.plusMinutes(15).toString()),
                    action("inicio_caminar", "caminar"),
                    action("inicio_podcast", "podcast"),
                ),
            ),
            RoutinePhase(TRASLADO, "Traslado", traslado, traslado, morning.availableGroups, PhaseMode.CHOOSER, listOf(action("traslado_musica", "musica"), action("traslado_podcast", "podcast"))),
            RoutinePhase(TRABAJO, "Trabajo", trabajo, trabajo, work.availableGroups, PhaseMode.CHOOSER, workActions(work.startsAt, work.endsAt)),
            RoutinePhase(
                SALIDA, "Salida", Trigger.Leave(PlaceIds.OFICINA), Trigger.Between(night.startsAt, nightSplit), night.availableGroups, PhaseMode.ORDERED,
                listOf(action("salida_pendientes", "pendientes"), action("salida_whatsapp", "whatsapp"), action("salida_musica", "musica")),
            ),
            RoutinePhase(
                CASA, "Casa", Trigger.Arrive(PlaceIds.CASA), Trigger.Between(nightSplit, night.endsAt), night.availableGroups, PhaseMode.ORDERED,
                listOf(
                    action("casa_ordenar", "ordenar"),
                    action("casa_agua", "agua"),
                    action("casa_descanso", "descanso", ActionCatalog.MINUTES to "5"),
                    action("casa_gimnasio", "gimnasio"),
                    action("casa_dormir", "dormir"),
                ),
            ),
        )
    }

    // Water every two hours at work, a walk every other time, and a snack midway.
    private fun workActions(start: LocalTime, end: LocalTime): List<RoutineAction> {
        val minutes = minutesBetween(start, end)
        if (minutes < 60) return emptyList()
        val list = mutableListOf<Pair<Long, RoutineAction>>()
        var offset = 60L
        var n = 0
        while (offset < minutes) {
            list += offset to action("trabajo_agua_$n", "agua", ActionCatalog.AT to start.plusMinutes(offset).toString())
            if (n % 2 == 1) list += (offset + 5) to action("trabajo_caminar_$n", "caminar", ActionCatalog.AT to start.plusMinutes(offset + 5).toString())
            offset += 120
            n++
        }
        list += minutes / 2 to action("trabajo_colacion", "colacion", ActionCatalog.AT to start.plusMinutes(minutes / 2).toString())
        return list.sortedBy { it.first }.map { it.second }
    }

    /** A time between [start] and [end]: the first part gets at most [maxFirst], or half the span. */
    private fun split(start: LocalTime, end: LocalTime, maxFirst: Duration = Duration.ofHours(2)): LocalTime =
        start.plusMinutes(minOf(maxFirst.toMinutes(), minutesBetween(start, end) / 2))

    private fun minutesBetween(start: LocalTime, end: LocalTime): Long {
        val m = Duration.between(start, end).toMinutes()
        return if (m <= 0) m + 24 * 60 else m
    }
}

/** Phases are stored as JSON in the routine store and in the backup. Action types are stored by key. */
object RoutineJson {
    fun encode(phases: List<RoutinePhase>): JSONArray = JSONArray(phases.map { p ->
        JSONObject()
            .put("id", p.id)
            .put("name", p.name)
            .put("trigger", encodeTrigger(p.trigger))
            .put("window", p.window?.let(::encodeTrigger) ?: JSONObject.NULL)
            .put("groups", JSONArray(p.groups.map { it.name }))
            .put("mode", p.mode.name)
            .put("actions", JSONArray(p.actions.map { a -> JSONObject().put("id", a.id).put("type", a.type).put("config", JSONObject(a.config)).put("enabled", a.enabled) }))
    })

    fun decode(array: JSONArray): List<RoutinePhase>? = runCatching {
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            RoutinePhase(
                id = o.getString("id"),
                name = o.getString("name"),
                trigger = decodeTrigger(o.getJSONObject("trigger")),
                window = o.optJSONObject("window")?.let { decodeTrigger(it) as? Trigger.Between },
                groups = o.getJSONArray("groups").let { a -> (0 until a.length()).mapNotNull { runCatching { AppGroup.valueOf(a.getString(it)) }.getOrNull() }.toSet() },
                mode = runCatching { PhaseMode.valueOf(o.getString("mode")) }.getOrDefault(PhaseMode.CHOOSER),
                actions = o.optJSONArray("actions")?.let { a ->
                    (0 until a.length()).map { j ->
                        val x = a.getJSONObject(j)
                        val config = x.optJSONObject("config")?.let { c -> c.keys().asSequence().associateWith { c.getString(it) } }.orEmpty()
                        RoutineAction(x.getString("id"), x.getString("type"), config, x.optBoolean("enabled", true))
                    }
                }.orEmpty(),
            )
        }.takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun encodeTrigger(t: Trigger): JSONObject = when (t) {
        is Trigger.Between -> JSONObject().put("type", "between").put("start", t.start.toString()).put("end", t.end.toString())
        is Trigger.Arrive -> JSONObject().put("type", "arrive").put("place", t.placeId)
        is Trigger.Leave -> JSONObject().put("type", "leave").put("place", t.placeId)
    }

    private fun decodeTrigger(o: JSONObject): Trigger = when (o.getString("type")) {
        "arrive" -> Trigger.Arrive(o.getString("place"))
        "leave" -> Trigger.Leave(o.getString("place"))
        else -> Trigger.Between(LocalTime.parse(o.getString("start")), LocalTime.parse(o.getString("end")))
    }
}

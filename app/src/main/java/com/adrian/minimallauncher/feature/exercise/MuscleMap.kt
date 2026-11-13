package com.adrian.minimallauncher.feature.exercise

import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.finance.FinanceBackHeader
import com.adrian.minimallauncher.feature.finance.FinanceMuted
import com.adrian.minimallauncher.feature.finance.tinted
import android.graphics.Region
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.feature.finance.FinanceWhite
import com.adrian.minimallauncher.feature.finance.LabelCream
import com.adrian.minimallauncher.feature.finance.SectionLabel
import com.adrian.minimallauncher.feature.finance.TextBlue

/** How much a movement works a muscle. */
internal const val PRIMARY = 2
internal const val HELPS = 1

internal val MusclePrimary = TextBlue
internal val MuscleHelps = Color(0xFF2E6A99)
internal val MuscleRest = Color(0xFF4A5263)
private val BodyDecor = Color(0xFF3A414D)

/** The muscles the map knows, by key, with the name the views show. */
internal val MuscleNames = linkedMapOf(
    "pecho" to "Pecho", "hombros" to "Hombros", "biceps" to "Bíceps", "triceps" to "Tríceps", "antebrazo" to "Antebrazo",
    "abdomen" to "Abdomen", "oblicuos" to "Oblicuos", "trapecio" to "Trapecio", "dorsales" to "Dorsales", "lumbar" to "Lumbar",
    "gluteos" to "Glúteos", "cuadriceps" to "Cuádriceps", "aductores" to "Aductores", "isquios" to "Isquiotibiales",
    "pantorrillas" to "Pantorrillas",
    "abdomen_superior" to "Abdomen superior", "abdomen_inferior" to "Abdomen inferior",
    "pecho_superior" to "Pectoral superior", "pecho_medio" to "Pectoral esternocostal",
    "deltoide_anterior" to "Deltoides anterior", "deltoide_lateral" to "Deltoides lateral",
    "deltoide_posterior" to "Deltoides posterior", "serrato" to "Serrato anterior",
    "espalda_media" to "Espalda media", "gluteo_mayor" to "Glúteo mayor", "gluteo_medio" to "Glúteo medio",
    "recto_femoral" to "Recto femoral", "vasto_lateral" to "Vasto lateral", "vasto_medial" to "Vasto medial",
    "gemelo" to "Gemelos", "soleo" to "Sóleo", "tibial" to "Tibial anterior",
)

/** Expand legacy groups without changing saved routine IDs or storage. */
private val MuscleRegions = mapOf(
    "abdomen" to listOf("abdomen_superior", "abdomen_inferior"),
    "pecho" to listOf("pecho_superior", "pecho_medio"),
    "hombros" to listOf("deltoide_anterior", "deltoide_lateral", "deltoide_posterior"),
    "gluteos" to listOf("gluteo_mayor", "gluteo_medio"),
    "cuadriceps" to listOf("recto_femoral", "vasto_lateral", "vasto_medial"),
    "pantorrillas" to listOf("gemelo", "soleo"),
)

private fun regionalMuscles(source: Map<String, Int>): Map<String, Int> = buildMap {
    source.forEach { (key, level) ->
        (MuscleRegions[key] ?: listOf(key)).forEach { put(it, maxOf(get(it) ?: 0, level)) }
    }
}

/** What a movement of a muscle group usually works, when the movement itself is not known. */
private val GroupMuscles: Map<String, Map<String, Int>> = mapOf(
    "pecho" to mapOf("pecho" to PRIMARY, "triceps" to HELPS, "hombros" to HELPS),
    "espalda" to mapOf("dorsales" to PRIMARY, "biceps" to HELPS, "trapecio" to HELPS),
    "piernas" to mapOf("cuadriceps" to PRIMARY, "gluteos" to PRIMARY, "isquios" to HELPS, "pantorrillas" to HELPS),
    "hombros" to mapOf("hombros" to PRIMARY, "triceps" to HELPS, "trapecio" to HELPS),
    "brazos" to mapOf("biceps" to PRIMARY, "triceps" to PRIMARY, "antebrazo" to HELPS),
    "core" to mapOf("abdomen" to PRIMARY, "oblicuos" to HELPS),
    "cardio" to mapOf("cuadriceps" to HELPS, "isquios" to HELPS, "gluteos" to HELPS, "pantorrillas" to HELPS),
)

/** A movement the user can add to a routine: what it is called, its group and what it works. */
internal data class CatalogExercise(
    val id: String,
    val name: String,
    val group: String,
    val muscles: Map<String, Int>,
    val timed: Boolean = false,
    val sets: Int = 3,
    val reps: Int = 10,
    val how: String = "",
)

private fun m(vararg pairs: Pair<String, Int>) = mapOf(*pairs)

/** The movements the add screen offers: the guide's, plus common ones for dumbbells, body weight and cardio. */
internal val ExerciseCatalog: List<CatalogExercise> = listOf(
    CatalogExercise("push_up", "Flexiones", "pecho", m("pecho" to 2, "triceps" to 1, "hombros" to 1, "abdomen" to 1)),
    CatalogExercise("floor_press", "Press de pecho en el suelo", "pecho", m("pecho" to 2, "triceps" to 1, "hombros" to 1)),
    CatalogExercise("db_fly", "Aperturas con mancuerna", "pecho", m("pecho" to 2, "hombros" to 1)),
    CatalogExercise("db_row", "Remo con mancuerna", "espalda", m("dorsales" to 2, "biceps" to 1, "trapecio" to 1)),
    CatalogExercise("pull_up", "Dominadas", "espalda", m("dorsales" to 2, "biceps" to 1, "antebrazo" to 1), sets = 3, reps = 6),
    CatalogExercise("superman", "Superman", "espalda", m("lumbar" to 2, "gluteos" to 1), reps = 12),
    CatalogExercise("shrug", "Encogimientos", "espalda", m("trapecio" to 2), reps = 15),
    CatalogExercise("shoulder_press", "Press de hombros", "hombros", m("hombros" to 2, "triceps" to 1, "trapecio" to 1)),
    CatalogExercise("lateral_raise", "Elevaciones laterales", "hombros", m("hombros" to 2, "trapecio" to 1), reps = 15),
    CatalogExercise("biceps_curl", "Curl de bíceps", "brazos", m("biceps" to 2, "antebrazo" to 1), sets = 2, reps = 12),
    CatalogExercise("hammer_curl", "Curl martillo", "brazos", m("biceps" to 2, "antebrazo" to 2), sets = 2, reps = 12),
    CatalogExercise("triceps_ext", "Extensión de tríceps", "brazos", m("triceps" to 2), sets = 2, reps = 12),
    CatalogExercise("bench_dip", "Fondos en banco", "brazos", m("triceps" to 2, "pecho" to 1, "hombros" to 1), sets = 2, reps = 12),
    CatalogExercise("goblet_squat", "Sentadilla goblet", "piernas", m("cuadriceps" to 2, "gluteos" to 2, "aductores" to 1, "abdomen" to 1), reps = 12),
    CatalogExercise("romanian_deadlift", "Peso muerto rumano", "piernas", m("isquios" to 2, "gluteos" to 2, "lumbar" to 1), reps = 12),
    CatalogExercise("static_lunge", "Zancada estática", "piernas", m("cuadriceps" to 2, "gluteos" to 1, "aductores" to 1), sets = 2),
    CatalogExercise("reverse_lunge", "Zancada hacia atrás", "piernas", m("cuadriceps" to 2, "gluteos" to 2, "isquios" to 1), sets = 2),
    CatalogExercise("glute_bridge", "Puente de glúteo", "piernas", m("gluteos" to 2, "isquios" to 1), reps = 15),
    CatalogExercise("calf_raise", "Elevación de talón", "piernas", m("pantorrillas" to 2), sets = 2, reps = 15),
    CatalogExercise("dead_bug", "Dead bug", "core", m("abdomen" to 2, "oblicuos" to 1), sets = 2),
    CatalogExercise("reverse_crunch", "Crunch inverso", "core", m("abdomen" to 2, "oblicuos" to 1), sets = 2, reps = 12),
    CatalogExercise("plank", "Plancha", "core", m("abdomen" to 2, "oblicuos" to 1, "hombros" to 1), timed = true, sets = 2, reps = 30),
    CatalogExercise("side_plank", "Plancha lateral", "core", m("oblicuos" to 2, "abdomen" to 1, "hombros" to 1), timed = true, sets = 2, reps = 30),
    CatalogExercise("bike", "Bici", "cardio", m("cuadriceps" to 1, "isquios" to 1, "pantorrillas" to 1, "gluteos" to 1), timed = true, sets = 1, reps = 20),
    CatalogExercise("walk", "Caminata rápida", "cardio", m("pantorrillas" to 1, "cuadriceps" to 1, "gluteos" to 1), timed = true, sets = 1, reps = 25),
    CatalogExercise("run", "Trote", "cardio", m("cuadriceps" to 1, "isquios" to 1, "pantorrillas" to 2, "gluteos" to 1), timed = true, sets = 1, reps = 20),
)

private val CatalogById = ExerciseCatalog.associateBy { it.id }

/** What a movement works: the catalog's when it is known, else what its group usually works. */
internal fun musclesOf(id: String, group: String): Map<String, Int> {
    val muscles = regionalMuscles(CatalogById[id]?.muscles ?: GroupMuscles[group] ?: emptyMap()).toMutableMap()
    // These are qualitative movement emphases, not isolated activation measurements.
    when (id) {
        "push_up", "floor_press", "db_fly", "bench_dip", "plank", "side_plank" -> {
            muscles.remove("deltoide_lateral")
            muscles.remove("deltoide_posterior")
        }
        "shoulder_press" -> { muscles.remove("deltoide_posterior"); muscles["deltoide_lateral"] = HELPS }
        "lateral_raise" -> { muscles.remove("deltoide_anterior"); muscles.remove("deltoide_posterior") }
        "db_row" -> { muscles["espalda_media"] = PRIMARY; muscles["deltoide_posterior"] = HELPS }
        "reverse_crunch" -> muscles["abdomen_superior"] = HELPS
        "glute_bridge", "romanian_deadlift", "superman" -> muscles["gluteo_medio"] = HELPS
    }
    return muscles
}

internal fun PlanExercise.muscles(): Map<String, Int> = musclesOf(id, group())
internal fun RoutineItem.muscles(): Map<String, Int> = musclesOf(id, group)

/** Several movements as one map: each muscle at the most any of them works it. */
internal fun heatOf(all: List<Map<String, Int>>): Map<String, Int> {
    val heat = mutableMapOf<String, Int>()
    all.forEach { muscles -> muscles.forEach { (k, v) -> heat[k] = maxOf(heat[k] ?: 0, v) } }
    return heat
}

/** "Pecho · Tríceps": the names of the muscles worked at [level]. */
internal fun namesAt(muscles: Map<String, Int>, level: Int): String =
    muscles.filterValues { it == level }.keys.mapNotNull { MuscleNames[it] }.joinToString(" · ")

private class ParsedPart(val muscle: String?, val path: Path, val region: Region)

private fun parse(parts: List<MusclePart>): List<ParsedPart> = parts.map { part ->
    val path = PathParser().parsePathString(part.path).toPath().asAndroidPath().let { original ->
        android.graphics.Path(original).apply {
            if (part.mirrored) transform(Matrix().apply { setScale(-1f, 1f); postTranslate(100f, 0f) })
        }.asComposePath()
    }
    // A region at ten times the box, to tell which muscle a tap fell on.
    val big = android.graphics.Path(path.asAndroidPath()).apply { transform(Matrix().apply { setScale(10f, 10f) }) }
    ParsedPart(part.muscle, path, Region().apply { setPath(big, Region(0, 0, 1000, 2000)) })
}

/**
 * One side of the body, as tall as [height]: each muscle in its color by [heat] (light blue works most, dark blue
 * helps, gray rests), the [selected] one outlined in white. A tap on a muscle calls [onPick] with its key.
 */
@Composable
internal fun MuscleBody(front: Boolean, heat: Map<String, Int>, height: Dp, selected: String? = null, onPick: ((String) -> Unit)? = null) {
    val parts = remember(front) { parse(if (front) BodyShapes.front else BodyShapes.back) }
    val currentOnPick by rememberUpdatedState(onPick)
    val worked = heat.filterValues { it > 0 }.keys.mapNotNull { MuscleNames[it] }.joinToString(", ")
    Canvas(
        Modifier
            // Exactly half as wide as tall, whatever room the parent offers, so it never spills onto its label.
            .size(width = height / 2, height = height)
            .clipToBounds()
            .semantics { contentDescription = (if (front) "Frente" else "Espalda") + if (worked.isNotEmpty()) ": $worked" else "" }
            .then(
                if (onPick == null) Modifier else Modifier.pointerInput(parts) {
                    detectTapGestures { at ->
                        val k = minOf(size.width / 100f, size.height / 200f)
                        val x = ((at.x - (size.width - 100f * k) / 2f) / k * 10).toInt()
                        val y = ((at.y - (size.height - 200f * k) / 2f) / k * 10).toInt()
                        parts.lastOrNull { it.muscle != null && it.region.contains(x, y) }?.muscle?.let { currentOnPick?.invoke(it) }
                    }
                },
            ),
    ) {
        val k = minOf(size.width / 100f, size.height / 200f)
        translate((size.width - 100f * k) / 2f, (size.height - 200f * k) / 2f) {
        scale(k, k, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            parts.forEach { part ->
                val level = part.muscle?.let { heat[it] } ?: 0
                val fill = when {
                    part.muscle == null -> BodyDecor
                    level >= PRIMARY -> MusclePrimary
                    level == HELPS -> MuscleHelps
                    else -> MuscleRest
                }
                drawPath(part.path, fill)
                drawPath(part.path, Color.Black, style = Stroke(0.55f))
            }
            parts.filter { it.muscle != null && it.muscle == selected }.forEach { drawPath(it.path, FinanceWhite, style = Stroke(1.4f)) }
        }
        }
    }
}

/** Front and back side by side, each [height] tall, with their names under them when [labels]. */
@Composable
internal fun MuscleMap(heat: Map<String, Int>, height: Dp, selected: String? = null, labels: Boolean = true, onPick: ((String) -> Unit)? = null) {
    BoxWithConstraints(Modifier.widthIn(max = height + height / 14)) {
    val fittedHeight = minOf(height, maxWidth * 14 / 15)
    Row(horizontalArrangement = Arrangement.spacedBy(fittedHeight / 14), verticalAlignment = Alignment.Top) {
        listOf(true, false).forEach { front ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MuscleBody(front, heat, fittedHeight, selected, onPick)
                if (labels) SectionLabel(if (front) "Frente" else "Espalda")
            }
        }
    }
    }
}

/** The map's three colors with their meaning. */
@Composable
internal fun MuscleLegend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(MusclePrimary to "Principal", MuscleHelps to "Ayuda", MuscleRest to "Descansa").forEach { (color, label) ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (color == MuscleRest) LabelCream else FinanceWhite)
            }
        }
    }
}

/** A movement as the big map lists it. */
internal data class MapMove(val name: String, val ask: String, val muscles: Map<String, Int>)

private class MapLine(val name: String, val detail: String, val level: Int, val onClick: () -> Unit)

/**
 * The muscle map in big, for planning or during the routine, in reading order:
 * 1. The movements as chips: the whole routine or one of them paints what it moves.
 * 2. Front and back, filling the width; a tap on a muscle outlines it.
 * 3. A card: the muscle's movements (each one opens itself), the movement's muscles, or what the routine works most.
 */
@Composable
internal fun MuscleMapScreen(title: String, moves: List<MapMove>, start: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    var ex by rememberSaveable { mutableIntStateOf(start) }
    var sel by rememberSaveable { mutableStateOf<String?>(null) }
    val heat = heatOf(if (ex in moves.indices) listOf(moves[ex].muscles) else moves.map { it.muscles })
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Two figures, each half as wide as tall, filling the width.
        val figure = ((maxWidth - 64.dp) / 2 * 2).coerceAtMost(420.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) { FinanceBackHeader(title, onBack) }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf(-1 to "Toda la rutina") + moves.mapIndexed { i, m -> i to m.name }).forEach { (i, name) ->
                    val on = i == ex
                    Text(
                        name,
                        Modifier
                            .border(1.dp, if (on) LauncherAccent else Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                            .clickable(role = Role.RadioButton) { ex = i; sel = null }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge, color = if (on) FinanceWhite else LabelCream, maxLines = 1,
                    )
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MuscleMap(heat, height = figure, selected = sel, labels = true) { k -> sel = if (sel == k) null else k }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MuscleLegend() }
            MapPanel(moves, ex, sel, onPickMove = { ex = it; sel = null }, onPickMuscle = { sel = it })
        }
    }
}

@Composable
private fun MapPanel(moves: List<MapMove>, ex: Int, sel: String?, onPickMove: (Int) -> Unit, onPickMuscle: (String) -> Unit) {
    val title: String
    val sub: String
    val lines: List<MapLine>
    val frame: Color
    when {
        sel != null -> {
            lines = moves.mapIndexedNotNull { i, m ->
                m.muscles[sel]?.takeIf { it > 0 }?.let { MapLine(m.name, if (it >= PRIMARY) "Principal" else "Ayuda", it) { onPickMove(i) } }
            }
            title = MuscleNames[sel] ?: sel
            sub = if (lines.isEmpty()) "Descansa" else if (lines.size == 1) "1 ejercicio" else "${lines.size} ejercicios"
            frame = FinanceWhite
        }
        ex in moves.indices -> {
            val m = moves[ex]
            lines = m.muscles.entries.sortedByDescending { it.value }.map { (k, v) -> MapLine(MuscleNames[k] ?: k, if (v >= PRIMARY) "Principal" else "Ayuda", v) { onPickMuscle(k) } }
            title = m.name
            sub = m.ask
            frame = TextBlue
        }
        else -> {
            val counts = mutableMapOf<String, Int>()
            moves.forEach { m -> m.muscles.forEach { (k, v) -> if (v >= PRIMARY) counts[k] = (counts[k] ?: 0) + 1 } }
            lines = counts.entries.sortedByDescending { it.value }.take(4).map { (k, n) -> MapLine(MuscleNames[k] ?: k, if (n == 1) "1 ejercicio" else "$n ejercicios", PRIMARY) { onPickMuscle(k) } }
            title = "Lo que más trabajas"
            sub = "esta rutina"
            frame = Color.White.copy(alpha = 0.3f)
        }
    }
    Column(
        Modifier.padding(horizontal = 24.dp).fillMaxWidth().tinted(frame, radius = 20.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = FinanceWhite)
            Text(sub, Modifier.padding(bottom = 2.dp), style = MaterialTheme.typography.bodySmall, color = LabelCream)
        }
        lines.forEach { line ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = line.onClick).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).background(if (line.level >= PRIMARY) MusclePrimary else MuscleHelps, CircleShape))
                Text(line.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = FinanceWhite)
                Text(line.detail, style = MaterialTheme.typography.bodySmall, color = LabelCream)
            }
        }
        Text(
            "El color indica el énfasis estimado del movimiento. Las zonas del abdomen trabajan juntas; no son músculos aislados.",
            Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = FinanceMuted,
        )
        Text(
            when {
                sel != null -> "Toca un ejercicio para ver todo lo que mueve."
                ex >= 0 -> "Toca un músculo para ver con qué más lo trabajas."
                else -> "Toca un músculo del cuerpo o un ejercicio de arriba."
            },
            Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = FinanceMuted,
        )
    }
}

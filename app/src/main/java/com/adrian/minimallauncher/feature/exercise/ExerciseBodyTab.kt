package com.adrian.minimallauncher.feature.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.BodyMetricRecord
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.finance.ActionColor
import com.adrian.minimallauncher.feature.finance.FillBar
import com.adrian.minimallauncher.feature.finance.FinIcon
import com.adrian.minimallauncher.feature.finance.FinanceMuted
import com.adrian.minimallauncher.feature.finance.FinanceWhite
import com.adrian.minimallauncher.feature.finance.GlassFill
import com.adrian.minimallauncher.feature.finance.InfoIcon
import com.adrian.minimallauncher.feature.finance.LabelCream
import com.adrian.minimallauncher.feature.finance.MoneyLow
import com.adrian.minimallauncher.feature.finance.MoneyPlenty
import com.adrian.minimallauncher.feature.finance.Pop
import com.adrian.minimallauncher.feature.finance.PopRow
import com.adrian.minimallauncher.feature.finance.SectionHead
import com.adrian.minimallauncher.feature.finance.SectionLabel
import com.adrian.minimallauncher.feature.finance.TextBlue
import com.adrian.minimallauncher.feature.finance.glass
import com.adrian.minimallauncher.feature.finance.tinted
import com.adrian.minimallauncher.ui.components.LocalReactions
import java.time.LocalDate

/** The measurements Cuerpo follows: type stored in body metrics, and its name. */
private val Measurements = listOf("WAIST" to "Cintura", "CHEST" to "Pecho", "ARM" to "Brazo", "THIGH" to "Muslo")

/**
 * Cuerpo, in reading order:
 * 1. Peso: big, with how much it changed since the first record (a record, not a goal); + logs a new one.
 * 2. IMC on its scale, in a card.
 * 3. Medidas, as tinted cards; a tap logs one.
 */
@Composable
internal fun BodyTab(today: LocalDate, settings: ModuleSettings, metrics: List<BodyMetricRecord>, onAdd: (String, Float) -> Unit) {
    var entering by rememberSaveable { mutableStateOf<String?>(null) }
    val reactions = LocalReactions.current
    val byType = metrics.groupBy { it.type }.mapValues { (_, list) -> list.sortedBy { it.createdAt }.map { it.value } }
    val weights = byType["WEIGHT"].orEmpty()
    val weight = weights.lastOrNull()
    val goal = settings.weightGoalKg.takeIf { it > 0f }
    val heightM = settings.heightCm.takeIf { it > 0f }?.div(100f)
    val save = { type: String, value: Float -> onAdd(type, value); reactions.tick(); entering = null }

    // 1. Peso
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Peso")
            InfoIcon("Tu último peso y cuánto cambió desde tu primer registro. Es solo un registro de tu progreso; la meta de Ejercicio es la constancia.")
            Box(Modifier.weight(1f))
            FinIcon("nueva", "Anotar peso", ActionColor.Add) { entering = "WEIGHT" }
        }
        Pop(description = "Ver detalle del peso", pop = {
            weights.firstOrNull()?.let { PopRow("Empezaste", "${kgText(it)} kg") }
            weight?.let { PopRow("Ahora", "${kgText(it)} kg", TextBlue) }
            if (weights.isEmpty()) PopRow("Sin registros", "")
        }) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(weight?.let(::kgText) ?: "—", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 54.sp, lineHeight = 60.sp), color = FinanceWhite)
                Text("kg", Modifier.padding(bottom = 10.dp), style = MaterialTheme.typography.titleMedium, color = LabelCream)
                Box(Modifier.weight(1f))
                Trend(weights, null, "kg")
            }
        }
        val first = metrics.filter { it.type == "WEIGHT" }.minByOrNull { it.createdAt }
        if (weight != null && first != null && weights.size >= 2) {
            val change = weight - first.value
            val since = java.time.Instant.ofEpochMilli(first.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            Text(
                (if (change > 0) "+" else if (change < 0) "−" else "") + "%.1f kg desde el ${com.adrian.minimallauncher.feature.shortDay(since)}".format(kotlin.math.abs(change)),
                style = MaterialTheme.typography.titleMedium, color = FinanceWhite,
            )
        }
    }
    if (entering == "WEIGHT") Entry("Peso en kg", 30f..250f, { save("WEIGHT", it) }) { entering = null }

    // 2. IMC
    val bmi = if (weight != null && heightM != null) weight / (heightM * heightM) else null
    val goalBmi = if (goal != null && heightM != null) goal / (heightM * heightM) else null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHead("IMC", "Índice de masa corporal: tu peso entre tu estatura al cuadrado. La franja clara es el rango saludable (18.5 a 25); la marca delgada es el IMC de tu meta.")
        Column(Modifier.fillMaxWidth().glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (bmi == null) {
                Text(if (heightM == null) "Agrega tu estatura en el engrane" else "Anota tu peso", style = MaterialTheme.typography.bodyMedium, color = FinanceWhite)
            } else {
                val (label, color) = when {
                    bmi < 18.5f -> "Bajo" to MoneyLow
                    bmi < 25f -> "Saludable" to MoneyPlenty
                    bmi < 30f -> "Alto" to MoneyLow
                    else -> "Muy alto" to LauncherDanger
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("%.1f".format(bmi), style = MaterialTheme.typography.headlineMedium, color = FinanceWhite)
                    Text(label, Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.titleMedium, color = color)
                }
                fun x(v: Float) = ((v.coerceIn(15f, 35f) - 15f) / 20f)
                Scale(x(18.5f) to x(25f), listOfNotNull(goalBmi?.let { x(it) to LabelCream }, x(bmi) to color))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    listOf("15", "18.5", "25", "30", "35").forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = FinanceMuted) }
                }
            }
        }
    }

    // 3. Medidas
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHead("Medidas", "En centímetros. Toca una para anotarla. La meta de cintura es la mitad de tu estatura.")
        Measurements.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                pair.forEach { (type, name) ->
                    val values = byType[type].orEmpty()
                    Column(
                        Modifier.weight(1f).tinted(TextBlue).clickable(role = Role.Button) { entering = type }
                            .semantics(mergeDescendants = true) { contentDescription = "Anotar $name" }.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(name, style = MaterialTheme.typography.labelLarge, color = TextBlue)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(values.lastOrNull()?.let { "%.0f".format(it) } ?: "—", style = MaterialTheme.typography.titleLarge, color = FinanceWhite)
                            Text("cm", Modifier.padding(bottom = 3.dp), style = MaterialTheme.typography.bodySmall, color = LabelCream)
                            Box(Modifier.weight(1f))
                            Trend(values, if (type == "WAIST" && settings.heightCm > 0f) settings.heightCm / 2f else null, "")
                        }
                        if (type == "WAIST" && settings.heightCm > 0f) Text("Meta ${"%.0f".format(settings.heightCm / 2f)}", style = MaterialTheme.typography.bodySmall, color = FinanceMuted)
                    }
                }
            }
        }
        entering?.takeIf { it != "WEIGHT" }?.let { type ->
            Entry("${Measurements.first { it.first == type }.second} en cm", 10f..200f, { save(type, it) }) { entering = null }
        }
    }
}

/** A number to log: the field, ✓ saves (blue when the number is valid), ✕ closes. */
@Composable
private fun Entry(hint: String, valid: ClosedFloatingPointRange<Float>, onSave: (Float) -> Unit, onCancel: () -> Unit) {
    var text by rememberSaveable(hint) { mutableStateOf("") }
    val value = text.replace(',', '.').toFloatOrNull()?.takeIf { it in valid }
    Row(Modifier.fillMaxWidth().glass().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ModuleTextField(
            text, { text = it.take(5) }, hint, Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { value?.let(onSave) }),
        )
        FinIcon("listo", "Guardar", if (value != null) ActionColor.Add else FinanceMuted) { value?.let(onSave) }
        FinIcon("no", "Cancelar", onClick = onCancel)
    }
}

/** How the last change went: toward the goal in green, away in pink; with no goal, plain. */
@Composable
private fun Trend(values: List<Float>, goal: Float?, unit: String) {
    if (values.size < 2) return
    val (previous, current) = values.takeLast(2)
    val delta = current - previous
    if (kotlin.math.abs(delta) < 0.05f) return
    val better = goal?.let { kotlin.math.abs(current - it) < kotlin.math.abs(previous - it) }
    val color = when (better) {
        true -> MoneyPlenty
        false -> LauncherDanger
        null -> FinanceWhite
    }
    Text((if (delta > 0) "↑ " else "↓ ") + "%.1f".format(kotlin.math.abs(delta)) + if (unit.isNotEmpty()) " $unit" else "", style = MaterialTheme.typography.labelLarge, color = color)
}

/** A glass track with the [band] (from, to) lighter and [marks] (at, color) as ticks; fractions of the width. */
@Composable
private fun Scale(band: Pair<Float, Float>, marks: List<Pair<Float, Color>>) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(20.dp)) {
        val w = maxWidth
        Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(6.dp).background(GlassFill, RoundedCornerShape(3.dp)))
        Box(Modifier.align(Alignment.CenterStart).offset(x = w * band.first).width(w * (band.second - band.first)).height(6.dp).background(MoneyPlenty.copy(alpha = 0.35f), RoundedCornerShape(3.dp)))
        marks.forEach { (at, color) ->
            Box(Modifier.align(Alignment.CenterStart).offset(x = (w * at - 1.5.dp).coerceAtLeast(0.dp)).width(3.dp).height(20.dp).background(color, RoundedCornerShape(1.5.dp)))
        }
    }
}

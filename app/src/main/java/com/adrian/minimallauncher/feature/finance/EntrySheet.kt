package com.adrian.minimallauncher.feature.finance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.math.BigDecimal
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")

/** What the + records; an income then asks whether it is for stability. */
private enum class Entry(val icon: String, val label: String) {
    EXTRA("extra", "EXTRA"), SHARED("compartido", "COMPARTIDO"), FIXED("fijos", "FIJO"), INCOME("ingreso", "INGRESO")
}

private val APART = listOf(MoneyKind.TRANSFER to "TRANSFER.", MoneyKind.GIFT to "REGALO", MoneyKind.SERVICE to "SERVICIO")

internal fun parseCents(text: String): Long? =
    if (text.isBlank()) null else runCatching { BigDecimal(text.replace(',', '.')).movePointRight(2).longValueExact() }.getOrNull()

/**
 * The register sheet (an entry mode, so it has short captions): kind, amount, an optional concept, the category
 * for extras and shared bills (with what the others owe), and for an income the stability question: yes goes into
 * the budget, no asks whether it was a transfer, a gift or a service. Spending more than is left today shows the
 * warning before saving.
 */
@Composable
internal fun EntrySheet(
    summary: FinanceSummary,
    onClose: () -> Unit,
    onSave: (kind: MoneyKind, cents: Long, description: String, category: String, pendingFromOthers: Long) -> Unit,
) {
    var entry by rememberSaveable { mutableStateOf(Entry.EXTRA) }
    var amount by rememberSaveable { mutableStateOf("") }
    var concept by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(BASE_CATEGORIES.first()) }
    var others by rememberSaveable { mutableStateOf("") }
    var stable by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var apart by rememberSaveable { mutableStateOf<MoneyKind?>(null) }
    val amountFocus = remember { FocusRequester() }
    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) { amountFocus.requestFocus() }

    val cents = parseCents(amount)
    val spending = entry == Entry.EXTRA || entry == Entry.SHARED
    val categories = (BASE_CATEGORIES + summary.topics.map { it.topic.lowercase(spanish) }).distinct()
    val save = save@{
        val value = cents?.takeIf { it > 0 } ?: return@save
        val kind = when (entry) {
            Entry.EXTRA -> MoneyKind.EXTRA
            Entry.SHARED -> MoneyKind.SHARED
            Entry.FIXED -> MoneyKind.FIXED
            Entry.INCOME -> when (stable) {
                true -> MoneyKind.INCOME_FIXED
                false -> apart ?: return@save
                null -> return@save
            }
        }
        val owed = if (kind == MoneyKind.SHARED) (parseCents(others) ?: 0L).coerceIn(0, value) else 0L
        val description = concept.trim().ifBlank { if (spending) category else entry.label.lowercase(spanish) }
        onSave(kind, value, description, if (spending) category else "", owed)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(CardFill, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(1.dp, CardLine, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clickable(enabled = false) {}
            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            SmallLabel("REGISTRAR", color = LauncherRed)
            SmallLabel("✕", Modifier.clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "Cerrar" }.padding(8.dp))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Entry.entries.forEach { option ->
                Choice(option.icon, option.label, entry == option) { entry = option }
            }
        }
        ModuleTextField(
            amount, { value -> amount = value.filter { it.isDigit() || it == '.' || it == ',' }.take(10) }, "Monto",
            Modifier.fillMaxWidth().focusRequester(amountFocus),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        )
        ModuleTextField(concept, { concept = it.take(60) }, "Concepto", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
        if (spending) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                categories.forEach { name -> Choice(categoryIcon(name), name.uppercase(spanish), category == name) { category = name } }
            }
        }
        if (entry == Entry.SHARED) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                StrokeIcon("compartido", StrokeState.NOW, 24.dp)
                ModuleTextField(
                    others, { value -> others = value.filter { it.isDigit() || it == '.' || it == ',' }.take(10) }, "Te deben",
                    Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
        if (entry == Entry.INCOME) {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), Alignment.CenterVertically) {
                StrokeIcon("ancla", StrokeState.NOW, 28.dp)
                SmallLabel("¿PARA TU ESTABILIDAD?")
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally)) {
                Choice("listo", "SÍ", stable == true) { stable = true; apart = null }
                Choice("no", "NO", stable == false) { stable = false }
            }
            if (stable == false) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    APART.forEach { (kind, label) -> Choice(kindIcon(kind), label, apart == kind) { apart = kind } }
                }
            }
        }
        val left = summary.topics.firstOrNull { it.topic.equals(category, ignoreCase = true) }?.todayLeftCents ?: summary.todayLeftCents
        val warn = spending && cents != null && cents > 0 && Budget.wouldExceed(summary, category, cents)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (warn) {
                Row(
                    Modifier.weight(1f).border(1.dp, LauncherRed, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 8.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "Pasa lo de hoy: quedaría ${wholePesos(left - cents!!)}" },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StrokeIcon("aviso", StrokeState.NOW, 20.dp)
                    Text(wholePesos(left - cents!!), style = MaterialTheme.typography.labelLarge, color = LauncherRed)
                    Spacer(Modifier.weight(1f))
                    StrokeIcon("bolsa", StrokeState.DONE, 18.dp)
                }
                Spacer(Modifier.size(12.dp))
            } else Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(52.dp)
                    .background(Color(0xFFF5F5F5), CircleShape)
                    .clickable(role = Role.Button) { save() }
                    .semantics { contentDescription = "Guardar" },
                contentAlignment = Alignment.Center,
            ) {
                StrokeIcon("listo", StrokeState.DONE, 24.dp, color = Color.Black)
            }
        }
    }
}

/** An icon with its caption: red detail when picked, drawn once on the tap. */
@Composable
private fun Choice(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    Column(
        Modifier.clickable(role = Role.Button) { taps++; onClick() }.semantics(mergeDescendants = true) { contentDescription = label }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StrokeIcon(icon, if (selected) StrokeState.NOW else StrokeState.OFF, 36.dp, playKey = taps)
        SmallLabel(label, color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

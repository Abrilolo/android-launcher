package com.adrian.minimallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.ForcedState
import com.adrian.minimallauncher.data.ForcedStep
import com.adrian.minimallauncher.ui.theme.LauncherBlack
import com.adrian.minimallauncher.ui.theme.LauncherTheme
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import java.time.LocalDate

/**
 * PLACEHOLDER. Plain controls so the forced flow can be tried on the phone; the real view is being designed
 * apart. Back does nothing here: the flow is left only by saving.
 */
@Composable
fun ForcedEntryScreen(state: ForcedState, viewModel: LauncherViewModel) {
    BackHandler { }
    LauncherTheme {
        Column(
            Modifier.fillMaxSize().background(LauncherBlack).safeDrawingPadding().imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("PLACEHOLDER · REGISTRO PENDIENTE", style = MaterialTheme.typography.labelLarge)
            state.payment?.let { p -> Text("$${"%.2f".format(p.amountCents / 100.0)} · ${p.merchant}", style = MaterialTheme.typography.titleLarge) }
            // Pending payments as dots, never as a count.
            if (state.openPayments > 1) Text("●".repeat(state.openPayments))
            when (state.step) {
                ForcedStep.ASK_FOOD -> {
                    Text("¿Comida?")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button({ viewModel.forcedAnswerFood(true) }) { Text("Sí") }
                        OutlinedButton({ viewModel.forcedAnswerFood(false) }) { Text("No") }
                    }
                }
                ForcedStep.EXPENSE -> {
                    val payment = state.payment ?: return@Column
                    var amount by remember(payment.id) { mutableStateOf("%.2f".format(payment.amountCents / 100.0)) }
                    var concept by remember(payment.id) { mutableStateOf(payment.merchant) }
                    var category by remember(payment.id) { mutableStateOf(if (state.isFood == true) "comida" else "") }
                    OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(concept, { concept = it }, Modifier.fillMaxWidth(), label = { Text("Concepto") })
                    OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Categoría") })
                    val cents = amount.replace(',', '.').toDoubleOrNull()?.let { Math.round(it * 100) }
                    Button({ cents?.let { viewModel.forcedSaveExpense(it, concept, category) } }, enabled = cents != null && cents > 0) { Text("Guardar gasto") }
                }
                ForcedStep.MEAL -> {
                    var meal by remember(state.payment?.id) { mutableStateOf("") }
                    Text("¿Qué comiste?")
                    OutlinedTextField(meal, { meal = it }, Modifier.fillMaxWidth(), label = { Text("Comida") })
                    Button({ viewModel.forcedSaveMeal(meal.trim()) }, enabled = meal.isNotBlank()) { Text("Guardar comida") }
                }
                ForcedStep.ROUTINE_MEALS -> {
                    val meals by viewModel.unmarkedMeals.collectAsState()
                    val habits by viewModel.completedHabits.collectAsState()
                    val date = LocalDate.now()
                    Text("Comidas del día sin marcar")
                    meals.forEach { slot ->
                        val key = "$date|meal_${slot.id}"
                        Row {
                            Checkbox(key in habits, { viewModel.setHabit(key, it) })
                            Text("${slot.name} · ${slot.time}", Modifier.padding(top = 12.dp))
                        }
                    }
                    Button({ viewModel.finishRoutineRegistration() }) { Text("Listo") }
                }
            }
        }
    }
}

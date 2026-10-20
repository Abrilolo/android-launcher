package com.adrian.minimallauncher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MealRecord
import com.adrian.minimallauncher.data.NoteCategory
import com.adrian.minimallauncher.data.NoteRecord
import com.adrian.minimallauncher.data.ReminderRecord
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.ui.components.ExpandableSection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
fun PersonalSections(records: PersonalRecords, actions: PersonalRecordActions, spaceName: String, hiddenSections: Set<String>) {
    var notesExpanded by rememberSaveable { mutableStateOf(false) }
    var remindersExpanded by rememberSaveable { mutableStateOf(true) }
    var financeExpanded by rememberSaveable { mutableStateOf(false) }
    var dietExpanded by rememberSaveable { mutableStateOf(false) }
    var noteCategory by rememberSaveable { mutableStateOf(NoteCategory.GENERAL) }
    var noteText by rememberSaveable { mutableStateOf("") }
    var reminderTitle by rememberSaveable { mutableStateOf("") }
    var reminderTime by rememberSaveable { mutableStateOf("08:00") }
    var financeAmount by rememberSaveable { mutableStateOf("") }
    var financeDescription by rememberSaveable { mutableStateOf("") }
    var mealText by rememberSaveable { mutableStateOf("") }
    val today = LocalDate.now()
    if ("reminders" !in hiddenSections) ExpandableSection("Recordatorios", remindersExpanded, { remindersExpanded = !remindersExpanded }) {
        records.reminders.forEach { reminder ->
            RecordLine("${reminder.title}  ${"%02d:%02d".format(reminder.hour, reminder.minute)}  ·  ${reminder.spaceName}", "×") { actions.deleteReminder(reminder.id) }
        }
        OutlinedTextField(reminderTitle, { reminderTitle = it }, Modifier.fillMaxWidth(), label = { Text("Nuevo recordatorio") }, singleLine = true)
        OutlinedTextField(reminderTime, { reminderTime = it.take(5) }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Hora (HH:mm)") }, singleLine = true)
        TextButton(onClick = {
            val pieces = reminderTime.split(":")
            val hour = pieces.getOrNull(0)?.toIntOrNull()
            val minute = pieces.getOrNull(1)?.toIntOrNull()
            if (reminderTitle.isNotBlank() && hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                actions.addReminder(reminderTitle.trim(), hour, minute, spaceName)
                reminderTitle = ""
            }
        }) { Text("AÑADIR") }
    }
    if ("notes" !in hiddenSections) ExpandableSection("Notas", notesExpanded, { notesExpanded = !notesExpanded }) {
        records.notes.forEach { note -> RecordLine("${note.category.label.uppercase()}  ·  ${note.text}", "×") { actions.deleteNote(note.id) } }
        TextButton(onClick = { noteCategory = noteCategory.next() }) { Text("ETIQUETA: ${noteCategory.label.uppercase()}  ↻") }
        OutlinedTextField(noteText, { noteText = it }, Modifier.fillMaxWidth(), label = { Text("Nueva nota") }, minLines = 2)
        TextButton(onClick = { if (noteText.isNotBlank()) { actions.addNote(noteCategory, noteText.trim()); noteText = "" } }) { Text("GUARDAR NOTA") }
    }
    if ("finance" !in hiddenSections) ExpandableSection("Finanzas", financeExpanded, { financeExpanded = !financeExpanded }) {
        val todayRecords = records.finances.filter { it.createdAt.toLocalDate() == today }
        val totalCents = todayRecords.sumOf { it.amountCents }
        Text("HOY  ${formatMoney(totalCents)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        todayRecords.forEach { item -> RecordLine("${formatMoney(item.amountCents)}  ·  ${item.description}", "×") { actions.deleteFinance(item.id) } }
        OutlinedTextField(financeAmount, { financeAmount = it }, Modifier.fillMaxWidth(), label = { Text("Monto (ej. 125.50)") }, singleLine = true)
        OutlinedTextField(financeDescription, { financeDescription = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Concepto") }, singleLine = true)
        TextButton(onClick = {
            val cents = financeAmount.replace(',', '.').toBigDecimalOrNull()?.movePointRight(2)?.longValueExactOrNull()
            if (cents != null && cents > 0 && financeDescription.isNotBlank()) {
                actions.addFinance(cents, financeDescription.trim()); financeAmount = ""; financeDescription = ""
            }
        }) { Text("REGISTRAR GASTO") }
    }
    if ("diet" !in hiddenSections) ExpandableSection("Dieta", dietExpanded, { dietExpanded = !dietExpanded }) {
        val todayMeals = records.meals.filter { it.createdAt.toLocalDate() == today }
        Text("COMIDAS DE HOY  ${todayMeals.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        todayMeals.forEach { meal -> RecordLine(meal.description, "×") { actions.deleteMeal(meal.id) } }
        OutlinedTextField(mealText, { mealText = it }, Modifier.fillMaxWidth(), label = { Text("¿Qué comiste?") }, singleLine = true)
        TextButton(onClick = { if (mealText.isNotBlank()) { actions.addMeal(mealText.trim()); mealText = "" } }) { Text("REGISTRAR COMIDA") }
    }
}

@Composable
private fun RecordLine(text: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f).padding(vertical = 4.dp), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onAction) { Text(action) }
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
private fun formatMoney(cents: Long): String = "$${"%,.2f".format(Locale.getDefault(), cents / 100.0)}"
private fun java.math.BigDecimal.longValueExactOrNull(): Long? = runCatching { longValueExact() }.getOrNull()

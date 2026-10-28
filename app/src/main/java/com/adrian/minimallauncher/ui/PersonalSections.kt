package com.adrian.minimallauncher.ui

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
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.data.PersonalRecordActions
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Body of a personal section that has no page of its own ("reminders" or "diet"); the header is drawn by the caller. */
@Composable
fun PersonalSectionBody(id: String, records: PersonalRecords, actions: PersonalRecordActions, spaceName: String) {
    val today = LocalDate.now()
    when (id) {
        "reminders" -> {
            var reminderTitle by rememberSaveable { mutableStateOf("") }
            var reminderTime by rememberSaveable { mutableStateOf("08:00") }
            records.reminders.forEach { reminder ->
                RecordLine("${reminder.title}  ${"%02d:%02d".format(reminder.hour, reminder.minute)}  ·  ${reminder.phaseId}", "×") { actions.deleteReminder(reminder.id) }
            }
            OutlinedTextField(reminderTitle, { reminderTitle = it }, Modifier.fillMaxWidth(), placeholder = { Text("Recordatorio") }, singleLine = true)
            OutlinedTextField(reminderTime, { reminderTime = it.take(5) }, Modifier.fillMaxWidth().padding(top = 6.dp), placeholder = { Text("HH:mm") }, singleLine = true)
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
        "diet" -> {
            var mealText by rememberSaveable { mutableStateOf("") }
            val todayMeals = records.meals.filter { it.createdAt.toLocalDate() == today }
            Text("COMIDAS DE HOY  ${todayMeals.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            todayMeals.forEach { meal -> RecordLine(meal.description, "×") { actions.deleteMeal(meal.id) } }
            OutlinedTextField(mealText, { mealText = it }, Modifier.fillMaxWidth(), placeholder = { Text("¿Qué comiste?") }, singleLine = true)
            TextButton(onClick = { if (mealText.isNotBlank()) { actions.addMeal(mealText.trim()); mealText = "" } }) { Text("REGISTRAR") }
        }
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

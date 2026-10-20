package com.adrian.minimallauncher.data
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Base64
import java.util.UUID
import java.time.LocalTime
// Where the widget stack sits in the right column: share of the row width, height and top offset in dp.
data class WidgetPlacement(val widthFraction: Float = 0.45f, val heightDp: Int = 220, val topDp: Int = 0)
private val Context.settings by preferencesDataStore(name = "settings")
class SettingsRepository(private val context: Context) {
    private val widthKey = floatPreferencesKey("widget_width")
    private val heightKey = intPreferencesKey("widget_height")
    private val topKey = intPreferencesKey("widget_top")
    private val notesKey = stringSetPreferencesKey("notes")
    private val remindersKey = stringSetPreferencesKey("reminders")
    private val financesKey = stringSetPreferencesKey("finance_records")
    private val mealsKey = stringSetPreferencesKey("meal_records")
    private val spacesKey = stringPreferencesKey("spaces")
    private val hiddenSectionsKey = stringSetPreferencesKey("hidden_sections")
    private val pauseSecondsKey = intPreferencesKey("pause_seconds")
    val spaces = context.settings.data.map { prefs ->
        prefs[spacesKey]?.split(';')?.mapNotNull { row -> runCatching {
            val p = row.split('~')
            Space(decode(p[0]), LocalTime.parse(p[1]), LocalTime.parse(p[2]), p[3].split(',').filter(String::isNotBlank).map(AppGroup::valueOf).toSet())
        }.getOrNull() }?.takeIf { it.size == Space.all.size } ?: Space.all
    }
    val hiddenSections = context.settings.data.map { it[hiddenSectionsKey].orEmpty() }
    val pauseSeconds = context.settings.data.map { it[pauseSecondsKey] ?: 30 }
    private fun encode(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun decode(value: String) = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
    val notes = context.settings.data.map { prefs -> prefs[notesKey].orEmpty().mapNotNull { row ->
        runCatching { val p = row.split('|'); NoteRecord(p[0], NoteCategory.valueOf(p[1]), decode(p[2]), p[3].toLong()) }.getOrNull()
    }.sortedByDescending { it.createdAt } }
    val reminders = context.settings.data.map { prefs -> prefs[remindersKey].orEmpty().mapNotNull { row ->
        runCatching { val p = row.split('|'); ReminderRecord(p[0], decode(p[1]), p[2].toInt(), p[3].toInt(), decode(p[4])) }.getOrNull()
    }.sortedWith(compareBy({ it.hour }, { it.minute })) }
    val finances = context.settings.data.map { prefs -> prefs[financesKey].orEmpty().mapNotNull { row ->
        runCatching { val p = row.split('|'); FinanceRecord(p[0], p[1].toLong(), decode(p[2]), p[3].toLong()) }.getOrNull()
    }.sortedByDescending { it.createdAt } }
    val meals = context.settings.data.map { prefs -> prefs[mealsKey].orEmpty().mapNotNull { row ->
        runCatching { val p = row.split('|'); MealRecord(p[0], decode(p[1]), p[2].toLong()) }.getOrNull()
    }.sortedByDescending { it.createdAt } }
    val widgetPlacement: Flow<WidgetPlacement> = context.settings.data.map { prefs ->
        val default = WidgetPlacement()
        WidgetPlacement(prefs[widthKey] ?: default.widthFraction, prefs[heightKey] ?: default.heightDp, prefs[topKey] ?: default.topDp)
    }
    suspend fun setWidgetPlacement(placement: WidgetPlacement) {
        context.settings.edit {
            it[widthKey] = placement.widthFraction
            it[heightKey] = placement.heightDp
            it[topKey] = placement.topDp
        }
    }
    suspend fun setSpaces(spaces: List<Space>) {
        context.settings.edit { prefs -> prefs[spacesKey] = spaces.joinToString(";") { space ->
            "${encode(space.name)}~${space.startsAt}~${space.endsAt}~${space.availableGroups.joinToString(",") { it.name }}"
        } }
    }
    suspend fun setSectionVisible(section: String, visible: Boolean) {
        context.settings.edit { prefs ->
            val hidden = prefs[hiddenSectionsKey].orEmpty().toMutableSet()
            if (visible) hidden.remove(section) else hidden.add(section)
            prefs[hiddenSectionsKey] = hidden
        }
    }
    suspend fun setPauseSeconds(seconds: Int) { context.settings.edit { it[pauseSecondsKey] = seconds.coerceIn(0, 300) } }
    suspend fun addNote(category: NoteCategory, text: String) {
        context.settings.edit { prefs ->
            val rows = prefs[notesKey].orEmpty().toMutableSet()
            rows += "${UUID.randomUUID()}|${category.name}|${encode(text)}|${System.currentTimeMillis()}"
            prefs[notesKey] = rows
        }
    }
    suspend fun deleteNote(id: String) { context.settings.edit { prefs -> prefs[notesKey] = prefs[notesKey].orEmpty().filterNot { it.substringBefore('|') == id }.toSet() } }
    suspend fun addReminder(title: String, hour: Int, minute: Int, spaceName: String) {
        context.settings.edit { prefs ->
            val rows = prefs[remindersKey].orEmpty().toMutableSet()
            rows += "${UUID.randomUUID()}|${encode(title)}|$hour|$minute|${encode(spaceName)}"
            prefs[remindersKey] = rows
        }
    }
    suspend fun deleteReminder(id: String) { context.settings.edit { prefs -> prefs[remindersKey] = prefs[remindersKey].orEmpty().filterNot { it.substringBefore('|') == id }.toSet() } }
    suspend fun addFinance(amountCents: Long, description: String) {
        context.settings.edit { prefs ->
            val rows = prefs[financesKey].orEmpty().toMutableSet()
            rows += "${UUID.randomUUID()}|$amountCents|${encode(description)}|${System.currentTimeMillis()}"
            prefs[financesKey] = rows
        }
    }
    suspend fun deleteFinance(id: String) { context.settings.edit { prefs -> prefs[financesKey] = prefs[financesKey].orEmpty().filterNot { it.substringBefore('|') == id }.toSet() } }
    suspend fun addMeal(description: String) {
        context.settings.edit { prefs ->
            val rows = prefs[mealsKey].orEmpty().toMutableSet()
            rows += "${UUID.randomUUID()}|${encode(description)}|${System.currentTimeMillis()}"
            prefs[mealsKey] = rows
        }
    }
    suspend fun deleteMeal(id: String) { context.settings.edit { prefs -> prefs[mealsKey] = prefs[mealsKey].orEmpty().filterNot { it.substringBefore('|') == id }.toSet() } }
}

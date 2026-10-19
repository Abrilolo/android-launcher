package com.adrian.minimallauncher.data
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
// Where the widget stack sits in the right column: share of the row width, height and top offset in dp.
data class WidgetPlacement(val widthFraction: Float = 0.45f, val heightDp: Int = 220, val topDp: Int = 0)
private val Context.settings by preferencesDataStore(name = "settings")
class SettingsRepository(private val context: Context) {
    private val widthKey = floatPreferencesKey("widget_width")
    private val heightKey = intPreferencesKey("widget_height")
    private val topKey = intPreferencesKey("widget_top")
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
}

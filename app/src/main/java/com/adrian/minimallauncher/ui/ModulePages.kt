package com.adrian.minimallauncher.ui

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleCard
import com.adrian.minimallauncher.feature.ModuleHeader
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StatusDot
import com.adrian.minimallauncher.ui.components.DotMatrixText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/** Shared frame for the pages a home tile opens: back label, then scrolling content. */
@Composable
fun ModulePage(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ModuleHeader(title, onBack, Modifier.padding(top = 8.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
fun AlarmsPage(now: LocalDateTime, records: PersonalRecords, actions: PersonalRecordActions, spaceName: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val next = nextSystemAlarm()
    ModulePage("ALARMAS", onBack, modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (next != null) Box(Modifier.size(6.dp).background(LauncherRed, CircleShape))
            SmallLabel("SIGUIENTE")
        }
        DotMatrixText(next?.format(hhmm) ?: "--:--", cell = 9.dp)
        SmallLabel(next?.let { untilText(now, it) } ?: "SIN ALARMAS ACTIVAS", color = MaterialTheme.colorScheme.onSurface)
        // Alarms belong to the phone's clock app; the launcher shows the next one and opens the app to edit.
        OutlineButton("ABRIR ALARMAS DEL RELOJ") {
            runCatching { context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        SmallLabel("RECORDATORIOS", Modifier.padding(top = 12.dp))
        PersonalSectionBody("reminders", records, actions, spaceName)
    }
}

@Composable
fun WorldClockPage(now: LocalDateTime, zones: List<String>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val here = ZonedDateTime.now()
    ModulePage("MUNDO", onBack, modifier) {
        SmallLabel("AQUÍ")
        DotMatrixText(now.format(hhmm), cell = 9.dp)
        remoteCities(zones).forEach { city ->
            val time = ZonedDateTime.now(city.zone)
            val diff = (time.offset.totalSeconds - here.offset.totalSeconds) / 3600
            val day = when (time.toLocalDate().compareTo(here.toLocalDate())) {
                0 -> "HOY"
                in 1..Int.MAX_VALUE -> "MAÑANA"
                else -> "AYER"
            }
            ModuleCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(city.name, style = MaterialTheme.typography.bodyLarge)
                        SmallLabel("${if (diff >= 0) "+" else ""}$diff H · $day")
                    }
                    DotMatrixText(time.format(hhmm), cell = 4.dp)
                }
                // One bar per hour of the city's day: white while it is daytime there, red for the current hour.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(24) { hour ->
                        val color = when {
                            hour == time.hour -> LauncherRed
                            hour in 7..19 -> Color(0xFFCFCFCF)
                            else -> Color(0xFF262626)
                        }
                        Box(Modifier.weight(1f).height(5.dp).background(color, RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlineButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(26.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
    )
}

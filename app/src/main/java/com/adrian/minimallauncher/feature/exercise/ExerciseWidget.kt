package com.adrian.minimallauncher.feature.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.DotMatrixText
import java.time.LocalTime

/** Today's session at a glance; a tap opens the full exercise page. */
@Composable
fun ExerciseWidget(day: ExerciseDay, start: LocalTime) {
    val session = day.session
    Column(Modifier.fillMaxSize(), Arrangement.SpaceBetween) {
        SmallLabel("EJERCICIO")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // The status sits next to the big code: the widget column is too narrow for it beside the label.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                DotMatrixText(session.displayCode(), cell = if (session.kind == SessionKind.STRENGTH) 5.dp else 3.dp)
                SmallLabel(
                    when {
                        day.completed -> "HECHO"
                        session.kind == SessionKind.REST -> "LIBRE"
                        else -> start.toString()
                    },
                    color = if (day.completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(session.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
        }
        if (session.kind == SessionKind.STRENGTH) {
            val done = day.doneSets
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(2.dp)) {
                repeat(session.totalSets) { i ->
                    val color = when {
                        i < done -> MaterialTheme.colorScheme.onSurface
                        i == done && !day.completed -> LauncherRed
                        else -> Color(0xFF2A2A2A)
                    }
                    Box(Modifier.weight(1f).height(4.dp).background(color, RoundedCornerShape(2.dp)))
                }
            }
        } else {
            SmallLabel(session.duration)
        }
    }
}

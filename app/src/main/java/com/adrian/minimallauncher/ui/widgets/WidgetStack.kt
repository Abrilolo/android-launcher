package com.adrian.minimallauncher.ui.widgets

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.feature.exercise.ExerciseDay
import com.adrian.minimallauncher.feature.exercise.ExerciseWidget
import com.adrian.minimallauncher.feature.vault.VaultWidget
import com.adrian.minimallauncher.ui.components.rememberCurrentMinute
import java.time.LocalDateTime
import java.time.LocalTime

// How much detail a widget shows, derived from the card height.
enum class WidgetSize { SMALL, MEDIUM, LARGE }

val CardShape = RoundedCornerShape(20.dp)

/** Data the module widgets need, plus how to open a module's page ("exercise", "vault"). */
data class HomeWidgets(
    val exercise: ExerciseDay,
    val exerciseStart: LocalTime,
    val vaultAppCount: Int,
    val onOpenModule: (String) -> Unit,
)

private class StackEntry(val module: String?, val content: @Composable (LocalDateTime, WidgetSize) -> Unit)

@Composable
fun WidgetStack(
    records: PersonalRecords,
    home: HomeWidgets,
    editing: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = rememberCurrentMinute()

    val entries = listOf(
        StackEntry("exercise") { _, _ -> ExerciseWidget(home.exercise, home.exerciseStart) },
        StackEntry("vault") { _, _ -> VaultWidget(home.vaultAppCount) },
        StackEntry(null) { _, size -> ExpensesWidget(size, records.finances) },
        StackEntry(null) { n, size -> DietMealsWidget(n, size, records.meals) },
        StackEntry(null) { _, size -> WorldClockWidget(size) },
        StackEntry(null) { _, size -> AlarmWidget(size) },
        StackEntry(null) { n, size -> DayProgressWidget(n, size) },
        StackEntry(null) { n, size -> NextReminderWidget(n, size, records.reminders) },
    )

    val pagerState = rememberPagerState(pageCount = { entries.size })
    // The gesture detector outlives recompositions, so it reads the latest values through these.
    val currentEntries by rememberUpdatedState(entries)
    val currentHome by rememberUpdatedState(home)
    val currentEditing by rememberUpdatedState(editing)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, if (editing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline, CardShape),
        ) {
            val size = when {
                maxHeight < 160.dp -> WidgetSize.SMALL
                maxHeight < 260.dp -> WidgetSize.MEDIUM
                else -> WidgetSize.LARGE
            }
            HorizontalPager(
                pagerState,
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() },
                        onTap = {
                            if (!currentEditing) currentEntries[pagerState.currentPage].module?.let(currentHome.onOpenModule)
                        },
                    )
                },
                userScrollEnabled = !editing,
            ) { page ->
                Box(Modifier.fillMaxSize().padding(14.dp)) { entries[page].content(now, size) }
            }
        }
    }
}

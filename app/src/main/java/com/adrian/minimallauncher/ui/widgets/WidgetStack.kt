package com.adrian.minimallauncher.ui.widgets
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.ui.components.rememberCurrentMinute
import java.time.LocalDateTime
// How much detail a widget shows, derived from the card height.
enum class WidgetSize { SMALL, MEDIUM, LARGE }
private val widgets: List<@Composable (LocalDateTime, WidgetSize) -> Unit> = listOf(
    { now, size -> DayProgressWidget(now, size) },
    { now, size -> NextEventWidget(now, size) },
    { now, size -> BreakfastStreakWidget(now, size) },
    { _, size -> ExpensesWidget(size) },
)
val CardShape = RoundedCornerShape(20.dp)
@Composable
fun WidgetStack(editing: Boolean, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val now = rememberCurrentMinute()
    val pagerState = rememberPagerState(pageCount = { widgets.size })
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
                Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) },
                userScrollEnabled = !editing,
            ) { page ->
                Box(Modifier.fillMaxSize().padding(14.dp)) { widgets[page](now, size) }
            }
        }
        Row(Modifier.padding(top = 10.dp), Arrangement.spacedBy(6.dp)) {
            repeat(widgets.size) { index ->
                val color = if (index == pagerState.currentPage) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                Box(Modifier.size(5.dp).background(color, CircleShape))
            }
        }
    }
}

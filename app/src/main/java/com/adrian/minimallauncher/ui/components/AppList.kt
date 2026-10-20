package com.adrian.minimallauncher.ui.components
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.LauncherApp
import kotlinx.coroutines.launch
private fun LauncherApp.indexLetter(): Char = label.trim().firstOrNull()?.uppercaseChar()?.takeIf(Char::isLetter) ?: '#'
/**
 * Shows at most [visibleRows] apps and scrolls inside that window, so the section
 * headers around it stay in place. With [alphabetIndex], a letter strip on the left
 * jumps to the first app of the letter under the finger while dragging.
 */
@Composable
fun AppList(
    apps: List<LauncherApp>,
    visibleRows: Int,
    onClick: (LauncherApp) -> Unit,
    colorFor: @Composable (LauncherApp) -> Color,
    alphabetIndex: Boolean = false,
    emptyText: String? = null,
) {
    if (apps.isEmpty()) {
        if (emptyText != null) Text(emptyText, Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val listState = rememberLazyListState()
    val height = appItemHeight() * minOf(apps.size, visibleRows)
    Row(Modifier.fillMaxWidth().height(height)) {
        if (alphabetIndex && apps.size > visibleRows) {
            val letters = remember(apps) { apps.map { it.indexLetter() }.distinct() }
            val firstIndex = remember(apps) { letters.associateWith { letter -> apps.indexOfFirst { it.indexLetter() == letter } } }
            val scope = rememberCoroutineScope()
            var selected by remember { mutableStateOf<Char?>(null) }
            LetterStrip(letters, selected) { letter ->
                selected = letter
                if (letter != null) scope.launch { listState.scrollToItem(firstIndex.getValue(letter)) }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxHeight(), listState) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { _, app ->
                AppTextItem(app.label, { onClick(app) }, colorFor(app))
            }
        }
    }
}
@Composable
private fun LetterStrip(letters: List<Char>, selected: Char?, onSelect: (Char?) -> Unit) {
    Column(
        Modifier
            .width(28.dp)
            .fillMaxHeight()
            .pointerInput(letters) {
                fun letterAt(y: Float) = letters[(y / size.height * letters.size).toInt().coerceIn(0, letters.lastIndex)]
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onSelect(letterAt(down.position.y))
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        onSelect(letterAt(change.position.y))
                    }
                    onSelect(null)
                }
            },
        Arrangement.Top,
        Alignment.CenterHorizontally,
    ) {
        // Every letter gets an equal slice so the strip always fits the list window.
        letters.forEach { letter ->
            val active = letter == selected
            Text(
                letter.toString(),
                Modifier.fillMaxWidth().weight(1f).wrapContentHeight(Alignment.CenterVertically).padding(end = 6.dp),
                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

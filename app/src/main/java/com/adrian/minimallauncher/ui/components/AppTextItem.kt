package com.adrian.minimallauncher.ui.components
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
/** Fixed row height so app lists can show an exact number of rows; follows the user's font scale. */
@Composable
fun appItemHeight(): Dp = with(LocalDensity.current) { 28.sp.toDp() } + 16.dp
@Composable
fun AppTextItem(label: String, onClick: () -> Unit, color: Color = MaterialTheme.colorScheme.onSurface, locked: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().height(appItemHeight()).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            Modifier.weight(1f, fill = false),
            color = color,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Out of schedule: a small gray lock (it opens after the pause).
        if (locked) DotIcon(DotIcons.candado, done = false, contentDescription = "Fuera de horario", Modifier.padding(start = 10.dp), size = 16.dp)
    }
}

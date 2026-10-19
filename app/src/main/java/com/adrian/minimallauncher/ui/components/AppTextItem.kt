package com.adrian.minimallauncher.ui.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
@Composable
fun AppTextItem(label: String, onClick: () -> Unit, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        label,
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        color = color,
        style = MaterialTheme.typography.headlineSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

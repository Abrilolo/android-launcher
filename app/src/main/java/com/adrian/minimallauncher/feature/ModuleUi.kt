package com.adrian.minimallauncher.feature

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The single accent: only for "active", "next" or "attention". */
val LauncherRed = Color(0xFFD71921)
val CardFill = Color(0xFF0B0B0B)
val CardLine = Color(0xFF222222)
val ModuleCardShape = RoundedCornerShape(22.dp)

/** Back label plus optional tabs on the right; system back does the same as the label. */
@Composable
fun ModuleHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    BackHandler(onBack = onBack)
    Row(modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "‹  $title",
            Modifier.clickable(role = Role.Button, onClick = onBack).padding(vertical = 12.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.weight(1f), Arrangement.End, Alignment.CenterVertically, trailing)
    }
}

/** A text tab: white when selected, gray otherwise. */
@Composable
fun ModuleTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        Modifier.clickable(role = Role.Tab, onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ModuleCard(modifier: Modifier = Modifier, border: Color = CardLine, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().background(CardFill, ModuleCardShape).border(1.dp, border, ModuleCardShape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun SmallLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines: Int = Int.MAX_VALUE) {
    Text(text, modifier, style = MaterialTheme.typography.labelSmall, color = color, maxLines = maxLines)
}

/** Filled = done, red ring = next, gray ring = pending. */
@Composable
fun StatusDot(filled: Boolean, next: Boolean, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    val ring = when {
        filled -> MaterialTheme.colorScheme.onSurface
        next -> LauncherRed
        else -> Color(0xFF555555)
    }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) MaterialTheme.colorScheme.onSurface else Color.Transparent)
            .border(1.5.dp, ring, CircleShape),
    )
}

/** Dark rounded field with the hint inside, as in the concept; [minLines] above 1 makes it multi-line. */
@Composable
fun ModuleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        singleLine = minLines == 1,
        minLines = minLines,
        maxLines = if (minLines == 1) 1 else 6,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(CardFill, shape)
                    .border(1.dp, Color(0xFF333333), shape)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF777777))
                inner()
            }
        },
    )
}

/** White track when on, gray outline when off, like the concept's toggles. */
@Composable
fun ModuleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, contentDescription: String) {
    val track = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .size(width = 52.dp, height = 30.dp)
            .background(if (checked) MaterialTheme.colorScheme.onSurface else Color.Transparent, track)
            .border(1.dp, if (checked) MaterialTheme.colorScheme.onSurface else Color(0xFF444444), track)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { this.contentDescription = contentDescription }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(22.dp).background(if (checked) Color.Black else Color(0xFF5A5A5A), CircleShape))
    }
}

/** One option of a small set (rest seconds, lock time…): filled white when selected. */
@Composable
fun ModuleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .background(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, shape)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color(0xFF333333), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        SmallLabel(label, color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface)
    }
}

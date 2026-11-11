package com.adrian.minimallauncher.feature.finance

import androidx.compose.ui.unit.Dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.DockItem
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState

internal val FinanceWhite = Color(0xFFF8F9F7)
internal val FinanceMuted = Color(0xFFE8E8E8)
// Líneas que separan secciones: lo bastante claras para verse sobre negro.
internal val FinanceRule = Color(0xFF3B4C5C)

// Tarjetas que separan cada sección (diseño 2026-11-10): fondo azul muy oscuro y borde visible.
// 2026-11-11: translúcidas (gris sobre negro), sin el azul oscuro.
internal val FinanceCardFill = Color.White.copy(alpha = 0.05f)
internal val FinanceCardLine = Color.White.copy(alpha = 0.09f)

/**
 * Sección sin contenedor (contención implícita): la separan el espacio y su encabezado.
 * Reglas de Finanzas (2026-11-10):
 *  - Sin tarjeta: cifras principales (van con su barra de color a la izquierda), listas
 *    (encabezado y líneas entre filas) y cuadrículas cuyas celdas ya son tarjetas.
 *  - Con tarjeta ([FinanceCard]): lo que se toca como un bloque (las gráficas) y lo editable (Plan).
 */
@Composable
internal fun FinanceSection(modifier: Modifier = Modifier, spacing: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
}

/** Una sección de Finanzas en su propia tarjeta: solo para gráficas y lo editable. */
@Composable
internal fun FinanceCard(modifier: Modifier = Modifier, spacing: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(FinanceCardFill, RoundedCornerShape(20.dp))
            .border(1.dp, FinanceCardLine, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Amounts share tabular digits and fit the available column, including large balances. */
@Composable
internal fun FinanceAmount(cents: Long, modifier: Modifier = Modifier, size: Int = 40, color: Color = FinanceWhite) {
    val text = wholePesos(cents)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = MaterialTheme.typography.headlineLarge.copy(
        fontSize = size.sp, lineHeight = (size + 6).sp,
        fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum", letterSpacing = (-0.5).sp,
    )
    BoxWithConstraints(modifier) {
        val width = with(density) { maxWidth.toPx() }
        val measured = measurer.measure(text, style, softWrap = false).size.width
        val scale = (width / measured.coerceAtLeast(1)).coerceIn(0.1f, 1f)
        Text(text, style = style.copy(fontSize = (size * scale).sp, lineHeight = ((size + 6) * scale).sp), color = color, maxLines = 1)
    }
}

@Composable
internal fun FinanceSectionHeading(title: String, detail: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = FinanceWhite)
        detail?.let { FinanceReadMore(it) }
    }
}

/**
 * "Leer más" (Read more): truncates long secondary explanations or descriptions to [collapsedLines] with ellipsis,
 * and unfolds the full text on tap.
 */
@Composable
internal fun FinanceReadMore(
    text: String,
    modifier: Modifier = Modifier,
    collapsedLines: Int = 1,
    color: Color = FinanceMuted,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    var overflows by rememberSaveable(text) { mutableStateOf(false) }
    val longText = expanded || overflows
    Row(
        modifier
            .fillMaxWidth()
            .then(if (longText) Modifier.heightIn(min = 48.dp).semantics {
                stateDescription = if (expanded) "Expandido" else "Contraído"
            }.clickable(role = Role.Button) { expanded = !expanded } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded && !overflows) overflows = it.hasVisualOverflow },
        )
        if (longText) {
            DisclosureChevron(expanded)
        }
    }
}

/** Font-independent chevron; the containing button exposes the expanded state. */
@Composable
private fun DisclosureChevron(expanded: Boolean) {
    Canvas(Modifier.size(20.dp)) {
        val startY = size.height * if (expanded) 0.65f else 0.35f
        val middleY = size.height - startY
        drawLine(FinanceWhite, Offset(size.width * 0.25f, startY), Offset(size.width * 0.5f, middleY), 2.dp.toPx(), StrokeCap.Round)
        drawLine(FinanceWhite, Offset(size.width * 0.5f, middleY), Offset(size.width * 0.75f, startY), 2.dp.toPx(), StrokeCap.Round)
    }
}

/**
 * Colapsable (Collapsible / Toggle) & Acordeón (Accordion) item: header row with title, optional subtitle,
 * a summary below the title, and a chevron that reveals [content] when expanded.
 */
@Composable
internal fun FinanceCollapsible(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: String? = null,
    summaryText: String? = null,
    summaryColor: Color = FinanceWhite,
    showRule: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .semantics { stateDescription = if (expanded) "Expandido" else "Contraído" }
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Ocultar $title" else "Ver $title", onClick = onToggle)
                .heightIn(min = 48.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                StrokeIcon(icon, StrokeState.DONE, 22.dp, color = FinanceMuted)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = FinanceWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summaryText != null) {
                    Text(summaryText, style = MaterialTheme.typography.titleMedium, color = summaryColor)
                }
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            DisclosureChevron(expanded)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
        if (showRule) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(FinanceRule))
        }
    }
}

/**
 * Menú desplegable (Dropdown / Disclosure widget): compact button that reveals a menu of actions or options on click.
 */
@Composable
internal fun <T> FinanceDropdown(
    label: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: String? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .background(CardFill, RoundedCornerShape(14.dp))
                .border(1.dp, CardLine, RoundedCornerShape(14.dp))
                .semantics { stateDescription = if (open) "Expandido" else "Contraído" }
                .clickable(role = Role.Button) { open = !open }
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                StrokeIcon(leadingIcon, StrokeState.DONE, 16.dp)
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = FinanceWhite, maxLines = 1)
            DisclosureChevron(open)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(Color(0xFF141414)),
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, color = FinanceWhite, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        open = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

@Composable
internal fun FinanceBackHeader(title: String, onBack: () -> Unit) {
    // Solo el chevron para volver (sin la palabra) y el título de vista a 40.
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(44.dp).clickable(role = Role.Button, onClickLabel = "Volver", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Text("‹", style = MaterialTheme.typography.headlineLarge, color = FinanceMuted) }
        Text(title, style = MaterialTheme.typography.headlineLarge, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Equal, reachable tabs keep long period labels inside narrow screens. */
@Composable
internal fun FinanceRangeSwitch(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup().background(Color(0xFF141414), RoundedCornerShape(16.dp)).padding(4.dp)) {
        options.forEach { (key, label) ->
            Box(
                Modifier.weight(1f).background(if (key == selected) Color(0xFF303030) else Color.Transparent, RoundedCornerShape(12.dp))
                    .selectable(selected = key == selected, role = Role.Tab, onClick = { onSelect(key) })
                    .heightIn(min = 48.dp).padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (key == selected) FinanceWhite else FinanceMuted)
            }
        }
    }
}

/** Finance labels remain readable when Android's text size is increased. */
@Composable
internal fun FinanceDock(items: List<DockItem>, selected: String, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.Black)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(FinanceRule))
        Row(Modifier.fillMaxWidth().selectableGroup().padding(vertical = 8.dp)) {
            items.forEach { item ->
                val active = item.key == selected
                val color = if (active) LauncherAccent else FinanceMuted
                Column(
                    Modifier.weight(1f).selectable(active, role = Role.Tab, onClick = { onOpen(item.key) })
                        .heightIn(min = 60.dp).padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        StrokeIcon(item.icon, StrokeState.DONE, 24.dp, color = color)
                        item.badge?.let { Text(it, color = FinanceWhite, style = MaterialTheme.typography.labelSmall) }
                    }
                    Text(item.label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = color, maxLines = 2)
                }
            }
        }
    }
}

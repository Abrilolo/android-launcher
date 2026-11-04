package com.adrian.minimallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.AppCategory
import com.adrian.minimallauncher.data.AppGroup
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.data.Space
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleChip
import com.adrian.minimallauncher.feature.SearchField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StatusDot
import com.adrian.minimallauncher.ui.components.AppList
import com.adrian.minimallauncher.ui.components.DotIcon
import com.adrian.minimallauncher.ui.components.DotIcons
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import com.adrian.minimallauncher.ui.components.appItemHeight
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

val SECTION_TITLES = mapOf(
    "apps" to "Esenciales",
    "school" to "Escuela",
    "entertainment" to "Entretenimiento",
    "games" to "Juegos",
    "all" to "Todas",
)

@Composable
fun AppsPage(
    apps: List<LauncherApp>,
    categories: List<AppCategory>,
    isAllowed: (LauncherApp) -> Boolean,
    onOpenApp: (String) -> Unit,
    onRestrictedApp: (LauncherApp) -> Unit,
    onSetAppCategory: (packageName: String, categoryId: String) -> Unit,
    onSetCategoryApps: (categoryId: String, Set<String>) -> Unit,
    onSetAppDistraction: (packageName: String, Boolean) -> Unit,
    onAddCategory: (name: String, isDistraction: Boolean) -> String,
    onRenameCategory: (id: String, name: String) -> Unit,
    onSetCategoryDistraction: (id: String, isDistraction: Boolean) -> Unit,
    onSetCategoryVisible: (id: String, visible: Boolean) -> Unit,
    onMoveCategory: (id: String, delta: Int) -> Unit,
    onDeleteCategory: (id: String) -> Unit,
    space: Space,
    spaces: List<Space>,
    now: LocalDateTime,
    onSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf("apps") }
    var query by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editingAppPkg by rememberSaveable { mutableStateOf<String?>(null) }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val closeThresholdPx = with(density) { 72.dp.toPx() }

    val nonVaultApps = remember(apps) { apps.filterNot { it.isVault } }
    val categoryById = remember(categories) { categories.associateBy { it.id } }
    val shownCategories = remember(categories, editing) {
        if (editing) categories else categories.filter { it.visible }.ifEmpty { categories }
    }
    val current = tab.takeIf { id -> shownCategories.any { it.id == id } }
        ?: shownCategories.firstOrNull()?.id
        ?: "all"
    val currentCategory = categoryById[current]

    val searching = query.isNotBlank()
    val shown = remember(nonVaultApps, searching, query, current) {
        when {
            searching -> nonVaultApps
                .filter { it.label.contains(query.trim(), ignoreCase = true) }
                .sortedBy { it.label.lowercase() }
            current == "all" -> nonVaultApps.sortedBy { it.label.lowercase() }
            else -> nonVaultApps
                .filter { it.categoryId == current }
                .sortedBy { it.label.lowercase() }
        }
    }

    val listState = rememberLazyListState()
    val editListState = rememberLazyListState()
    var overscrollDown by remember { mutableFloatStateOf(0f) }
    val nestedSwipeClose = remember(listState, editListState, editing, closeThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f) overscrollDown = 0f
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val activeList = if (editing) editListState else listState
                if (!editing && source == NestedScrollSource.UserInput && available.y > 0f &&
                    activeList.firstVisibleItemIndex == 0 && activeList.firstVisibleItemScrollOffset == 0
                ) {
                    overscrollDown += available.y
                    if (overscrollDown >= closeThresholdPx) {
                        overscrollDown = 0f
                        onClose()
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                overscrollDown = 0f
                return Velocity.Zero
            }
        }
    }

    BackHandler {
        when {
            editingAppPkg != null -> editingAppPkg = null
            editing -> editing = false
            query.isNotEmpty() -> query = ""
            else -> onClose()
        }
    }

    val open: (LauncherApp) -> Unit = { app ->
        if (isAllowed(app)) onOpenApp(app.packageName) else onRestrictedApp(app)
    }
    val normal = MaterialTheme.colorScheme.onSurface
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedSwipeClose)
            .imePadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top bar: magnifying glass search + standard pencil/done icon
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                )
                RoundActionIcon(
                    icon = if (editing) "listo" else "lapiz",
                    description = if (editing) "Listo" else "Editar",
                    color = if (editing) LauncherRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { editing = !editing },
                )
            }

            // Category tabs strip
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                shownCategories.forEach { category ->
                    Box(Modifier.alpha(if (category.visible) 1f else 0.45f)) {
                        ModuleChip(
                            label = category.name.uppercase(spanish),
                            selected = (!searching || editing) && category.id == current,
                            onClick = {
                                tab = category.id
                                if (!editing) query = ""
                            },
                        )
                    }
                }
                if (editing) {
                    AddCategoryButton {
                        val id = onAddCategory("Grupo", false)
                        tab = id
                    }
                }
            }

            if (editing) {
                // In-place editor for the selected category and apps
                if (currentCategory != null && currentCategory.id != "all") {
                    val catIndex = categories.indexOfFirst { it.id == currentCategory.id }
                    var draftName by remember(currentCategory.id, currentCategory.name) { mutableStateOf(currentCategory.name) }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardFill)
                            .border(1.dp, CardLine, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BasicTextField(
                            value = draftName,
                            onValueChange = {
                                draftName = it.take(24)
                                if (draftName.isNotBlank()) onRenameCategory(currentCategory.id, draftName)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(LauncherRed),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (draftName.isNotBlank()) onRenameCategory(currentCategory.id, draftName)
                                },
                            ),
                        )
                        if (catIndex > 0) {
                            Text(
                                "←",
                                Modifier
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button) { onMoveCategory(currentCategory.id, -1) }
                                    .padding(6.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (catIndex in 0 until categories.lastIndex) {
                            Text(
                                "→",
                                Modifier
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button) { onMoveCategory(currentCategory.id, 1) }
                                    .padding(6.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RoundActionIcon(
                            icon = if (currentCategory.visible) "ojo" else "ojo_off",
                            description = "Mostrar u ocultar",
                            state = if (currentCategory.visible) StrokeState.DONE else StrokeState.OFF,
                            onClick = { onSetCategoryVisible(currentCategory.id, !currentCategory.visible) },
                        )
                        RoundActionIcon(
                            icon = if (currentCategory.isDistraction) "candado" else "candado_abierto",
                            description = "Bloquear fuera de ocio",
                            state = if (currentCategory.isDistraction) StrokeState.NOW else StrokeState.OFF,
                            onClick = { onSetCategoryDistraction(currentCategory.id, !currentCategory.isDistraction) },
                        )
                        if (currentCategory.id != "apps") {
                            RoundActionIcon(
                                icon = "papelera",
                                description = "Eliminar categoría",
                                onClick = {
                                    onDeleteCategory(currentCategory.id)
                                    tab = "apps"
                                },
                            )
                        }
                    }
                }

                val editableApps = remember(nonVaultApps, query, current) {
                    val filtered = if (query.isBlank()) nonVaultApps else nonVaultApps.filter {
                        it.label.contains(query.trim(), ignoreCase = true)
                    }
                    if (current == "all") {
                        filtered.sortedBy { it.label.lowercase() }
                    } else {
                        filtered.sortedWith(
                            compareByDescending<LauncherApp> { it.categoryId == current }
                                .thenBy { it.label.lowercase() },
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = editListState,
                ) {
                    items(editableApps, key = { it.packageName }) { app ->
                        val inCurrent = current != "all" && app.categoryId == current
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    if (current == "all") {
                                        editingAppPkg = app.packageName
                                    } else {
                                        val targetCat = if (inCurrent) "all" else current
                                        onSetAppCategory(app.packageName, targetCat)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (current != "all") {
                                StatusDot(filled = inCurrent, next = false, size = 14.dp)
                            }
                            Text(
                                app.label,
                                Modifier.weight(1f),
                                color = if (current == "all" || inCurrent) normal else dimmed,
                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (current == "all") {
                                val catName = categoryById[app.categoryId]?.name?.uppercase(spanish) ?: "TODAS"
                                SmallLabel(
                                    catName,
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, CardLine, RoundedCornerShape(10.dp))
                                        .clickable { editingAppPkg = app.packageName }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                            RoundActionIcon(
                                icon = if (app.isEntertainment) "candado" else "candado_abierto",
                                description = "Bloquear app",
                                state = if (app.isEntertainment) StrokeState.NOW else StrokeState.OFF,
                                onClick = { onSetAppDistraction(app.packageName, !app.isEntertainment) },
                            )
                        }
                    }
                }
            } else {
                // Normal app list
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val rows = (maxHeight / appItemHeight()).toInt().coerceAtLeast(3)
                    AppList(
                        apps = shown,
                        visibleRows = rows,
                        onClick = open,
                        colorFor = { app -> if (isAllowed(app)) normal else dimmed },
                        lockedFor = { app -> !isAllowed(app) },
                        onLongClick = { app ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            editingAppPkg = app.packageName
                        },
                        alphabetIndex = !searching && current == "all",
                        emptyText = null,
                        listState = listState,
                    )
                }

                val backAt = leisureBackAt(space, spaces, now)
                if (backAt != null && shown.any { !isAllowed(it) }) {
                    Row(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 8.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Ocio vuelve a las ${backAt.format(hhmm)}"
                            },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DotIcon(DotIcons.relojArena, done = true, contentDescription = null, size = 18.dp)
                        SmallLabel(backAt.format(hhmm), color = LauncherRed)
                    }
                }
            }
        }

        // Minimal single-app category & lock sheet on long-press (zero explanatory grey text)
        val editingApp = editingAppPkg?.let { pkg -> nonVaultApps.firstOrNull { it.packageName == pkg } }
        if (editingApp != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable { editingAppPkg = null },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false, onClick = {}),
                ) {
                    AppCategorySheet(
                        app = editingApp,
                        categories = categories,
                        onSelectCategory = { catId ->
                            onSetAppCategory(editingApp.packageName, catId)
                            editingAppPkg = null
                        },
                        onToggleDistraction = { isDistraction ->
                            onSetAppDistraction(editingApp.packageName, isDistraction)
                        },
                        onClose = { editingAppPkg = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundActionIcon(
    icon: String,
    description: String,
    state: StrokeState = StrokeState.DONE,
    color: Color? = null,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        StrokeIcon(icon, state, 20.dp, color = color)
    }
}

@Composable
private fun AddCategoryButton(onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Nueva categoría" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(36.dp)) {
            val w = 1.5.dp.toPx()
            drawRoundRect(
                Color(0xFF3A3A3A),
                topLeft = Offset(w / 2, w / 2),
                size = Size(size.width - w, size.height - w),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
            )
        }
        Text("+", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C5C5C))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppCategorySheet(
    app: LauncherApp,
    categories: List<AppCategory>,
    onSelectCategory: (String) -> Unit,
    onToggleDistraction: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardFill)
            .border(1.dp, CardLine, shape)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                app.label,
                Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            RoundActionIcon(
                icon = if (app.isEntertainment) "candado" else "candado_abierto",
                description = "Bloquear app",
                state = if (app.isEntertainment) StrokeState.NOW else StrokeState.OFF,
                onClick = { onToggleDistraction(!app.isEntertainment) },
            )
            RoundActionIcon(
                icon = "listo",
                description = "Listo",
                color = LauncherRed,
                onClick = onClose,
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                ModuleChip(
                    label = category.name.uppercase(spanish),
                    selected = app.categoryId == category.id,
                    onClick = { onSelectCategory(category.id) },
                )
            }
        }
    }
}

/** When entertainment opens again, or null if the current space already allows it. */
private fun leisureBackAt(space: Space, spaces: List<Space>, now: LocalDateTime): LocalTime? {
    if (AppGroup.ENTERTAINMENT in space.availableGroups) return null
    val time = now.toLocalTime()
    return spaces.filter { AppGroup.ENTERTAINMENT in it.availableGroups }
        .map { it.startsAt }
        .minByOrNull { start -> Duration.between(time, start).let { if (it.isNegative) it.plusDays(1) else it } }
}

package com.adrian.minimallauncher.feature.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.Space
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.animationsOff
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The AI assistants' widgets: Claude, ChatGPT and Gemini as 17×17 dot icons (concept: the "Widgets IA" canvas,
 * BACKLOG "Widgets de asistentes IA"). Claude was drawn by the user; ChatGPT and Gemini come from the user's vectors.
 * Each plays its own animation (Claude waves, ChatGPT draws itself, Gemini sparkles) when it comes into view.
 */
enum class AiApp(val id: String, val label: String, val packageName: String, val grid: List<String>) {
    CLAUDE(
        "claude", "Claude", "com.anthropic.claude",
        listOf(
            ".................",
            ".................",
            ".................",
            "..#############..",
            "..#############..",
            "..#############..",
            "..##.#######.##..",
            "..#############..",
            "#################",
            "#################",
            "..#############..",
            "..#############..",
            "...#.#.....#.#...",
            "...#.#.....#.#...",
            ".................",
            ".................",
            ".................",
        ),
    ),
    CHATGPT(
        "chatgpt", "ChatGPT", "com.openai.chatgpt",
        listOf(
            ".....#####.......",
            "....###..####....",
            "....#...#######..",
            "..###..##.....##.",
            ".####.##..##..##.",
            "##.##.#.######.#.",
            "##.##.#####..###.",
            "##.##.#...##..##.",
            "##.####...####.##",
            ".##..##...#.##..#",
            ".###..#####.##.##",
            ".#.######.#.##.##",
            ".##..##..##.####.",
            ".##.....###.###..",
            "..#######...#....",
            "....#####.###....",
            ".......#####.....",
        ),
    ),
    GEMINI(
        "gemini", "Gemini", "com.google.android.apps.bard",
        listOf(
            ".................",
            "........#........",
            "........#........",
            ".......###.......",
            ".......###.......",
            "......#####......",
            ".....#######.....",
            "...###########...",
            ".###############.",
            "...###########...",
            ".....#######.....",
            "......#####......",
            ".......###.......",
            ".......###.......",
            "........#........",
            "........#........",
            ".................",
        ),
    );

    /** The widget showing only this app (2×2 or 4×2). */
    val widgetId: String get() = WIDGET_PREFIX + id

    /** How many frames its tap animation has; each lasts [FRAME_MS]. */
    val frameCount: Int get() = when (this) {
        CLAUDE -> CLAUDE_FRAMES.size
        CHATGPT -> GPT_STEPS + 1
        GEMINI -> GEM_FRAMES.size
    }

    companion object {
        fun byId(id: String): AiApp? = entries.firstOrNull { it.id == id }
        fun fromWidget(widgetId: String): AiApp? = if (widgetId.startsWith(WIDGET_PREFIX)) byId(widgetId.removePrefix(WIDGET_PREFIX)) else null
        /** The first app, in the user's order, that [taken] widgets don't show yet; for a stack that already has some. */
        fun firstFree(order: List<String>, taken: List<String>): AiApp =
            (order.mapNotNull(::byId) + entries).firstOrNull { it.widgetId !in taken } ?: CLAUDE
    }
}

/** The 4×1 strip with every assistant the user chose; the single-app widgets are "ai:<id>". */
const val AI_STRIP = "ai"
private const val WIDGET_PREFIX = "ai:"
val AI_APP_IDS = AiApp.entries.map { it.id }

/** A shortcut an app publishes (new chat, voice…), shown on the 4×2 widget. */
data class AppShortcut(val packageName: String, val id: String, val label: String)

/** What a widget needs to know about one assistant on the phone. */
data class AiAppState(
    val installed: Boolean = false,
    /** False when the current space keeps it closed; opening it goes through the pause. */
    val allowed: Boolean = true,
    /** When the schedule lets it open again, if it is closed now. */
    val backAt: LocalTime? = null,
    val shortcuts: List<AppShortcut> = emptyList(),
)

/** Builds every assistant's state from the installed apps and the schedule. */
fun aiStates(
    apps: List<LauncherApp>,
    space: Space,
    spaces: List<Space>,
    now: LocalDateTime,
    shortcuts: Map<String, List<AppShortcut>>,
): Map<AiApp, AiAppState> = AiApp.entries.associateWith { ai ->
    val app = apps.firstOrNull { it.packageName == ai.packageName }
    val allowed = app == null || space.allows(app)
    AiAppState(
        installed = app != null,
        allowed = allowed,
        backAt = if (app != null && !allowed) allowedAgainAt(app, spaces, now) else null,
        shortcuts = arrangeShortcuts(ai, shortcuts[ai.packageName].orEmpty()),
    )
}

/** Claude: take a photo for the chat, Code, a gallery image for the chat, voice. Gemini: voice and Lens. ChatGPT: its own. */
private fun arrangeShortcuts(app: AiApp, published: List<AppShortcut>): List<AppShortcut> = when (app) {
    AiApp.CLAUDE -> listOfNotNull(
        AppShortcut(app.packageName, AI_CAMERA, "Foto para el chat"),
        published.firstOrNull { glyphFor(it) == Glyph.CODE },
        AppShortcut(app.packageName, AI_GALLERY, "Imagen de la galería para el chat"),
        published.firstOrNull { glyphFor(it) == Glyph.VOICE },
    )
    else -> published.take(4)
}

/** Shortcuts the launcher handles itself: it takes or picks the photo, then shares it to the app. */
const val AI_CAMERA = "launcher:camera"
const val AI_GALLERY = "launcher:gallery"

/** Start of the next space that allows [app], looking a day ahead. */
private fun allowedAgainAt(app: LauncherApp, spaces: List<Space>, now: LocalDateTime): LocalTime? =
    (1..96).asSequence()
        .map { now.plusMinutes(15L * it) }
        .map { Space.at(it, spaces) }
        .firstOrNull { it.allows(app) }
        ?.startsAt

/** What tapping an assistant asks the launcher to do, through the widgets' onOpenModule. */
object AiRoutes {
    fun open(app: AiApp) = "app:" + app.packageName
    fun install(app: AiApp) = "install:" + app.packageName
    fun shortcut(shortcut: AppShortcut) = "shortcut:" + shortcut.packageName + "/" + shortcut.id
}

// ---- Dot icons and their animations ----

private const val FRAME_MS = 75L
private val White = Color.White
private val Gray = Color(0xFF5C5C5C)
private val Dim = Color(0xFF262626)

/** Claude blinks (gray eyelids), waves a white hand with a gray trail and bright eyes, then a white shine crosses it. */
private val CLAUDE_FRAMES = listOf("base", "blink", "base", "wave2", "wave1", "wave2", "wave1", "shine1", "shine2", "shine3", "base")
/** ChatGPT's knot draws itself clockwise from the top, its head in red. */
private const val GPT_STEPS = 8
/** Gemini's star blooms from its center with a red edge, its tips spark red, then it settles. */
private val GEM_FRAMES: List<Any> = listOf(0.15f, 0.35f, 0.55f, 0.75f, 1f, "spark", "spark2", "base")

private class Dot(val row: Int, val col: Int, val color: Color)

private fun cells(rows: List<String>): List<Triple<Int, Int, Char>> =
    rows.flatMapIndexed { r, line -> line.mapIndexedNotNull { c, ch -> if (ch != '.') Triple(r, c, ch) else null } }

/** Claude's grid for one frame: '#' its red body, 'w' white, 'g' gray. */
private fun claudeGrid(kind: String): List<String> {
    val g = AiApp.CLAUDE.grid.map { it.toCharArray() }
    when (kind) {
        "blink" -> { g[6][4] = 'g'; g[6][12] = 'g' }
        "sleep" -> { g[6][4] = '#'; g[6][12] = '#' }
        "wave1", "wave2" -> {
            val lift = if (kind == "wave1") 1 else 3
            val trail = if (kind == "wave1") 3 else 1
            g[6][4] = 'w'; g[6][12] = 'w'
            for (r in 8..9) { g[r][15] = '.'; g[r][16] = '.' }
            for (r in 8..9) { g[r - trail][15] = 'g'; g[r - trail][16] = 'g' }
            for (r in 8..9) { g[r - lift][15] = 'w'; g[r - lift][16] = 'w' }
        }
    }
    val band = when (kind) { "shine1" -> 9; "shine2" -> 15; "shine3" -> 21; else -> null }
    if (band != null) g.forEachIndexed { r, row -> row.forEachIndexed { c, ch -> if (ch == '#' && r + c in band..band + 2) row[c] = 'w' } }
    return g.map { String(it) }
}

/** Where a dot sits around the icon's center, 0 at the top going clockwise to 1. */
private fun angleOf(r: Int, c: Int): Float {
    var a = atan2((c - 8).toDouble(), (-(r - 8)).toDouble())
    if (a < 0) a += 2 * Math.PI
    return (a / (2 * Math.PI)).toFloat()
}

/** 0 at the star's center, about 1 at its tips. */
private fun starDist(r: Int, c: Int): Float =
    (abs(c - 8) / 7.5f).pow(0.6f) + (abs(r - 8) / 7.5f).pow(0.6f)

/** The dots of [app] at animation [frame] (-1 = at rest). [gray] is for an app that is missing or closed by the schedule. */
private fun frameDots(app: AiApp, frame: Int, gray: Boolean): List<Dot> {
    if (gray) return when (app) {
        AiApp.CLAUDE -> cells(claudeGrid("sleep")).map { (r, c) -> Dot(r, c, Gray) }
        AiApp.CHATGPT -> cells(app.grid).map { (r, c) -> Dot(r, c, Gray) }
        AiApp.GEMINI -> cells(app.grid).filter { (r, c) -> starDist(r, c) <= 0.62f }.map { (r, c) -> Dot(r, c, Gray) }
    }
    return when (app) {
        AiApp.CLAUDE -> {
            val kind = CLAUDE_FRAMES.getOrNull(frame) ?: "base"
            cells(claudeGrid(kind)).map { (r, c, ch) -> Dot(r, c, when (ch) { 'w' -> White; 'g' -> Gray; else -> LauncherRed }) }
        }
        AiApp.CHATGPT -> {
            val all = cells(app.grid)
            if (frame < 0 || frame >= GPT_STEPS) return all.map { (r, c) -> Dot(r, c, White) }
            val t = (frame + 1f) / GPT_STEPS
            all.map { (r, c) ->
                val a = angleOf(r, c)
                Dot(r, c, if (a > t) Dim else if (a > t - 1f / GPT_STEPS) LauncherRed else White)
            }
        }
        AiApp.GEMINI -> {
            val all = cells(app.grid)
            when (val f = GEM_FRAMES.getOrNull(frame) ?: "base") {
                is Float -> all.filter { (r, c) -> starDist(r, c) <= f * 1.05f }
                    .map { (r, c) -> Dot(r, c, if (f < 1f && starDist(r, c) > f * 1.05f - 0.22f) LauncherRed else White) }
                else -> {
                    val base = all.map { (r, c) -> Dot(r, c, White) }
                    when (f) {
                        "spark" -> base + listOf(0 to 8, 16 to 8, 8 to 0, 8 to 16).map { (r, c) -> Dot(r, c, LauncherRed) }
                        "spark2" -> base + listOf(3 to 3, 3 to 13, 13 to 3, 13 to 13).map { (r, c) -> Dot(r, c, LauncherRed) }
                        else -> base
                    }
                }
            }
        }
    }
}

/** One assistant's dot icon at [frame] (-1 = still), gray when [gray]. */
@Composable
fun AiDotIcon(app: AiApp, size: Dp, modifier: Modifier = Modifier, frame: Int = -1, gray: Boolean = false) {
    val dots = remember(app, frame, gray) { frameDots(app, frame, gray) }
    Canvas(modifier.size(size)) {
        val step = this.size.width / 17f
        val radius = step * 0.37f
        dots.forEach { drawCircle(it.color, radius, Offset((it.col + 0.5f) * step, (it.row + 0.5f) * step)) }
    }
}

/**
 * Bumped by the launcher when a widget comes into view: its page is entered, or a stack is swiped to it. Each
 * assistant icon then plays its animation once, and again when the launcher comes back from an app or the system.
 * Tapping never waits for it: the app opens right away.
 */
val LocalAiReplay = compositionLocalOf { 0 }

/** How many times the launcher came back to the front since this was first shown. */
@Composable
private fun rememberResumeCount(): Int {
    val owner = LocalLifecycleOwner.current
    var count by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        // Adding the observer replays the current state; only later resumes count.
        var seen = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { if (seen) count++ else seen = true }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return count
}

/** An assistant's icon that plays its animation once whenever it comes back into view (not on first show), unless gray. */
@Composable
private fun AnimatedAiIcon(app: AiApp, size: Dp, gray: Boolean, modifier: Modifier = Modifier, startDelayMs: Long = 0) {
    var frame by remember { mutableIntStateOf(-1) }
    val replay = LocalAiReplay.current
    val resumes = rememberResumeCount()
    val off = animationsOff()
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(replay, resumes) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        frame = -1
        if (gray || off) return@LaunchedEffect
        delay(startDelayMs)
        for (k in 0 until app.frameCount) { frame = k; delay(FRAME_MS) }
        frame = -1
    }
    AiDotIcon(app, size, modifier, frame, gray)
}

// ---- Widgets ----

private val ButtonShape = RoundedCornerShape(12.dp)
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The assistants' widget, logos only. [widgetId] is [AI_STRIP] (the chosen apps in a row) or one app's id; one app
 * takes a 2×2 square or, when the widget is wide, a 4×2 with that app's shortcuts. A tap asks the launcher, through
 * [onOpen], to open it right away (or install it, or pause first when the schedule says so).
 */
@Composable
fun AiWidget(
    widgetId: String,
    settings: ModuleSettings,
    states: Map<AiApp, AiAppState>,
    onOpen: (String) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launch: (AiApp, String?) -> Unit = { app, route ->
        val installed = states[app]?.installed == true
        onOpen(route ?: if (installed) AiRoutes.open(app) else AiRoutes.install(app))
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        // The strip previewed or stacked at a taller size shows one app, as it will once placed there.
        val single = AiApp.fromWidget(widgetId) ?: if (maxHeight >= 100.dp) AiApp.firstFree(settings.aiApps, emptyList()) else null
        when {
            single == null -> AiStrip(settings, states, launch, onLongClick)
            maxWidth > maxHeight * 1.6f && maxHeight >= 100.dp -> AiWide(single, states[single] ?: AiAppState(), launch, onLongClick)
            else -> AiSquare(single, states[single] ?: AiAppState(), launch, onLongClick)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.press(label: String, onClick: () -> Unit, onLongClick: () -> Unit) =
    combinedClickable(role = Role.Button, onClickLabel = label, onClick = onClick, onLongClick = onLongClick)

private val AiAppState.gray: Boolean get() = !installed || !allowed

/** 4×1: the chosen assistants side by side; they animate one after another. */
@Composable
private fun AiStrip(settings: ModuleSettings, states: Map<AiApp, AiAppState>, launch: (AiApp, String?) -> Unit, onLongClick: () -> Unit) {
    val apps = settings.aiApps.mapNotNull(AiApp::byId).ifEmpty { AiApp.entries }
    Row(Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))) {
        apps.forEachIndexed { i, app ->
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF1A1A1A)))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .press("Abrir ${app.label}", { launch(app, null) }, onLongClick)
                    .semantics { contentDescription = app.label },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedAiIcon(app, 34.dp, (states[app] ?: AiAppState()).gray, startDelayMs = i * 180L)
            }
        }
    }
}

/** 2×2: one assistant, and below it why it can't open, if so. */
@Composable
private fun AiSquare(app: AiApp, state: AiAppState, launch: (AiApp, String?) -> Unit, onLongClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .press("Abrir ${app.label}", { launch(app, null) }, onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        AnimatedAiIcon(app, 72.dp, state.gray, Modifier.align(Alignment.Center))
        StatusLabel(state, Modifier.align(Alignment.BottomEnd))
    }
}

/** 4×2: the assistant on the left, up to four of its shortcuts on the right as icons in a 2×2 matrix. */
@Composable
private fun AiWide(app: AiApp, state: AiAppState, launch: (AiApp, String?) -> Unit, onLongClick: () -> Unit) {
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .press("Abrir ${app.label}", { launch(app, null) }, onLongClick),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedAiIcon(app, 88.dp, state.gray)
        }
        val shortcuts = state.shortcuts.take(4)
        if (state.gray || shortcuts.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                ShortcutButton(if (state.installed) "ABRIR" else "INSTALAR", { launch(app, null) }, onLongClick)
                StatusLabel(state, Modifier.align(Alignment.End))
            }
        } else if (shortcuts.size <= 2) {
            Row(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                shortcuts.forEach { shortcut -> ShortcutCell(shortcut, { launch(app, AiRoutes.shortcut(shortcut)) }, onLongClick, Modifier) }
            }
        } else {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until 2).forEach { r ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0 until 2).forEach { c ->
                            val shortcut = shortcuts.getOrNull(r * 2 + c)
                            if (shortcut == null) Spacer(Modifier.weight(1f))
                            else ShortcutCell(shortcut, { launch(app, AiRoutes.shortcut(shortcut)) }, onLongClick, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ShortcutButton(label: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f, fill = false)
            .fillMaxWidth()
            .clip(ButtonShape)
            .border(1.dp, CardLine, ButtonShape)
            .background(Color.Black, ButtonShape)
            .press(label, onClick, onLongClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One shortcut as a line icon in a snug circle (no card); its label is read out by TalkBack. */
@Composable
private fun ShortcutCell(shortcut: AppShortcut, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .border(1.dp, CardLine, CircleShape)
                .press(shortcut.label, onClick, onLongClick)
                .semantics { contentDescription = shortcut.label },
            contentAlignment = Alignment.Center,
        ) {
            ShortcutGlyph(glyphFor(shortcut), Modifier.size(22.dp))
        }
    }
}

private enum class Glyph { NEW, VOICE, CAMERA, CODE, SEARCH, ASK, IMAGE, OTHER }

/** Guesses the icon from what the app calls the shortcut (Spanish or English) and its id. */
private fun glyphFor(shortcut: AppShortcut): Glyph {
    val text = (shortcut.label + " " + shortcut.id).lowercase()
    fun has(vararg words: String) = words.any { it in text }
    return when {
        has("voz", "voice", "hablar", "talk", "live", "audio", "dictat") -> Glyph.VOICE
        has("code", "código", "codigo") -> Glyph.CODE
        has("cámara", "camara", "camera", "lens", "foto", "photo", "scan") -> Glyph.CAMERA
        has("imagen", "image", "picture", "dibuj", "draw") -> Glyph.IMAGE
        has("buscar", "search", "busca") -> Glyph.SEARCH
        has("nuev", "new", "sesión", "sesion", "session", "chat") -> Glyph.NEW
        has("pregunt", "ask", "assist") -> Glyph.ASK
        else -> Glyph.OTHER
    }
}

/** Line icons, 2 dp with round ends, as in docs/design/PLAN_VISUAL.md. */
@Composable
private fun ShortcutGlyph(glyph: Glyph, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val c = Color.White
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(c, Offset(x1 * w, y1 * h), Offset(x2 * w, y2 * h), stroke.width, StrokeCap.Round)
        when (glyph) {
            Glyph.NEW -> { line(0.5f, 0.15f, 0.5f, 0.85f); line(0.15f, 0.5f, 0.85f, 0.5f) }
            Glyph.VOICE -> {
                drawRoundRect(c, Offset(0.36f * w, 0.08f * h), Size(0.28f * w, 0.5f * h), CornerRadius(0.14f * w), style = stroke)
                drawArc(c, 0f, 180f, false, Offset(0.2f * w, 0.2f * h), Size(0.6f * w, 0.52f * h), style = stroke)
                line(0.5f, 0.72f, 0.5f, 0.92f)
            }
            Glyph.CAMERA -> {
                drawRoundRect(c, Offset(0.08f * w, 0.28f * h), Size(0.84f * w, 0.6f * h), CornerRadius(0.12f * w), style = stroke)
                line(0.36f, 0.28f, 0.42f, 0.14f); line(0.42f, 0.14f, 0.58f, 0.14f); line(0.58f, 0.14f, 0.64f, 0.28f)
                drawCircle(c, 0.16f * w, Offset(0.5f * w, 0.58f * h), style = stroke)
            }
            Glyph.CODE -> {
                line(0.3f, 0.25f, 0.08f, 0.5f); line(0.08f, 0.5f, 0.3f, 0.75f)
                line(0.7f, 0.25f, 0.92f, 0.5f); line(0.92f, 0.5f, 0.7f, 0.75f)
                line(0.58f, 0.15f, 0.42f, 0.85f)
            }
            Glyph.SEARCH -> {
                drawCircle(c, 0.28f * w, Offset(0.42f * w, 0.42f * h), style = stroke)
                line(0.63f, 0.63f, 0.9f, 0.9f)
            }
            Glyph.ASK -> {
                drawRoundRect(c, Offset(0.08f * w, 0.12f * h), Size(0.84f * w, 0.58f * h), CornerRadius(0.18f * w), style = stroke)
                line(0.3f, 0.7f, 0.22f, 0.9f); line(0.22f, 0.9f, 0.48f, 0.7f)
            }
            Glyph.IMAGE -> {
                drawRoundRect(c, Offset(0.08f * w, 0.14f * h), Size(0.84f * w, 0.72f * h), CornerRadius(0.1f * w), style = stroke)
                line(0.16f, 0.76f, 0.4f, 0.48f); line(0.4f, 0.48f, 0.58f, 0.66f); line(0.58f, 0.66f, 0.7f, 0.54f); line(0.7f, 0.54f, 0.86f, 0.74f)
                drawCircle(c, 0.07f * w, Offset(0.68f * w, 0.32f * h))
            }
            Glyph.OTHER -> { line(0.25f, 0.75f, 0.78f, 0.22f); line(0.38f, 0.22f, 0.78f, 0.22f); line(0.78f, 0.22f, 0.78f, 0.62f) }
        }
    }
}

/** INSTALAR, or the hour it comes back in red; nothing when it can open. */
@Composable
private fun StatusLabel(state: AiAppState, modifier: Modifier = Modifier) {
    when {
        !state.installed -> SmallLabel("INSTALAR", modifier, color = Color.White)
        !state.allowed -> SmallLabel(state.backAt?.let { "VUELVE ${it.format(hhmm)}" } ?: "EN PAUSA", modifier, color = LauncherRed)
    }
}

// ---- Settings ----

/** The strip's settings: which assistants it shows and in what order. */
@Composable
fun AiAppsEditor(settings: ModuleSettings, onUpdate: ((ModuleSettings) -> ModuleSettings) -> Unit) {
    SmallLabel("APPS DEL WIDGET 4×1")
    val order = settings.aiApps.mapNotNull(AiApp::byId) + AiApp.entries.filter { it.id !in settings.aiApps }
    order.forEachIndexed { i, app ->
        val shown = app.id in settings.aiApps
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AiDotIcon(app, 26.dp, gray = !shown)
            Text(app.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (shown && i > 0) {
                SmallLabel("SUBIR", Modifier.clickable(role = Role.Button) {
                    onUpdate { s -> s.copy(aiApps = s.aiApps.toMutableList().apply { val at = indexOf(app.id); if (at > 0) { removeAt(at); add(at - 1, app.id) } }) }
                }.padding(8.dp))
            }
            SmallLabel(
                if (shown) "QUITAR" else "MOSTRAR",
                Modifier.clickable(role = Role.Button) {
                    onUpdate { s -> s.copy(aiApps = if (shown) s.aiApps - app.id else s.aiApps + app.id) }
                }.padding(8.dp),
                color = if (shown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Asked after placing a 2×2 or 4×2 assistant: which app it shows. */
@Composable
fun AiAppPicker(current: AiApp?, onPick: (AiApp) -> Unit) {
    SmallLabel("ELIGE LA APP DEL WIDGET")
    AiApp.entries.forEach { app ->
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(app) }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AiDotIcon(app, 32.dp, gray = app != current)
            Text(app.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (app == current) SmallLabel("EN EL WIDGET", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

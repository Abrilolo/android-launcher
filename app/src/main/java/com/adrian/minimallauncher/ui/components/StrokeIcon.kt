package com.adrian.minimallauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How a stroke icon is lit: gray when missing, white when done, white with its red detail when active. */
enum class StrokeState { OFF, DONE, NOW }

/** A continuous-line icon on a 24 grid: [main] is the line, [red] its optional detail. Paths are SVG path data. */
class StrokeGlyph(val main: String, val red: String? = null)

/** The stroke icon set (docs/design/concept/RutinasVisual.html). The AI assistants keep their dot icons. */
object StrokeIcons {
    val all: Map<String, StrokeGlyph> = mapOf(
        "inicio" to StrokeGlyph("M2 18h4a6 6 0 0 1 12 0h4", "M12 4v3M5 8l2 2M19 8l-2 2"),
        "traslado" to StrokeGlyph("M7 20v-2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-1v2M4 11h16", "M8 14.5h.01M16 14.5h.01"),
        "trabajo" to StrokeGlyph("M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7h4.5A1.5 1.5 0 0 1 21 8.5v10a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5v-10A1.5 1.5 0 0 1 4.5 7H9", "M10.5 12.5h3"),
        "salida" to StrokeGlyph("M13 4H6.5A1.5 1.5 0 0 0 5 5.5v13A1.5 1.5 0 0 0 6.5 20H13", "M10 12h10M17 9l3 3-3 3"),
        "casa" to StrokeGlyph("M5 20V10l7-6 7 6v10H5", "M10 20v-5h4v5"),
        "agua" to StrokeGlyph("M5 4h14l-2 16H7L5 4", "M6 10h12"),
        "caminar" to StrokeGlyph("M7 3c2 0 3 2.2 3 5s-1 5-3 5-3-2.2-3-5 1-5 3-5", "M17 10c2 0 3 2.2 3 5s-1 5-3 5-3-2.2-3-5 1-5 3-5"),
        "colacion" to StrokeGlyph("M12 7.5C10 6 5 6 5 12s4 9.5 7 8c3 1.5 7-2 7-8s-5-6-7-4.5", "M12 7.5c0-2 1-3.5 3-4.5"),
        "musica" to StrokeGlyph("M9 18V5l11-2v13", "M9 18a3 3 0 1 1-6 0a3 3 0 1 1 6 0M20 16a3 3 0 1 1-6 0a3 3 0 1 1 6 0"),
        "podcast" to StrokeGlyph("M9 6a3 3 0 0 1 6 0v6a3 3 0 0 1-6 0V6", "M5 11a7 7 0 0 0 14 0M12 18v3"),
        "whatsapp" to StrokeGlyph("M4 20l1.4-4.2A8 8 0 1 1 8.6 19z", "M8.5 12h.01M12 12h.01M15.5 12h.01"),
        "pendientes" to StrokeGlyph("M6 3h9l4 4v14H6V3", "M9 12h6M9 16h4"),
        "ordenar" to StrokeGlyph("M3 8l9-4 9 4v9l-9 4-9-4V8l9 4 9-4", "M12 12v9"),
        "descanso" to StrokeGlyph("M6 3h12M17 3c0 6-10 8-10 18M7 3c0 6 10 8 10 18M6 21h12", "M10 18h4"),
        "gimnasio" to StrokeGlyph("M3 10v4M6 7v10M18 7v10M21 10v4", "M6 12h12"),
        "dormir" to StrokeGlyph("M20 14A8 8 0 1 1 10 4a6 6 0 0 0 10 10z", "M17 3v3M15.5 4.5h3"),
        "apps" to StrokeGlyph("M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z"),
        "candado" to StrokeGlyph("M7 11V8a5 5 0 0 1 10 0v3M5 11h14v10H5z", "M12 15v2"),
        "pin" to StrokeGlyph("M12 21s-6-5.7-6-11a6 6 0 0 1 12 0c0 5.3-6 11-6 11z", "M12 10h.01"),
        "reloj" to StrokeGlyph("M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0", "M12 7v5l3 2"),
        "nueva" to StrokeGlyph("M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0", "M12 8v8M8 12h8"),
        "lapiz" to StrokeGlyph("M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"),
        "papelera" to StrokeGlyph("M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14"),
        "listo" to StrokeGlyph("M20 6 9 17l-5-5"),
        "plato" to StrokeGlyph("M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0", "M16 12a4 4 0 1 1-8 0a4 4 0 1 1 8 0"),
        "pago" to StrokeGlyph("M3 6h18v12H3V6", "M3 10h18M7 15h3"),
        "campana" to StrokeGlyph("M6 16v-5a6 6 0 0 1 12 0v5l2 2H4l2-2", "M10 21h4"),
        "escuela" to StrokeGlyph("M4 5h7a1 1 0 0 1 1 1v14a1 1 0 0 0-1-1H4zM20 5h-7a1 1 0 0 0-1 1v14a1 1 0 0 1 1-1h7z", "M15 9h2"),
        "hoja" to StrokeGlyph("M5 19C5 10 10 5 19 5c0 9-5 14-14 14z", "M5 19l8-8"),
        // Finanzas (docs/design/concept/FinanzasVisual.html); bolsa, transferencia, regalo, servicio and semana
        // come from Registro and Ejercicio below.
        "ingreso" to StrokeGlyph("M4 14v5h16v-5", "M12 3v10M8 9l4 4 4-4"),
        "fijos" to StrokeGlyph("M6 3h12v18l-3-2-3 2-3-2-3 2V3", "M9 8h6M9 12h6"),
        "ahorro" to StrokeGlyph("M6 8v11a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V8M5 8h14M8 4h8v4", "M12 12v4"),
        "extra" to StrokeGlyph("M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0", "M12 7v10M15 9.5c0-1.2-1.3-2-3-2s-3 .8-3 2c0 3 6 1.8 6 4.8 0 1.2-1.3 2-3 2s-3-.8-3-2"),
        "compartido" to StrokeGlyph("M12 8a3 3 0 1 1-6 0a3 3 0 0 1 6 0M3 20a6 6 0 0 1 12 0", "M15 5a3 3 0 0 1 0 6M18 20a6 6 0 0 0-2.5-4.9"),
        "ancla" to StrokeGlyph("M12 8v13M5 14a7 7 0 0 0 14 0", "M14 5a2 2 0 1 1-4 0a2 2 0 0 1 4 0M9 12h6"),
        "dia" to StrokeGlyph("M4 6h16v14H4zM4 10h16M8 3v4M16 3v4", "M12 15h.01"),
        "mes" to StrokeGlyph("M4 6h16v14H4zM4 10h16M8 3v4M16 3v4", "M7.5 13.5h9M7.5 17h9"),
        "otro" to StrokeGlyph("M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0", "M8 12h.01M12 12h.01M16 12h.01"),
        "no" to StrokeGlyph("M6 6l12 12", "M18 6 6 18"),
        "aviso" to StrokeGlyph("M12 3 2 20h20L12 3", "M12 10v4M12 17h.01"),
        // Registro obligatorio (docs/design/concept/RegistroVisual.html).
        "bolsa" to StrokeGlyph("M5 8h14l-1 12H6L5 8M9 8V6a3 3 0 0 1 6 0v2", "M10 13l2 2 2-2"),
        "transferencia" to StrokeGlyph("M4 8h15l-3-3M20 16H5l3 3", "M19 8l-3 3M5 16l3-3"),
        "regalo" to StrokeGlyph("M4 11h16v9H4v-9M3 7h18v4H3V7M12 7v13", "M12 7C10 3 6 4 8 7M12 7c2-4 6-3 4 0"),
        "servicio" to StrokeGlyph("M9 3v5M15 3v5M6 8h12v3a6 6 0 0 1-12 0V8", "M12 17v4"),
        "borrar" to StrokeGlyph("M9 6h11v12H9l-6-6 6-6", "M12 10l4 4M16 10l-4 4"),
        // Notas (docs/design/concept/NotasRecordatoriosVisual.html): a pending note and a done one.
        "circulo" to StrokeGlyph("M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0"),
        "nota_hecha" to StrokeGlyph("M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0", "M16 9l-5.5 6L8 12.5"),
        // Ejercicio (docs/design/concept/EjercicioVisual.html): muscle groups, session kinds and body tracking.
        "pecho" to StrokeGlyph("M4 6c3-2 13-2 16 0l-1.5 14h-13L4 6", "M7 10.5c2 2 4 2 5 0c1 2 3 2 5 0"),
        "espalda" to StrokeGlyph("M4 6c4 1.5 12 1.5 16 0l-4 14H8L4 6", "M12 9v9"),
        "piernas" to StrokeGlyph("M8 3h8M8 3v8l-2 10M16 3v8l2 10", "M8 11h.01M16 11h.01"),
        "hombros" to StrokeGlyph("M12 4a2.5 2.5 0 1 1 0 5a2.5 2.5 0 1 1 0-5M4 19c0-5 3-8 8-8s8 3 8 8", "M5 14l2 1M19 14l-2 1"),
        "brazos" to StrokeGlyph("M4 20V9l4-5h3l-1 3H8v4c2-2 6-2 8 0l4 4v5H4", "M10 14c1-1 3-1 4 0"),
        "core" to StrokeGlyph("M7 3h10v18H7z", "M12 3v18M7 9h10M7 15h10"),
        "bici" to StrokeGlyph("M9 17.5a3.5 3.5 0 1 1-7 0a3.5 3.5 0 1 1 7 0M22 17.5a3.5 3.5 0 1 1-7 0a3.5 3.5 0 1 1 7 0", "M5.5 17.5L9 9h6l3.5 8.5M8 6h3"),
        "hiit" to StrokeGlyph("M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0", "M13 6l-4 7h4l-2 5"),
        "fuego" to StrokeGlyph("M12 21c-4 0-7-3-7-7 0-4 3-6 4-10 2 2 3 3 3 5 1-1 2-2 2-4 3 2 5 5 5 9 0 4-3 7-7 7z", "M12 21c-2 0-3-1.5-3-3.5 0-2 2-3 3-5 1 2 3 3 3 5 0 2-1 3.5-3 3.5z"),
        "calentar" to StrokeGlyph("M10 14V5a2 2 0 0 1 4 0v9a4 4 0 1 1-4 0", "M12 9v8"),
        "bascula" to StrokeGlyph("M4 4h16v16H4z", "M8 10a4 4 0 0 1 8 0M12 10l1.5-2"),
        "cinta" to StrokeGlyph("M17 12a6 6 0 1 1-12 0a6 6 0 1 1 12 0M11 18h10", "M11 10a2 2 0 1 1 0 4a2 2 0 1 1 0-4M15 18v-2M18 18v-2"),
        "semana" to StrokeGlyph("M4 5h16v15H4zM4 10h16M8 3v4M16 3v4", "M14 14h3v3h-3z"),
        "meta" to StrokeGlyph("M5 21V4h12l-2 4 2 4H5"),
        "info" to StrokeGlyph("M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0", "M12 11v6M12 7.5h.01"),
        // Comida (docs/design/concept/ComidaVisual.html); "bascula" and "semana" come from Ejercicio.
        "snack" to StrokeGlyph("M7 21V3h10v18H7M7 9h10M7 15h10", "M12 9v6"),
        "cena" to StrokeGlyph("M3 12h18c0 4.5-4 8-9 8s-9-3.5-9-8", "M9 9c-1-1.2 1-2.3 0-3.5M15 9c-1-1.2 1-2.3 0-3.5"),
        "termo" to StrokeGlyph("M8 3h8v3H8zM7 6h10v15H7z", "M7 11h10"),
        "huevo" to StrokeGlyph("M12 3c3.5 0 6 5 6 10.5a6 6 0 0 1-12 0C6 8 8.5 3 12 3", "M9.5 14.5a2.5 2.5 0 0 0 5 0"),
        "vitamina" to StrokeGlyph("M20 12a8 8 0 1 1-16 0a8 8 0 0 1 16 0", "M12 6v12M6.8 9l10.4 6M17.2 9 6.8 15"),
        "sinazucar" to StrokeGlyph("M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0", "M5.6 5.6l12.8 12.8"),
        "hoy" to StrokeGlyph("M16 12a4 4 0 1 1-8 0a4 4 0 0 1 8 0", "M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4"),
        "tazon" to StrokeGlyph("M3 11h18c0 5-4 9-9 9s-9-4-9-9z", "M8 11c0-3 2-5 4-5M14 8l3-3"),
        "camara" to StrokeGlyph("M4 8h3l2-3h6l2 3h3v11H4z", "M15 13a3 3 0 1 1-6 0a3 3 0 0 1 6 0"),
    )

    fun get(name: String): StrokeGlyph = all[name] ?: all.getValue("nueva")
}

private val StrokeGray = Color(0xFF5C5C5C)
private val StrokeWhite = Color(0xFFF5F5F5)
private val StrokeRed = Color(0xFFD71921)

/** Each "M" starts its own piece, so PathMeasure (one contour at a time) can draw the line from start to end. */
private fun pieces(data: String?): List<Path> =
    data?.split(Regex("(?=M)")).orEmpty().filter { it.isNotBlank() }.map { PathParser().parsePathString(it).toPath() }

private class Parsed(val main: List<Path>, val red: List<Path>, val mainLength: Float, val redLength: Float)

private fun parse(glyph: StrokeGlyph): Parsed {
    val main = pieces(glyph.main)
    val red = pieces(glyph.red)
    val measure = PathMeasure()
    fun length(paths: List<Path>) = paths.sumOf { measure.setPath(it, false); measure.length.toDouble() }.toFloat()
    return Parsed(main, red, length(main), length(red))
}

/**
 * A continuous-line icon. When [playKey] changes (not on first show) the line draws itself once from start to
 * end and the red detail draws last, then stays still. Nothing loops.
 */
@Composable
fun StrokeIcon(name: String, state: StrokeState, size: Dp, modifier: Modifier = Modifier, playKey: Any? = null, color: Color? = null) {
    val parsed = remember(name) { parse(StrokeIcons.get(name)) }
    val progress = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(playKey) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(DRAW_MS, easing = FastOutSlowInEasing))
    }
    val lineColor = color ?: if (state == StrokeState.OFF) StrokeGray else StrokeWhite
    val redColor = if (state == StrokeState.NOW && color == null) StrokeRed else lineColor
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        val p = progress.value
        // The line takes the first 70 % of the time; the red detail the rest.
        val mainP = if (parsed.red.isEmpty()) p else (p / MAIN_SHARE).coerceAtMost(1f)
        val redP = if (parsed.red.isEmpty()) 0f else ((p - MAIN_SHARE) / (1f - MAIN_SHARE)).coerceIn(0f, 1f)
        val line = Stroke(LINE_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(k, k, pivot = Offset.Zero) {
            drawPieces(parsed.main, parsed.mainLength * mainP, lineColor, line)
            if (parsed.red.isNotEmpty()) drawPieces(parsed.red, parsed.redLength * redP, redColor, line)
        }
    }
}

/** Draws [paths] in order up to [budget] of their total length. */
private fun DrawScope.drawPieces(paths: List<Path>, budget: Float, color: Color, stroke: Stroke) {
    var left = budget
    val measure = PathMeasure()
    for (path in paths) {
        if (left <= 0f) return
        measure.setPath(path, false)
        val len = measure.length
        if (left >= len) drawPath(path, color, style = stroke)
        else {
            val part = Path()
            measure.getSegment(0f, left, part, true)
            drawPath(part, color, style = stroke)
        }
        left -= len
    }
}

private const val LINE_WIDTH = 1.75f
private const val DRAW_MS = 650
private const val MAIN_SHARE = 0.7f

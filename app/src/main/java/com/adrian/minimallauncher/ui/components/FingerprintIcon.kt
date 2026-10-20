package com.adrian.minimallauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Line fingerprint: open concentric arcs, thin stroke, no fill. */
@Composable
fun FingerprintIcon(modifier: Modifier = Modifier, size: Dp = 40.dp, color: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = this.size.minDimension * 0.045f, cap = StrokeCap.Round)
        val center = Offset(this.size.width / 2f, this.size.height * 0.55f)
        // Each ring: radius as a share of the icon, start angle and sweep, leaving gaps like a real print.
        val rings = listOf(
            Triple(0.08f, 200f, 260f),
            Triple(0.18f, 190f, 250f),
            Triple(0.28f, 185f, 230f),
            Triple(0.38f, 195f, 200f),
            Triple(0.47f, 205f, 130f),
        )
        rings.forEach { (share, start, sweep) ->
            val r = this.size.minDimension * share
            drawArc(color, start, sweep, false, Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = stroke)
        }
    }
}

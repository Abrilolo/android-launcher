package com.adrian.minimallauncher.ui

import android.os.Build
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** How dark the home gets behind the open drawer, as the exercise scene's sheets (#000000B8). */
const val DRAWER_DIM = 0.72f

/** Share of the screen the drawer travels; the finger moves it 1:1 over that distance. */
const val DRAWER_TRAVEL = 0.5f

/**
 * The app drawer's position, 0 closed to 1 open. It follows the finger while dragged and
 * settles open or closed when let go, by how far and how fast it went.
 */
@Stable
class AppsDrawerState(private val scope: CoroutineScope) {
    var value by mutableFloatStateOf(0f)
        private set

    /** Drag distance in px that takes it from closed to open. */
    var heightPx = 2000f

    val visible by derivedStateOf { value > 0f }

    private var settling: Job? = null
    private var dragStart: Float? = null

    /** [dy] in px: negative pulls it up (open), positive pushes it down. */
    fun dragBy(dy: Float) {
        settling?.cancel()
        if (dragStart == null) dragStart = value
        value = (value - dy / heightPx).coerceIn(0f, 1f)
    }

    /** [velocityY] in px/s, negative upward. */
    fun release(velocityY: Float) {
        val start = dragStart ?: value
        dragStart = null
        val target = when {
            velocityY < -FLING -> 1f
            velocityY > FLING -> 0f
            // A short pull is enough either way, as on One UI.
            start < 0.5f -> if (value > 0.3f) 1f else 0f
            else -> if (value < 0.7f) 0f else 1f
        }
        settleTo(target)
    }

    fun open() = settleTo(1f)
    fun close() = settleTo(0f)

    fun snapClosed() {
        settling?.cancel()
        dragStart = null
        value = 0f
    }

    private fun settleTo(target: Float) {
        settling?.cancel()
        if (value == target) return
        settling = scope.launch {
            animate(value, target, animationSpec = tween(260)) { v, _ -> value = v }
        }
    }

    private companion object {
        const val FLING = 900f
    }
}

@Composable
fun rememberAppsDrawerState(): AppsDrawerState {
    val scope = rememberCoroutineScope()
    return remember { AppsDrawerState(scope) }
}

/** The home behind the drawer: blurs as the drawer comes up. */
fun Modifier.drawerBackdrop(drawer: AppsDrawerState): Modifier = graphicsLayer {
    val radius = 18.dp.toPx() * drawer.value
    renderEffect = if (radius > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) BlurEffect(radius, radius, TileMode.Clamp) else null
}

/**
 * Pulls the drawer with the leftover of a vertical drag: up when [canPull] says the content
 * can't scroll any further in that direction from the start of the gesture. A gesture that
 * scrolled the content never moves the drawer, so scrolling a long page doesn't open it.
 */
class DrawerPull(
    private val drawer: AppsDrawerState,
    /** Upward (open) for the home, downward (close) for the drawer itself. */
    private val upward: Boolean,
    private val enabled: () -> Boolean,
) : NestedScrollConnection {
    private var pulling = false
    private var scrolled = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (!pulling || source != NestedScrollSource.UserInput) return Offset.Zero
        drawer.dragBy(available.y)
        return Offset(0f, available.y)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || !enabled()) return Offset.Zero
        if (abs(consumed.y) > 0f) { scrolled = true; return Offset.Zero }
        val toward = if (upward) available.y < 0f else available.y > 0f
        if (scrolled || !toward) return Offset.Zero
        pulling = true
        drawer.dragBy(available.y)
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        scrolled = false
        if (!pulling) return Velocity.Zero
        pulling = false
        drawer.release(available.y)
        return available
    }
}

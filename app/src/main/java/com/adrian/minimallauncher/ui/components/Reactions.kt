package com.adrian.minimallauncher.ui.components

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Feedback for relevant actions: a short vibration when marking or saving, a double one when
 * something is finished, and a "no" when an action can't be done. Off when the user turns
 * vibration off in Módulos.
 */
class Reactions(private val view: View?, private val enabled: Boolean) {
    fun tick() = feel(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
    fun done() {
        tick()
        view?.postDelayed({ tick() }, 110)
    }
    fun reject() = feel(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)

    private fun feel(type: Int) {
        if (enabled) view?.performHapticFeedback(type)
    }
}

val LocalReactions = staticCompositionLocalOf { Reactions(null, enabled = false) }

@Composable
fun rememberReactions(enabled: Boolean): Reactions {
    val view = LocalView.current
    return remember(view, enabled) { Reactions(view, enabled) }
}

/** Whether the phone has animations turned off (developer options or accessibility). */
@Composable
internal fun animationsOff(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/** A small bounce whenever [key] changes (not on first show): something was just marked or added. */
fun Modifier.popOnChange(key: Any?): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    val off = animationsOff()
    LaunchedEffect(key) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (off) return@LaunchedEffect
        scale.animateTo(1.22f, tween(120))
        scale.animateTo(1f, tween(200))
    }
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}
